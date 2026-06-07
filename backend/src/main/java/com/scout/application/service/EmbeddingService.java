package com.scout.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scout.application.dto.DocumentSearchResultDto;
import com.scout.application.dto.EmbeddingJobResponse;
import com.scout.domain.entity.JobRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final String MODEL = "text-embedding-3-small";
    private static final int BATCH_SIZE = 50;

    private final JdbcTemplate jdbcTemplate;
    private final JobRunService jobRunService;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(60))
            .build();

    @Transactional
    public EmbeddingJobResponse generateMissingEmbeddings(int limit) {
        requireApiKey();
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<ChunkInput> chunks = findChunksMissingEmbeddings(safeLimit);

        JobRun jobRun = jobRunService.start("EmbeddingJob", chunks.size());
        int created = 0;
        int aiCalls = 0;

        try {
            for (int start = 0; start < chunks.size(); start += BATCH_SIZE) {
                List<ChunkInput> batch = chunks.subList(start, Math.min(start + BATCH_SIZE, chunks.size()));
                List<List<Double>> embeddings = createEmbeddings(batch.stream().map(ChunkInput::text).toList());
                aiCalls++;

                for (int index = 0; index < batch.size(); index++) {
                    insertEmbedding(batch.get(index).id(), embeddings.get(index));
                    created++;
                }
            }

            JobRun completed = jobRunService.complete(jobRun, created, aiCalls);
            return EmbeddingJobResponse.builder()
                    .jobRunId(completed.getId())
                    .chunksFound(chunks.size())
                    .embeddingsCreated(created)
                    .aiCalls(aiCalls)
                    .modelName(MODEL)
                    .build();
        } catch (RuntimeException e) {
            jobRunService.fail(jobRun, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentSearchResultDto> semanticSearch(String query, int limit) {
        requireApiKey();
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("q query parameter is required");
        }

        List<Double> queryEmbedding = createEmbeddings(List.of(query.trim())).get(0);
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        int safeLimit = Math.max(1, Math.min(limit, 50));

        return jdbcTemplate.query("""
                select
                    d.id as document_id,
                    c.id as chunk_id,
                    c.chunk_index,
                    co.ticker,
                    d.title,
                    d.source_type,
                    c.chunk_text,
                    e.embedding <=> ?::vector as distance
                from document_embeddings e
                join document_chunks c on c.id = e.chunk_id
                join raw_documents d on d.id = c.document_id
                left join companies co on co.id = d.company_id
                order by e.embedding <=> ?::vector
                limit ?
                """,
                (rs, rowNum) -> DocumentSearchResultDto.builder()
                        .documentId(rs.getLong("document_id"))
                        .chunkId(rs.getLong("chunk_id"))
                        .chunkIndex(rs.getInt("chunk_index"))
                        .ticker(rs.getString("ticker"))
                        .title(rs.getString("title"))
                        .sourceType(rs.getString("source_type"))
                        .snippet(buildSnippet(rs.getString("chunk_text")))
                        .distance(rs.getDouble("distance"))
                        .build(),
                vectorLiteral,
                vectorLiteral,
                safeLimit);
    }

    private List<ChunkInput> findChunksMissingEmbeddings(int limit) {
        return jdbcTemplate.query("""
                select c.id, c.chunk_text
                from document_chunks c
                left join document_embeddings e on e.chunk_id = c.id
                where e.id is null
                order by c.created_at asc, c.id asc
                limit ?
                """,
                (rs, rowNum) -> new ChunkInput(rs.getLong("id"), rs.getString("chunk_text")),
                limit);
    }

    private void insertEmbedding(Long chunkId, List<Double> embedding) {
        jdbcTemplate.update("""
                insert into document_embeddings (chunk_id, embedding, model_name)
                values (?, ?::vector, ?)
                """,
                chunkId,
                toVectorLiteral(embedding),
                MODEL);
    }

    private List<List<Double>> createEmbeddings(List<String> inputs) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", MODEL);
            payload.put("input", inputs);
            payload.put("encoding_format", "float");

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/embeddings")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("OpenAI embeddings API error: " + response.code() + " - " + responseBody);
                }

                JsonNode data = objectMapper.readTree(responseBody).path("data");
                List<List<Double>> embeddings = new ArrayList<>();
                for (JsonNode item : data) {
                    List<Double> vector = new ArrayList<>();
                    for (JsonNode value : item.path("embedding")) {
                        vector.add(value.asDouble());
                    }
                    embeddings.add(vector);
                }
                return embeddings;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create OpenAI embeddings", e);
        }
    }

    private void requireApiKey() {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is required for embeddings and semantic search");
        }
    }

    static String toVectorLiteral(List<Double> embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < embedding.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(embedding.get(index));
        }
        return builder.append(']').toString();
    }

    private static String buildSnippet(String text) {
        if (text == null || text.length() <= 320) {
            return text;
        }
        return text.substring(0, 320) + "...";
    }

    private record ChunkInput(Long id, String text) {
    }
}
