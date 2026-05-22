package com.example.kproject.dto.report;

public record ReportPersonalityResponse(
        Long reportId,
        ReportResponse.Estimate mbti,
        ReportResponse.Estimate attachmentType,
        ReportResponse.BigFive bigFive,
        String speechStyle,
        String emotionalExpressionStyle,
        String counterpartyTendency
) {
}
