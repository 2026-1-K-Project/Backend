package com.example.kproject.dto.report;

import java.util.List;

public record ReportInsightsResponse(
        Long reportId,
        List<String> tips,
        List<String> warnings,
        List<ReportResponse.DecisiveMoment> decisiveMoments,
        List<String> recommendedQuestions,
        List<String> recommendedReplies
) {
}
