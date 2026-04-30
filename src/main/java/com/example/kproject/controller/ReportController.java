package com.example.kproject.controller;

import com.example.kproject.dto.report.ReportGenerateRequest;
import com.example.kproject.dto.report.ReportQuestionsResponse;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.service.report.ReportGenerationService;
import com.example.kproject.service.report.ReportQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리포트", description = "대화 분석 리포트 API")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportGenerationService reportGenerationService;
    private final ReportQueryService reportQueryService;

    public ReportController(ReportGenerationService reportGenerationService, ReportQueryService reportQueryService) {
        this.reportGenerationService = reportGenerationService;
        this.reportQueryService = reportQueryService;
    }

    @Operation(
            summary = "종합 분석 리포트 생성",
            description = "구조화된 대화 메시지와 분석 텍스트를 기반으로 프론트 UI에서 바로 사용할 수 있는 종합 분석 리포트를 생성합니다."
    )
    @PostMapping("/generate")
    public ReportResponse generate(@Valid @RequestBody ReportGenerateRequest request) {
        return reportGenerationService.generate(request);
    }

    @Operation(
            summary = "저장된 리포트 조회",
            description = "DB에 저장된 종합 분석 리포트를 reportId 기준으로 조회합니다."
    )
    @GetMapping("/{reportId}")
    public ReportResponse getReport(
            @Parameter(description = "조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        return reportQueryService.getReport(reportId);
    }

    @Operation(
            summary = "저장된 리포트의 추천 질문 조회",
            description = "저장된 종합 분석 리포트에서 추천 질문 목록만 별도로 조회합니다."
    )
    @GetMapping("/{reportId}/questions")
    public ReportQuestionsResponse getRecommendedQuestions(
            @Parameter(description = "추천 질문을 조회할 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        return reportQueryService.getRecommendedQuestions(reportId);
    }
}
