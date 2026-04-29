package com.example.kproject.dto;

public record KakaoChatParseErrorDto(
        int lineNumber,
        String rawLine,
        String reason
) {
}
