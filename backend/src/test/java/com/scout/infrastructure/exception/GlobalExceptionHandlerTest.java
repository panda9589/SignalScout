package com.scout.infrastructure.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgumentReturnsBadRequest() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad input"), null);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("bad input");
    }

    @Test
    void unreadableMessageReturnsBadRequest() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleUnreadableMessage(new HttpMessageNotReadableException("bad json"), null);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Malformed request body");
    }

    @Test
    void illegalStateReturnsBadRequest() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleIllegalState(new IllegalStateException("missing configuration"), null);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("missing configuration");
    }
}
