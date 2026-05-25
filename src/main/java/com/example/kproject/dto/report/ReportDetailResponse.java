package com.example.kproject.dto.report;

import java.time.LocalDateTime;
import java.util.List;

public record ReportDetailResponse(
        Long reportId,
        String category,
        String sourceType,
        List<String> participants,
        int messageCount,
        String analysisStatus,
        ReportAnalysisMode analysisMode,
        String warning,
        LocalDateTime createdAt
) {
}
