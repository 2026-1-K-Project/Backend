package com.example.kproject.controller;

import com.example.kproject.dto.report.ReportGenerateRequest;
import com.example.kproject.dto.report.ReportQuestionsResponse;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.service.report.ReportGenerationService;
import com.example.kproject.service.report.ReportQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports", description = "Conversation analysis report API")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportGenerationService reportGenerationService;
    private final ReportQueryService reportQueryService;

    public ReportController(ReportGenerationService reportGenerationService, ReportQueryService reportQueryService) {
        this.reportGenerationService = reportGenerationService;
        this.reportQueryService = reportQueryService;
    }

    @Operation(summary = "Generate a conversation report")
    @PostMapping("/generate")
    public ReportResponse generate(@Valid @RequestBody ReportGenerateRequest request) {
        return reportGenerationService.generate(request);
    }

    @Operation(summary = "Get a stored report")
    @GetMapping("/{reportId}")
    public ReportResponse getReport(@PathVariable Long reportId) {
        return reportQueryService.getReport(reportId);
    }

    @Operation(summary = "Get recommended questions from a stored report")
    @GetMapping("/{reportId}/questions")
    public ReportQuestionsResponse getRecommendedQuestions(@PathVariable Long reportId) {
        return reportQueryService.getRecommendedQuestions(reportId);
    }
}
