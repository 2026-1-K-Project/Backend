package com.example.kproject.controller;

import com.example.kproject.dto.report.AppReportResultResponse;
import com.example.kproject.dto.report.ReportDetailResponse;
import com.example.kproject.dto.report.ReportInsightsResponse;
import com.example.kproject.dto.report.ReportListItemResponse;
import com.example.kproject.dto.report.ReportPersonalityResponse;
import com.example.kproject.dto.report.ReportPreferencesResponse;
import com.example.kproject.dto.report.ReportQuestionsResponse;
import com.example.kproject.dto.report.ReportRelationshipResponse;
import com.example.kproject.dto.report.ReportStatusResponse;
import com.example.kproject.dto.report.ReportSummaryResponse;
import com.example.kproject.service.report.ReportSectionQueryService;
import com.example.kproject.service.report.ReportStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "리포트", description = "대화 분석 리포트 API")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportSectionQueryService reportSectionQueryService;
    private final ReportStorageService reportStorageService;

    public ReportController(
            ReportSectionQueryService reportSectionQueryService,
            ReportStorageService reportStorageService
    ) {
        this.reportSectionQueryService = reportSectionQueryService;
        this.reportStorageService = reportStorageService;
    }

    @Operation(summary = "보관함 리포트 목록 조회")
    @GetMapping
    public List<ReportListItemResponse> listReports(
            @Parameter(description = "로그인 사용자 memberId. 없으면 게스트 리포트만 조회합니다.", example = "1")
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        return reportStorageService.listReports(memberId, false);
    }

    @Operation(summary = "휴지통 리포트 목록 조회")
    @GetMapping("/trash")
    public List<ReportListItemResponse> listTrash(
            @Parameter(description = "로그인 사용자 memberId. 없으면 게스트 휴지통만 조회합니다.", example = "1")
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        return reportStorageService.listReports(memberId, true);
    }

    @Operation(summary = "저장된 리포트 조회")
    @GetMapping("/{reportId}")
    public ReportDetailResponse getReport(
            @Parameter(description = "조회할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.assertReportOwner(reportId, memberId);
        return reportSectionQueryService.getDetail(reportId);
    }

    @Operation(summary = "리포트 처리 상태 조회")
    @GetMapping("/{reportId}/status")
    public ReportStatusResponse getStatus(
            @Parameter(description = "상태를 조회할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        return reportStorageService.getStatus(reportId, memberId);
    }

    @Operation(summary = "종합 요약 조회")
    @GetMapping("/{reportId}/summary")
    public ReportSummaryResponse getSummary(
            @Parameter(description = "요약을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.assertReportOwner(reportId, memberId);
        return reportSectionQueryService.getSummary(reportId);
    }

    @Operation(summary = "관계 분석 조회")
    @GetMapping("/{reportId}/relationship")
    public ReportRelationshipResponse getRelationship(
            @Parameter(description = "관계 분석을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.assertReportOwner(reportId, memberId);
        return reportSectionQueryService.getRelationship(reportId);
    }

    @Operation(summary = "성격 및 심리 분석 조회")
    @GetMapping("/{reportId}/personality")
    public ReportPersonalityResponse getPersonality(
            @Parameter(description = "성격 분석을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.assertReportOwner(reportId, memberId);
        return reportSectionQueryService.getPersonality(reportId);
    }

    @Operation(summary = "선호 분석 조회")
    @GetMapping("/{reportId}/preferences")
    public ReportPreferencesResponse getPreferences(
            @Parameter(description = "선호 분석을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.assertReportOwner(reportId, memberId);
        return reportSectionQueryService.getPreferences(reportId);
    }

    @Operation(summary = "인사이트 및 추천 조회")
    @GetMapping("/{reportId}/insights")
    public ReportInsightsResponse getInsights(
            @Parameter(description = "인사이트를 조회할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.assertReportOwner(reportId, memberId);
        return reportSectionQueryService.getInsights(reportId);
    }

    @Operation(summary = "저장된 리포트의 추천 질문 조회")
    @GetMapping("/{reportId}/questions")
    public ReportQuestionsResponse getRecommendedQuestions(
            @Parameter(description = "추천 질문을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.assertReportOwner(reportId, memberId);
        ReportInsightsResponse insights = reportSectionQueryService.getInsights(reportId);
        return new ReportQuestionsResponse(reportId, insights.recommendedQuestions());
    }

    @Operation(summary = "앱용 통합 결과 조회")
    @GetMapping("/{reportId}/app-result")
    public AppReportResultResponse getAppResult(
            @Parameter(description = "앱용 결과를 조회할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.assertReportOwner(reportId, memberId);
        return reportSectionQueryService.getAppResult(reportId);
    }

    @Operation(summary = "리포트를 휴지통으로 이동")
    @PatchMapping("/{reportId}/trash")
    public ReportListItemResponse moveToTrash(
            @Parameter(description = "휴지통으로 이동할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        return reportStorageService.moveToTrash(reportId, memberId);
    }

    @Operation(summary = "휴지통 리포트 복원")
    @PatchMapping("/{reportId}/restore")
    public ReportListItemResponse restore(
            @Parameter(description = "복원할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        return reportStorageService.restore(reportId, memberId);
    }

    @Operation(summary = "리포트 영구 삭제")
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(
            @Parameter(description = "삭제할 리포트 ID", required = true)
            @PathVariable Long reportId,
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.deleteReport(reportId, memberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "휴지통 비우기")
    @DeleteMapping("/trash")
    public ResponseEntity<Void> emptyTrash(
            @Parameter(description = "로그인 사용자 memberId. 없으면 게스트 휴지통만 비웁니다.", example = "1")
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        reportStorageService.emptyTrash(memberId);
        return ResponseEntity.noContent().build();
    }
}
