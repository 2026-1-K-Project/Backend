package com.example.kproject.domain;

public record KakaoChatParsingStats(
        int totalRelevantLines,
        int parsedRelevantLines,
        int messageStartCount,
        int continuationLineCount,
        int dateSeparatorCount,
        int unmatchedLineCount,
        double successRate,
        boolean usedSavedAtDateFallback
) {
}
