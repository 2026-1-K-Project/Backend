package com.example.kproject.dto.report;

public record AiConversationEvidence(
        String type,
        String label,
        String quote,
        String reason,
        int strength
) {
}
