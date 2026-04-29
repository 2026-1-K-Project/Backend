package com.example.kproject.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public record KakaoChatMessageDto(
        String sender,
        String date,
        String timeText,
        String dateTime,
        String content,
        KakaoChatSpecialType specialType,
        @JsonIgnore LocalDateTime timestamp
) {
}
