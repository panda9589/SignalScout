package com.scout.application.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RssIngestionServiceTest {

    @Test
    void stripsHtmlEntities() {
        assertThat(RssIngestionService.stripHtml("<p>AI&nbsp;&amp;&#160;chips</p>"))
                .isEqualTo("AI & chips");
    }

    @Test
    void parsesRssItems() throws Exception {
        String xml = """
                <rss><channel>
                  <item>
                    <title>NVIDIA update</title>
                    <link>https://example.com/nvda</link>
                    <guid>item-1</guid>
                    <description><![CDATA[<p>Data center growth</p>]]></description>
                    <pubDate>Thu, 04 Jun 2026 12:00:00 GMT</pubDate>
                  </item>
                </channel></rss>
                """;

        List<RssIngestionService.RssItem> items = RssIngestionService.parseFeed(xml, 5);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("NVIDIA update");
        assertThat(items.get(0).externalId()).isEqualTo("item-1");
        assertThat(items.get(0).toRawText()).contains("Data center growth");
    }
}
