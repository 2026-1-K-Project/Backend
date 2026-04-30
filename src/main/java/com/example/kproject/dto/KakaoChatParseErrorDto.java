package com.example.kproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오톡 txt 파싱 오류 정보")
public record KakaoChatParseErrorDto(
        @Schema(description = "오류가 발생한 줄 번호", example = "17")
        int lineNumber,
        @Schema(description = "원본 줄 내용")
        String rawLine,
        @Schema(description = "오류 사유")
        String reason
) {
}
