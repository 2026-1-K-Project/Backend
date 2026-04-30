package com.example.kproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "카카오톡 txt 파싱 결과 응답")
public record KakaoChatUploadResponse(
        @Schema(description = "업로드 파일 메타데이터")
        KakaoChatMetaDto meta,
        @Schema(description = "파싱된 메시지 수", example = "42")
        int messageCount,
        @Schema(description = "파싱된 메시지 목록")
        List<KakaoChatMessageDto> messages,
        @Schema(description = "분석용으로 정리된 평문 텍스트")
        String analysisText,
        @Schema(description = "파싱 중 수집된 오류 목록")
        List<KakaoChatParseErrorDto> errors
) {
    public KakaoChatUploadResponse withMeta(KakaoChatMetaDto meta) {
        return new KakaoChatUploadResponse(meta, messageCount, messages, analysisText, errors);
    }
}
