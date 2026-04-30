package com.example.kproject.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "리포트 추천 질문 응답")
public record ReportQuestionsResponse(
        @Schema(description = "리포트 ID", example = "1")
        Long reportId,
        @Schema(description = "추천 질문 목록")
        List<String> recommendedQuestions
) {
}
