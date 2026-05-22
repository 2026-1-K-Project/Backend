package com.example.kproject.dto.upload;

import com.example.kproject.dto.report.ReportAnalysisMode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비정형 대화 파일 업로드 및 정형화 결과 응답")
public record ChatUploadResponse(
        @Schema(description = "생성된 리포트 ID. 이후 모든 분석 조회 API에서 사용합니다.", example = "1")
        Long reportId,
        @Schema(description = "업로드 및 정형화 처리 상태", example = "COMPLETED")
        String status,
        @Schema(
                description = "정형화 결과 모드. OpenAI 또는 로컬 파서가 메시지 구조를 충분히 만들면 STRUCTURED, 제한적인 텍스트만 확보하면 FLEXIBLE입니다.",
                example = "STRUCTURED"
        )
        ReportAnalysisMode analysisMode,
        @Schema(description = "업로드 처리 메시지", example = "대화 업로드 및 정형화가 완료되었습니다.")
        String message,
        @Schema(
                description = "정형화 과정의 경고 메시지. OpenAI 사용 여부, fallback 여부, 구조화 한계 등을 안내하며 없으면 null입니다.",
                example = "OpenAI를 사용해 비정형 원본을 정형화했습니다."
        )
        String warning
) {
}
