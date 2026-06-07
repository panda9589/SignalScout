package com.scout.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scout.application.dto.SecIngestionResponse;
import com.scout.domain.entity.Company;
import com.scout.domain.entity.DataSource;
import com.scout.domain.entity.JobRun;
import com.scout.domain.entity.RawDocument;
import com.scout.domain.repository.CompanyRepository;
import com.scout.domain.repository.DataSourceRepository;
import com.scout.domain.repository.RawDocumentRepository;
import com.scout.infrastructure.exception.ResourceNotFoundException;
import com.scout.infrastructure.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecEdgarIngestionService {

    private static final String SOURCE_TYPE = "SEC_FILING";
    private static final List<String> DEFAULT_FORMS = List.of("10-K", "10-Q", "8-K");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?[0-9a-fA-F]+);");

    private final CompanyRepository companyRepository;
    private final DataSourceRepository dataSourceRepository;
    private final RawDocumentRepository rawDocumentRepository;
    private final DocumentChunkingService documentChunkingService;
    private final JobRunService jobRunService;
    private final ObjectMapper objectMapper;

    @Value("${sec.api.user-agent:SignalScout local-dev contact@example.com}")
    private String userAgent;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Transactional
    public SecIngestionResponse ingestRecentFilings(String ticker, int limitPerCompany) {
        List<Company> companies = resolveCompanies(ticker);
        int safeLimit = Math.max(1, Math.min(limitPerCompany, 20));

        JobRun jobRun = jobRunService.start("SecEdgarIngestionJob", companies.size() * safeLimit);
        int filingsFound = 0;
        int documentsStored = 0;
        int duplicatesSkipped = 0;
        int chunksCreated = 0;

        try {
            DataSource source = resolveSecSource();

            for (Company company : companies) {
                List<SecFiling> filings = fetchRecentFilings(company, safeLimit);
                filingsFound += filings.size();

                for (SecFiling filing : filings) {
                    if (rawDocumentRepository.existsBySourceTypeAndExternalId(SOURCE_TYPE, filing.accessionNumber())) {
                        duplicatesSkipped++;
                        continue;
                    }

                    String rawText = fetchPrimaryDocumentText(filing);
                    String contentHash = HashUtil.sha256(filing.accessionNumber() + "\n" + rawText);
                    if (rawDocumentRepository.findByContentHash(contentHash).isPresent()) {
                        duplicatesSkipped++;
                        continue;
                    }

                    RawDocument document = rawDocumentRepository.save(RawDocument.builder()
                            .company(company)
                            .source(source)
                            .sourceType(SOURCE_TYPE)
                            .externalId(filing.accessionNumber())
                            .sourceUrl(filing.documentUrl())
                            .title(truncate(filing.title(), 500))
                            .publishedAt(filing.filingDateTime())
                            .retrievedAt(LocalDateTime.now())
                            .contentHash(contentHash)
                            .rawText(rawText)
                            .metadataJson(toMetadataJson(filing))
                            .processingStatus("completed")
                            .build());

                    chunksCreated += documentChunkingService.recreateChunks(document);
                    documentsStored++;
                }
            }

            JobRun completed = jobRunService.complete(jobRun, documentsStored);
            return SecIngestionResponse.builder()
                    .jobRunId(completed.getId())
                    .companiesScanned(companies.size())
                    .filingsFound(filingsFound)
                    .documentsStored(documentsStored)
                    .duplicatesSkipped(duplicatesSkipped)
                    .chunksCreated(chunksCreated)
                    .build();
        } catch (RuntimeException e) {
            jobRunService.fail(jobRun, e);
            throw e;
        }
    }

    @Transactional
    public SecIngestionResponse reprocessStoredFiling(Long documentId) {
        RawDocument document = rawDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (!SOURCE_TYPE.equals(document.getSourceType())) {
            throw new IllegalArgumentException("Document is not a SEC filing: " + documentId);
        }
        if (document.getSourceUrl() == null || document.getSourceUrl().isBlank()) {
            throw new IllegalArgumentException("SEC filing has no source URL: " + documentId);
        }

        JobRun jobRun = jobRunService.start("SecFilingReprocessJob", 1);
        try {
            String body = httpGet(document.getSourceUrl());
            String cleanedText = htmlToText(body);
            document.setRawText(cleanedText.isBlank() ? body : cleanedText);
            document.setContentHash(HashUtil.sha256(document.getExternalId() + "\n" + document.getRawText()));
            document.setRetrievedAt(LocalDateTime.now());
            rawDocumentRepository.save(document);

            int chunksCreated = documentChunkingService.recreateChunks(document);
            JobRun completed = jobRunService.complete(jobRun, 1);
            return SecIngestionResponse.builder()
                    .jobRunId(completed.getId())
                    .companiesScanned(1)
                    .filingsFound(1)
                    .documentsStored(1)
                    .duplicatesSkipped(0)
                    .chunksCreated(chunksCreated)
                    .build();
        } catch (IOException e) {
            jobRunService.fail(jobRun, e);
            throw new IllegalStateException("Failed to reprocess SEC filing " + documentId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jobRunService.fail(jobRun, e);
            throw new IllegalStateException("Interrupted while reprocessing SEC filing " + documentId, e);
        } catch (RuntimeException e) {
            jobRunService.fail(jobRun, e);
            throw e;
        }
    }

    private List<Company> resolveCompanies(String ticker) {
        if (ticker != null && !ticker.isBlank()) {
            Company company = companyRepository.findByTicker(ticker.trim().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + ticker));
            if (company.getCik() == null || company.getCik().isBlank()) {
                throw new IllegalArgumentException("Company has no CIK configured: " + company.getTicker());
            }
            return List.of(company);
        }

        return companyRepository.findAll().stream()
                .filter(company -> company.getCik() != null && !company.getCik().isBlank())
                .toList();
    }

    private DataSource resolveSecSource() {
        return dataSourceRepository.findBySourceType(SOURCE_TYPE)
                .or(() -> dataSourceRepository.findBySourceType("SEC"))
                .orElseGet(() -> dataSourceRepository.save(DataSource.builder()
                        .sourceType(SOURCE_TYPE)
                        .name("SEC EDGAR")
                        .baseUrl("https://data.sec.gov/")
                        .trustLevel("high")
                        .enabled(true)
                        .pollIntervalMinutes(1440)
                        .build()));
    }

    private List<SecFiling> fetchRecentFilings(Company company, int limit) {
        try {
            String paddedCik = padCik(company.getCik());
            JsonNode root = objectMapper.readTree(httpGet("https://data.sec.gov/submissions/CIK" + paddedCik + ".json"));
            JsonNode recent = root.path("filings").path("recent");

            List<SecFiling> filings = new ArrayList<>();
            JsonNode accessionNumbers = recent.path("accessionNumber");
            for (int index = 0; index < accessionNumbers.size() && filings.size() < limit; index++) {
                String form = textAt(recent, "form", index);
                String primaryDocument = textAt(recent, "primaryDocument", index);
                if (!DEFAULT_FORMS.contains(form) || primaryDocument.isBlank()) {
                    continue;
                }

                String accessionNumber = textAt(recent, "accessionNumber", index);
                String filingDate = textAt(recent, "filingDate", index);
                String reportDate = textAt(recent, "reportDate", index);
                String description = textAt(recent, "primaryDocDescription", index);
                String accessionNoDashes = accessionNumber.replace("-", "");
                String cikNoLeadingZeros = stripLeadingZeros(company.getCik());
                String documentUrl = "https://www.sec.gov/Archives/edgar/data/"
                        + cikNoLeadingZeros + "/" + accessionNoDashes + "/" + primaryDocument;

                filings.add(new SecFiling(
                        company.getCik(),
                        accessionNumber,
                        form,
                        filingDate,
                        reportDate,
                        primaryDocument,
                        description,
                        documentUrl
                ));
            }
            return filings;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch SEC submissions for " + company.getTicker(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching SEC submissions for " + company.getTicker(), e);
        }
    }

    private String fetchPrimaryDocumentText(SecFiling filing) {
        try {
            String body = httpGet(filing.documentUrl());
            String text = htmlToText(body);
            return text.isBlank() ? body : text;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch SEC filing document: " + filing.accessionNumber(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching SEC filing document: " + filing.accessionNumber(), e);
        }
    }

    private String httpGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json,text/html,application/xhtml+xml,text/plain")
                .header("User-Agent", userAgent)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("SEC request failed with status " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private String toMetadataJson(SecFiling filing) {
        try {
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("cik", filing.cik());
            metadata.put("accessionNumber", filing.accessionNumber());
            metadata.put("form", filing.form());
            metadata.put("filingDate", filing.filingDate());
            metadata.put("reportDate", filing.reportDate());
            metadata.put("primaryDocument", filing.primaryDocument());
            metadata.put("primaryDocDescription", filing.primaryDocDescription());
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize SEC filing metadata", e);
        }
    }

    private static String textAt(JsonNode parent, String field, int index) {
        JsonNode value = parent.path(field).path(index);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    static String padCik(String cik) {
        String digits = cik == null ? "" : cik.replaceAll("\\D", "");
        if (digits.isBlank()) {
            throw new IllegalArgumentException("CIK is required");
        }
        return String.format("%010d", Long.parseLong(digits));
    }

    static String stripLeadingZeros(String cik) {
        String digits = cik == null ? "" : cik.replaceAll("\\D", "");
        String stripped = digits.replaceFirst("^0+", "");
        return stripped.isBlank() ? "0" : stripped;
    }

    static String htmlToText(String html) {
        String cleaned = html
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<ix:hidden.*?</ix:hidden>", " ")
                .replaceAll("(?is)<ix:header.*?</ix:header>", " ")
                .replaceAll("(?is)<xbrli:context.*?</xbrli:context>", " ")
                .replaceAll("(?is)<xbrli:unit.*?</xbrli:unit>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .replace("&#xA0;", " ")
                .replace("&#xa0;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#8217;", "'")
                .replace("&#8220;", "\"")
                .replace("&#8221;", "\"")
                .replace("&#8226;", " ")
                .replace("&#8211;", "-")
                .replace("&#8212;", "-");

        cleaned = decodeNumericEntities(cleaned)
                .replaceAll("\\b[a-zA-Z][a-zA-Z0-9-]*:[a-zA-Z][a-zA-Z0-9-]*\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned;
    }

    static String decodeNumericEntities(String text) {
        Matcher matcher = NUMERIC_ENTITY.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1);
            int codePoint = token.startsWith("x") || token.startsWith("X")
                    ? Integer.parseInt(token.substring(1), 16)
                    : Integer.parseInt(token, 10);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(normalizeEntityCodePoint(codePoint)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String normalizeEntityCodePoint(int codePoint) {
        return switch (codePoint) {
            case 160, 8226 -> " ";
            case 8211, 8212 -> "-";
            case 8217 -> "'";
            case 8220, 8221 -> "\"";
            default -> new String(Character.toChars(codePoint));
        };
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record SecFiling(
            String cik,
            String accessionNumber,
            String form,
            String filingDate,
            String reportDate,
            String primaryDocument,
            String primaryDocDescription,
            String documentUrl
    ) {
        String title() {
            String suffix = primaryDocDescription == null || primaryDocDescription.isBlank()
                    ? primaryDocument
                    : primaryDocDescription;
            return form + " - " + filingDate + " - " + suffix;
        }

        LocalDateTime filingDateTime() {
            if (filingDate == null || filingDate.isBlank()) {
                return null;
            }
            return LocalDate.parse(filingDate).atStartOfDay();
        }
    }
}
