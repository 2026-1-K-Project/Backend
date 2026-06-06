package com.example.kproject.service.report;

import com.example.kproject.domain.ChatSourceType;
import com.example.kproject.domain.ConversationReport;
import com.example.kproject.domain.NormalizedConversationResult;
import com.example.kproject.domain.ReportStatus;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.exception.ReportGenerationException;
import com.example.kproject.exception.ReportNotFoundException;
import com.example.kproject.repository.ConversationReportRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class ReportStorageService {

    private final ConversationReportRepository conversationReportRepository;
    private final ObjectMapper objectMapper;

    public ReportStorageService(
            ConversationReportRepository conversationReportRepository,
            ObjectMapper objectMapper
    ) {
        this.conversationReportRepository = conversationReportRepository;
        this.objectMapper = objectMapper;
    }

    public ConversationReport createReport(
            String category,
            ChatSourceType sourceType,
            NormalizedConversationResult normalizedResult
    ) {
        return createReport(category, sourceType, normalizedResult, null, 1);
    }

    public ConversationReport createReport(
            String category,
            ChatSourceType sourceType,
            NormalizedConversationResult normalizedResult,
            String description,
            int uploadedFileCount
    ) {
        try {
            NormalizedConversationDto conversation = normalizedResult.conversation();
            String participantsJson = objectMapper.writeValueAsString(conversation.participants());
            String normalizedJson = objectMapper.writeValueAsString(conversation);
            return conversationReportRepository.save(new ConversationReport(
                    category,
                    sourceType,
                    conversation.rawText(),
                    normalizedJson,
                    participantsJson,
                    conversation.messages().size(),
                    ReportStatus.COMPLETED,
                    normalizedResult.analysisMode().name(),
                    normalizedResult.warning(),
                    description,
                    uploadedFileCount
            ));
        } catch (Exception exception) {
            throw new ReportGenerationException(
                    "Failed to store normalized conversation: " + rootCauseMessage(exception),
                    exception
            );
        }
    }

    public ConversationReport getReport(Long reportId) {
        return conversationReportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
    }

    public NormalizedConversationDto readNormalizedConversation(ConversationReport report) {
        try {
            return objectMapper.readValue(report.getNormalizedJson(), NormalizedConversationDto.class);
        } catch (Exception exception) {
            throw new ReportGenerationException("Failed to read normalized conversation.", exception);
        }
    }

    public List<String> readParticipants(ConversationReport report) {
        try {
            String[] participants = objectMapper.readValue(report.getParticipantsJson(), String[].class);
            return List.of(participants);
        } catch (Exception exception) {
            throw new ReportGenerationException("Failed to read report participants.", exception);
        }
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }
}
