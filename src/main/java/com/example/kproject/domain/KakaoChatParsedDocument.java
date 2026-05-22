package com.example.kproject.domain;

import com.example.kproject.dto.KakaoChatMessageDto;
import com.example.kproject.dto.KakaoChatMetaDto;
import com.example.kproject.dto.KakaoChatParseErrorDto;

import java.util.List;

public record KakaoChatParsedDocument(
        KakaoChatMetaDto meta,
        String rawText,
        List<String> lines,
        List<KakaoChatMessageDto> messages,
        String analysisText,
        List<KakaoChatParseErrorDto> errors,
        KakaoChatParsingStats parsingStats
) {
    public boolean hasReadableText() {
        return rawText != null && !rawText.isBlank();
    }

    public boolean supportsStructuredAnalysis(double threshold, int minimumMessages) {
        return messages.size() >= minimumMessages
                && parsingStats != null
                && parsingStats.successRate() >= threshold;
    }
}
