package com.example.kproject.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "종합 분석 리포트 생성 요청")
public record ReportGenerateRequest(
        @Schema(description = "분석 카테고리", example = "썸/연애")
        @NotBlank(message = "category is required")
        String category,
        @Schema(description = "대화 참여자 목록", example = "[\"나\", \"상대방\"]")
        @NotEmpty(message = "participants are required")
        List<@NotBlank(message = "participant name is required") String> participants,
        @Schema(description = "구조화된 대화 메시지 목록")
        @NotEmpty(message = "messages are required")
        List<@Valid MessageDto> messages,
        @Schema(
                description = "분석용으로 정리된 전체 대화 텍스트. 비어 있으면 메시지 목록을 기준으로 분석합니다.",
                example = "나: 오늘 뭐해?\n상대방: 집이지 ㅋㅋ 너는?"
        )
        String analysisText
) {
    @Schema(description = "구조화된 대화 메시지")
    public record MessageDto(
            @Schema(description = "메시지 발신자", example = "나")
            @NotBlank(message = "sender is required")
            String sender,
            @Schema(description = "메시지 시각", example = "2026-04-29T10:48:00")
            @NotNull(message = "dateTime is required")
            LocalDateTime dateTime,
            @Schema(description = "메시지 내용", example = "오늘 뭐해?")
            String content
    ) {
    }
}
