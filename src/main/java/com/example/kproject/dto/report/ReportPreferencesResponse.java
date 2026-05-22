package com.example.kproject.dto.report;

import java.util.List;

public record ReportPreferencesResponse(
        Long reportId,
        List<String> likedPhrases,
        List<String> likedBehaviors,
        List<String> favoriteTopics,
        List<String> dislikedExpressions,
        List<String> burdensomeExpressions
) {
}
