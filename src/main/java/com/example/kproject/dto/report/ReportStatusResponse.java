package com.example.kproject.dto.report;

public record ReportStatusResponse(
        Long reportId,
        String status,
        int progress,
        String message,
        ReportAnalysisMode analysisMode,
        String warning
) {
}
