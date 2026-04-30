package com.example.kproject.dto.report;

import java.util.List;

public record ReportQuestionsResponse(
        Long reportId,
        List<String> recommendedQuestions
) {
}
