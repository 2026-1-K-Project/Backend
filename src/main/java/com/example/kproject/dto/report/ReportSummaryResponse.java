package com.example.kproject.dto.report;

import java.util.List;

public record ReportSummaryResponse(
        Long reportId,
        int interestScore,
        String headline,
        List<String> keywords,
        String atmosphereSummary,
        ReportAnalysisMode analysisMode,
        String warning
) {
}
