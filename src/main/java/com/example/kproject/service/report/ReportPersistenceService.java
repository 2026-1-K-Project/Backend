package com.example.kproject.service.report;

import com.example.kproject.domain.ConversationReport;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.exception.ReportGenerationException;
import com.example.kproject.repository.ConversationReportRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class ReportPersistenceService {

    private final ConversationReportRepository conversationReportRepository;
    private final ObjectMapper objectMapper;

    public ReportPersistenceService(
            ConversationReportRepository conversationReportRepository,
            ObjectMapper objectMapper
    ) {
        this.conversationReportRepository = conversationReportRepository;
        this.objectMapper = objectMapper;
    }

    public ReportResponse persist(ReportResponse draftResponse, String category, List<String> participants) {
        try {
            String participantsJson = objectMapper.writeValueAsString(participants == null ? List.of() : participants);
            String summaryJson = objectMapper.writeValueAsString(draftResponse.summary());

            ConversationReport saved = conversationReportRepository.save(
                    new ConversationReport(category, participantsJson, summaryJson, "{}")
            );

            ReportResponse persistedResponse = draftResponse.withReportId(saved.getId());
            String fullReportJson = objectMapper.writeValueAsString(persistedResponse);
            saved.updateStoredJson(summaryJson, fullReportJson);
            conversationReportRepository.save(saved);

            return persistedResponse;
        } catch (Exception exception) {
            throw new ReportGenerationException("Failed to serialize report json.", exception);
        }
    }
}
