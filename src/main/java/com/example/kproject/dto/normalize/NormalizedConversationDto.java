package com.example.kproject.dto.normalize;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "정형화된 대화 데이터")
public record NormalizedConversationDto(
        List<String> participants,
        List<MessageDto> messages,
        List<String> keywords,
        String rawText
) {
    public record MessageDto(
            String sender,
            String timestamp,
            String content,
            String type
    ) {
    }
}
