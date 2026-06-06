package com.example.kproject.dto.report;

import java.time.LocalDateTime;

public record ReportListItemResponse(
        Long reportId,
        Long memberId,
        String title,
        String category,
        String sourceType,
        int messageCount,
        int uploadedFileCount,
        String status,
        ReportAnalysisMode analysisMode,
        int resultScore,
        String description,
        boolean trashed,
        LocalDateTime createdAt,
        LocalDateTime trashedAt
) {
}
