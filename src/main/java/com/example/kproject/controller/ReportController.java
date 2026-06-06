package com.example.kproject.controller;

import com.example.kproject.dto.report.AppReportResultResponse;
import com.example.kproject.dto.report.ReportDetailResponse;
import com.example.kproject.dto.report.ReportInsightsResponse;
import com.example.kproject.dto.report.ReportPersonalityResponse;
import com.example.kproject.dto.report.ReportPreferencesResponse;
import com.example.kproject.dto.report.ReportQuestionsResponse;
import com.example.kproject.dto.report.ReportRelationshipResponse;
import com.example.kproject.dto.report.ReportSummaryResponse;
import com.example.kproject.service.report.ReportSectionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리포트", description = "대화 분석 리포트 API")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportSectionQueryService reportSectionQueryService;

    public ReportController(ReportSectionQueryService reportSectionQueryService) {
        this.reportSectionQueryService = reportSectionQueryService;
    }

    @Operation(
            summary = "저장된 리포트 조회",
            description = "DB에 저장된 리포트의 기본 정보와 처리 상태를 reportId 기준으로 조회합니다."
    )
    @GetMapping("/{reportId}")
    public ReportDetailResponse getReport(
            @Parameter(description = "조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        return reportSectionQueryService.getDetail(reportId);
    }

    @Operation(
            summary = "종합 요약 조회",
            description = "호감 지수, 한 줄 요약, 핵심 키워드, 기본 분위기 요약을 조회합니다."
    )
    @GetMapping("/{reportId}/summary")
    public ReportSummaryResponse getSummary(
            @Parameter(description = "요약을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        return reportSectionQueryService.getSummary(reportId);
    }

    @Operation(
            summary = "관계 분석 조회",
            description = "호감도, 친밀도, 대화 점유율, 평균 답장 시간, 언어 동기화, 감정 흐름을 조회합니다."
    )
    @GetMapping("/{reportId}/relationship")
    public ReportRelationshipResponse getRelationship(
            @Parameter(description = "관계 분석을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        return reportSectionQueryService.getRelationship(reportId);
    }

    @Operation(
            summary = "성격 및 심리 분석 조회",
            description = "예상 MBTI, Big Five, 애착 유형, 말투와 감정 표현 스타일을 조회합니다."
    )
    @GetMapping("/{reportId}/personality")
    public ReportPersonalityResponse getPersonality(
            @Parameter(description = "성격 분석을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        return reportSectionQueryService.getPersonality(reportId);
    }

    @Operation(
            summary = "선호 분석 조회",
            description = "상대방이 좋아할 가능성이 높은 말과 행동, 선호 주제, 부담스러운 표현을 조회합니다."
    )
    @GetMapping("/{reportId}/preferences")
    public ReportPreferencesResponse getPreferences(
            @Parameter(description = "선호 분석을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        return reportSectionQueryService.getPreferences(reportId);
    }

    @Operation(
            summary = "인사이트 및 추천 조회",
            description = "대화 팁, 주의할 점, 결정적 순간, 추천 질문과 추천 답장을 조회합니다."
    )
    @GetMapping("/{reportId}/insights")
    public ReportInsightsResponse getInsights(
            @Parameter(description = "인사이트를 조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        return reportSectionQueryService.getInsights(reportId);
    }

    @Operation(
            summary = "저장된 리포트의 추천 질문 조회",
            description = "저장된 리포트의 인사이트 분석에서 추천 질문 목록만 별도로 조회합니다."
    )
    @GetMapping("/{reportId}/questions")
    public ReportQuestionsResponse getRecommendedQuestions(
            @Parameter(description = "추천 질문을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        ReportInsightsResponse insights = reportSectionQueryService.getInsights(reportId);
        return new ReportQuestionsResponse(reportId, insights.recommendedQuestions());
    }

    @Operation(
            summary = "앱용 통합 결과 조회",
            description = "React Native 결과 화면의 AnalysisData 구조에 맞춰 주요 분석 값을 한 번에 조회합니다."
    )
    @GetMapping("/{reportId}/app-result")
    public AppReportResultResponse getAppResult(
            @Parameter(description = "앱용 결과를 조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        return reportSectionQueryService.getAppResult(reportId);
    }
}
