package com.example.kproject.dto.upload;

import com.example.kproject.dto.report.ReportAnalysisMode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대화 업로드 및 리포트 생성 응답")
public record ChatUploadResponse(
        @Schema(description = "생성된 리포트 ID", example = "1")
        Long reportId,
        @Schema(description = "리포트 처리 상태", example = "COMPLETED")
        String status,
        @Schema(description = "분석 모드", example = "STRUCTURED")
        ReportAnalysisMode analysisMode,
        @Schema(description = "업로드 처리 메시지")
        String message,
        @Schema(description = "분석 경고 메시지. 없으면 null")
        String warning
) {
}
