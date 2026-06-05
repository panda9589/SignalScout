package com.scout.infrastructure.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkerTest {

    @Test
    void chunkByWordsSplitsTextWithOverlap() {
        List<String> chunks = TextChunker.chunkByWords("one two three four five six seven", 3, 1);

        assertThat(chunks).containsExactly(
                "one two three",
                "three four five",
                "five six seven"
        );
    }

    @Test
    void chunkByWordsReturnsEmptyForBlankText() {
        assertThat(TextChunker.chunkByWords("   ", 3, 1)).isEmpty();
    }

    @Test
    void chunkByWordsRejectsInvalidOverlap() {
        assertThatThrownBy(() -> TextChunker.chunkByWords("one two", 2, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlapWords");
    }

    @Test
    void approximateTokenCountUsesFourCharactersPerTokenFlooringAtOne() {
        assertThat(TextChunker.approximateTokenCount("abcd")).isEqualTo(1);
        assertThat(TextChunker.approximateTokenCount("abcde")).isEqualTo(2);
        assertThat(TextChunker.approximateTokenCount("")).isZero();
    }
}
