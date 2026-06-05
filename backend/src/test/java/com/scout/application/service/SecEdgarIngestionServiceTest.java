package com.scout.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecEdgarIngestionServiceTest {

    @Test
    void padsCikToTenDigits() {
        assertThat(SecEdgarIngestionService.padCik("1045810")).isEqualTo("0001045810");
        assertThat(SecEdgarIngestionService.padCik("0000789019")).isEqualTo("0000789019");
    }

    @Test
    void rejectsBlankCik() {
        assertThatThrownBy(() -> SecEdgarIngestionService.padCik(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CIK is required");
    }

    @Test
    void stripsLeadingZerosForArchivePath() {
        assertThat(SecEdgarIngestionService.stripLeadingZeros("0001045810")).isEqualTo("1045810");
    }

    @Test
    void convertsBasicHtmlToText() {
        String html = "<html><head><style>.x{}</style></head><body><h1>10-K</h1><p>AI&nbsp;&amp;&#160;data center&#8217;s growth</p></body></html>";

        assertThat(SecEdgarIngestionService.htmlToText(html)).isEqualTo("10-K AI & data center's growth");
    }

    @Test
    void removesInlineXbrlNoise() {
        String html = """
                <html><body>
                  <ix:header><xbrli:context>context noise</xbrli:context></ix:header>
                  <ix:hidden>hidden fact noise</ix:hidden>
                  <p>Data center demand&#8226; remains strong.</p>
                  <p>us-gaap:Revenue nvda:ComputeAndNetworkingSegmentMember</p>
                </body></html>
                """;

        String text = SecEdgarIngestionService.htmlToText(html);

        assertThat(text).contains("Data center demand remains strong.");
        assertThat(text).doesNotContain("hidden fact noise");
        assertThat(text).doesNotContain("us-gaap");
        assertThat(text).doesNotContain("nvda:");
    }

    @Test
    void decodesNumericEntities() {
        assertThat(SecEdgarIngestionService.decodeNumericEntities("A&#8217;s B&#x2014;C"))
                .isEqualTo("A's B-C");
    }
}
