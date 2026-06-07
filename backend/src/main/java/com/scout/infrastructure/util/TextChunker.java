package com.scout.infrastructure.util;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    private TextChunker() {
    }

    public static List<String> chunkByWords(String text, int maxWords, int overlapWords) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (maxWords <= 0) {
            throw new IllegalArgumentException("maxWords must be positive");
        }
        if (overlapWords < 0 || overlapWords >= maxWords) {
            throw new IllegalArgumentException("overlapWords must be non-negative and smaller than maxWords");
        }

        String[] words = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + maxWords, words.length);
            chunks.add(String.join(" ", java.util.Arrays.copyOfRange(words, start, end)));
            if (end == words.length) {
                break;
            }
            start = end - overlapWords;
        }

        return chunks;
    }

    public static int approximateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }
}
