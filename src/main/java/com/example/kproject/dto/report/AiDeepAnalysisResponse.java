package com.example.kproject.dto.report;

import java.util.List;

public record AiDeepAnalysisResponse(
        String verdict,
        int confidence,
        String relationshipStage,
        String oneLineSummary,
        String requestAnswer,
        String requestReason,
        List<String> requestEvidence,
        List<AiConversationEvidence> positiveSignals,
        List<AiConversationEvidence> riskSignals,
        String counterpartyStyle,
        String userPattern,
        List<AiNextAction> nextActions,
        List<AiAvoidMessage> avoidMessages
) {
}
