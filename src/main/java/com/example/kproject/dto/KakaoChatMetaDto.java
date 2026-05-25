package com.example.kproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오톡 대화 파일 메타데이터")
public record KakaoChatMetaDto(
        @Schema(description = "원본 제목", example = "홍길동 님과 카카오톡 대화")
        String title,
        @Schema(description = "파일 저장 시각", example = "2026-04-29 18:13:04")
        String savedAt,
        @Schema(description = "대화방 이름 또는 추정 대상 이름", example = "홍길동")
        String roomName,
        @Schema(description = "분석 카테고리", example = "썸/연애")
        String category,
        @Schema(description = "분석 대상 이름", example = "홍길동")
        String targetName
) {
    public KakaoChatMetaDto withCategoryAndTargetName(String category, String targetName) {
        return new KakaoChatMetaDto(title, savedAt, roomName, category, targetName);
    }
}
