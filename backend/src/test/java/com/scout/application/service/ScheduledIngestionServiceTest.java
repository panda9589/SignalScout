package com.scout.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledIngestionServiceTest {

    @Test
    void parsesRssFeedSpecs() {
        var specs = ScheduledIngestionService.parseFeedSpecs(
                "https://example.com/feed.xml|NVDA, https://example.com/macro.xml");

        assertThat(specs).hasSize(2);
        assertThat(specs.get(0).feedUrl()).isEqualTo("https://example.com/feed.xml");
        assertThat(specs.get(0).ticker()).isEqualTo("NVDA");
        assertThat(specs.get(1).feedUrl()).isEqualTo("https://example.com/macro.xml");
        assertThat(specs.get(1).ticker()).isNull();
    }
}
