package com.scout.presentation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void healthReturnsOkPayload() {
        ResponseEntity<Map<String, Object>> response = new HealthController().health();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "ok");
        assertThat(response.getBody()).containsEntry("service", "signalscout-api");
        assertThat(response.getBody()).containsKey("timestamp");
    }
}
