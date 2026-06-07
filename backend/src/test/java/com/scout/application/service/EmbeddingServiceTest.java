package com.scout.application.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceTest {

    @Test
    void formatsPgvectorLiteral() {
        assertThat(EmbeddingService.toVectorLiteral(List.of(0.1, -0.25, 3.0)))
                .isEqualTo("[0.1,-0.25,3.0]");
    }
}
