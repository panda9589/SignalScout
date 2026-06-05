package com.scout.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scout.application.dto.RssIngestionResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RssIngestionService {

    private static final String SOURCE_TYPE = "RSS_FEED";

    private final CompanyRepository companyRepository;
    private final DataSourceRepository dataSourceRepository;
    private final RawDocumentRepository rawDocumentRepository;
    private final DocumentChunkingService documentChunkingService;
    private final CompanyMatchingService companyMatchingService;
    private final JobRunService jobRunService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Transactional
    public RssIngestionResponse ingestFeed(String feedUrl, String ticker, int limit) {
        if (feedUrl == null || feedUrl.isBlank()) {
            throw new IllegalArgumentException("feedUrl is required");
        }

        int safeLimit = Math.max(1, Math.min(limit, 50));
        Company company = resolveCompany(ticker);
        boolean explicitCompany = company != null;
        JobRun jobRun = jobRunService.start("RssIngestionJob", safeLimit);
        int documentsStored = 0;
        int duplicatesSkipped = 0;
        int autoMatchedDocuments = 0;
        int chunksCreated = 0;

        try {
            DataSource source = resolveRssSource(feedUrl);
            List<RssItem> items = fetchFeedItems(feedUrl, safeLimit);

            for (RssItem item : items) {
                String externalId = item.externalId();
                if (rawDocumentRepository.existsBySourceTypeAndExternalId(SOURCE_TYPE, externalId)) {
                    duplicatesSkipped++;
                    continue;
                }

                String rawText = item.toRawText();
                Company matchedCompany = explicitCompany
                        ? company
                        : companyMatchingService.matchWatchlistCompany(rawText).orElse(null);
                if (!explicitCompany && matchedCompany != null) {
                    autoMatchedDocuments++;
                }

                String contentHash = HashUtil.sha256(externalId + "\n" + rawText);
                if (rawDocumentRepository.findByContentHash(contentHash).isPresent()) {
                    duplicatesSkipped++;
                    continue;
                }

                RawDocument document = rawDocumentRepository.save(RawDocument.builder()
                        .company(matchedCompany)
                        .source(source)
                        .sourceType(SOURCE_TYPE)
                        .externalId(externalId)
                        .sourceUrl(item.link())
                        .title(truncate(item.title(), 500))
                        .publishedAt(null)
                        .retrievedAt(LocalDateTime.now())
                        .contentHash(contentHash)
                        .rawText(rawText)
                        .metadataJson(toMetadataJson(feedUrl, item, matchedCompany, !explicitCompany && matchedCompany != null))
                        .processingStatus("completed")
                        .build());

                chunksCreated += documentChunkingService.recreateChunks(document);
                documentsStored++;
            }

            JobRun completed = jobRunService.complete(jobRun, documentsStored);
            return RssIngestionResponse.builder()
                    .jobRunId(completed.getId())
                    .itemsFound(items.size())
                    .documentsStored(documentsStored)
                    .duplicatesSkipped(duplicatesSkipped)
                    .autoMatchedDocuments(autoMatchedDocuments)
                    .chunksCreated(chunksCreated)
                    .build();
        } catch (RuntimeException e) {
            jobRunService.fail(jobRun, e);
            throw e;
        }
    }

    private Company resolveCompany(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return null;
        }
        return companyRepository.findByTicker(ticker.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + ticker));
    }

    private DataSource resolveRssSource(String feedUrl) {
        return dataSourceRepository.findBySourceType(SOURCE_TYPE)
                .orElseGet(() -> dataSourceRepository.save(DataSource.builder()
                        .sourceType(SOURCE_TYPE)
                        .name("RSS Feed")
                        .baseUrl(feedUrl)
                        .trustLevel("medium")
                        .enabled(true)
                        .pollIntervalMinutes(1440)
                        .build()));
    }

    private List<RssItem> fetchFeedItems(String feedUrl, int limit) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(feedUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/rss+xml,application/atom+xml,application/xml,text/xml")
                    .header("User-Agent", "SignalScout local-dev")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("RSS request failed with status " + response.statusCode());
            }
            return parseFeed(response.body(), limit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching RSS feed", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch RSS feed: " + e.getMessage(), e);
        }
    }

    static List<RssItem> parseFeed(String xml, int limit) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);

        org.w3c.dom.Document document = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        document.getDocumentElement().normalize();

        NodeList rssItems = document.getElementsByTagName("item");
        if (rssItems.getLength() > 0) {
            return parseRssItems(rssItems, limit);
        }

        NodeList atomEntries = document.getElementsByTagName("entry");
        return parseAtomEntries(atomEntries, limit);
    }

    private static List<RssItem> parseRssItems(NodeList nodes, int limit) {
        List<RssItem> items = new ArrayList<>();
        for (int index = 0; index < nodes.getLength() && items.size() < limit; index++) {
            Element item = (Element) nodes.item(index);
            String title = childText(item, "title");
            String link = childText(item, "link");
            String guid = childText(item, "guid");
            String description = childText(item, "description");
            String publishedAt = childText(item, "pubDate");
            items.add(new RssItem(title, link, guid, description, publishedAt));
        }
        return items;
    }

    private static List<RssItem> parseAtomEntries(NodeList nodes, int limit) {
        List<RssItem> items = new ArrayList<>();
        for (int index = 0; index < nodes.getLength() && items.size() < limit; index++) {
            Element entry = (Element) nodes.item(index);
            String title = childText(entry, "title");
            String link = atomLink(entry);
            String id = childText(entry, "id");
            String summary = childText(entry, "summary");
            if (summary.isBlank()) {
                summary = childText(entry, "content");
            }
            String publishedAt = childText(entry, "published");
            if (publishedAt.isBlank()) {
                publishedAt = childText(entry, "updated");
            }
            items.add(new RssItem(title, link, id, summary, publishedAt));
        }
        return items;
    }

    private String toMetadataJson(String feedUrl, RssItem item, Company matchedCompany, boolean autoMatched) {
        try {
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("feedUrl", feedUrl);
            metadata.put("guid", item.guid());
            metadata.put("publishedAt", item.publishedAt());
            if (matchedCompany != null) {
                metadata.put("matchedTicker", matchedCompany.getTicker());
                metadata.put("matchedCompanyName", matchedCompany.getName());
                metadata.put("companyMatchMethod", autoMatched ? "auto" : "explicit");
            }
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize RSS item metadata", e);
        }
    }

    private static String childText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        if (children.getLength() == 0 || children.item(0).getTextContent() == null) {
            return "";
        }
        return children.item(0).getTextContent().trim();
    }

    private static String atomLink(Element entry) {
        NodeList links = entry.getElementsByTagName("link");
        if (links.getLength() == 0) {
            return "";
        }
        Element link = (Element) links.item(0);
        String href = link.getAttribute("href");
        return href == null || href.isBlank() ? link.getTextContent().trim() : href.trim();
    }

    static String stripHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "Untitled RSS Item";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    record RssItem(String title, String link, String guid, String description, String publishedAt) {
        String externalId() {
            if (guid != null && !guid.isBlank()) {
                return guid;
            }
            if (link != null && !link.isBlank()) {
                return link;
            }
            return HashUtil.sha256(title + "\n" + description);
        }

        String toRawText() {
            return """
                    Title: %s
                    Link: %s
                    Published: %s

                    %s
                    """.formatted(
                    title == null ? "" : title,
                    link == null ? "" : link,
                    publishedAt == null ? "" : publishedAt,
                    stripHtml(description));
        }
    }
}
