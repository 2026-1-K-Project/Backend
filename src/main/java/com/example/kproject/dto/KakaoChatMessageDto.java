package com.example.kproject.dto;

public record KakaoChatMessageDto(
        String sender,
        String date,
        String timeText,
        String dateTime,
        String content,
        KakaoChatSpecialType specialType
) {
}
