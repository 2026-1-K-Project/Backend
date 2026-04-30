package com.example.kproject.service.report;

import com.example.kproject.domain.ConversationReport;
import com.example.kproject.dto.report.ReportQuestionsResponse;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.exception.ReportGenerationException;
import com.example.kproject.exception.ReportNotFoundException;
import com.example.kproject.repository.ConversationReportRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ReportQueryService {

    private final ConversationReportRepository conversationReportRepository;
    private final ObjectMapper objectMapper;

    public ReportQueryService(ConversationReportRepository conversationReportRepository, ObjectMapper objectMapper) {
        this.conversationReportRepository = conversationReportRepository;
        this.objectMapper = objectMapper;
    }

    public ReportResponse getReport(Long reportId) {
        ConversationReport report = conversationReportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        try {
            ReportResponse response = objectMapper.readValue(report.getFullReportJson(), ReportResponse.class);
            return response.withReportId(report.getId());
        } catch (Exception exception) {
            throw new ReportGenerationException("Failed to read stored report json.", exception);
        }
    }

    public ReportQuestionsResponse getRecommendedQuestions(Long reportId) {
        ReportResponse response = getReport(reportId);
        return new ReportQuestionsResponse(reportId, response.actionableInsights().recommendedQuestions());
    }
}
