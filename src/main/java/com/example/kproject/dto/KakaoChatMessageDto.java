package com.example.kproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오톡 대화 메시지")
public record KakaoChatMessageDto(
        @Schema(description = "발신자 이름", example = "상대방")
        String sender,
        @Schema(description = "메시지 날짜", example = "2026-04-29")
        String date,
        @Schema(description = "원본 시각 문자열", example = "오전 10:48")
        String timeText,
        @Schema(description = "정규화된 일시", example = "2026-04-29T10:48:00")
        String dateTime,
        @Schema(description = "메시지 본문")
        String content,
        @Schema(description = "메시지 특수 유형")
        KakaoChatSpecialType specialType
) {
}
