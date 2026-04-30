package com.example.kproject.domain;

import java.util.List;

public record ReportAnalysisContext(
        String category,
        List<String> participants,
        List<String> otherParticipants,
        String meParticipant,
        List<ReportMessage> messages,
        String analysisText
) {
    public boolean isMe(String sender) {
        return meParticipant != null && meParticipant.equals(sender);
    }

    public boolean isOther(String sender) {
        return otherParticipants.contains(sender) || !isMe(sender);
    }
}
