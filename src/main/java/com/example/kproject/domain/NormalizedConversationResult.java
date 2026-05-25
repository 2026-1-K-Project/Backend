package com.example.kproject.domain;

import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.ReportAnalysisMode;

public record NormalizedConversationResult(
        NormalizedConversationDto conversation,
        ReportAnalysisMode analysisMode,
        boolean structuredParsingAvailable,
        String warning
) {
}
