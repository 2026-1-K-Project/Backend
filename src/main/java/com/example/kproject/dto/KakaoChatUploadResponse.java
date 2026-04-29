package com.example.kproject.dto;

import java.util.List;

public record KakaoChatUploadResponse(
        KakaoChatMetaDto meta,
        int messageCount,
        List<KakaoChatMessageDto> messages,
        String analysisText,
        List<KakaoChatParseErrorDto> errors
) {
    public KakaoChatUploadResponse withMeta(KakaoChatMetaDto meta) {
        return new KakaoChatUploadResponse(meta, messageCount, messages, analysisText, errors);
    }
}
