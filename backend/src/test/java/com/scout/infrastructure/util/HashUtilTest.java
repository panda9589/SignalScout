package com.scout.infrastructure.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilTest {

    @Test
    void sha256ReturnsStableLowercaseHexDigest() {
        assertThat(HashUtil.sha256("SignalScout"))
                .isEqualTo("5e321d7c7d5bd3dc0cf1e80a380882a4dcca3699a9982310aa6deac7460715fc");
    }

    @Test
    void sha256DistinguishesDifferentContent() {
        assertThat(HashUtil.sha256("document one"))
                .isNotEqualTo(HashUtil.sha256("document two"));
    }
}
