package com.example.kproject.service.report;

import com.example.kproject.domain.ChatSourceType;
import com.example.kproject.domain.ConversationReport;
import com.example.kproject.domain.NormalizedConversationResult;
import com.example.kproject.domain.ReportStatus;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.dto.report.ReportListItemResponse;
import com.example.kproject.dto.report.ReportStatusResponse;
import com.example.kproject.exception.ReportGenerationException;
import com.example.kproject.exception.ReportNotFoundException;
import com.example.kproject.repository.ConversationReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        return createReport(category, sourceType, normalizedResult, description, uploadedFileCount, null);
    }

    public ConversationReport createReport(
            String category,
            ChatSourceType sourceType,
            NormalizedConversationResult normalizedResult,
            String description,
            int uploadedFileCount,
            Long memberId
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
                    uploadedFileCount,
                    memberId
            ));
        } catch (Exception exception) {
            throw new ReportGenerationException(
                    "Failed to store normalized conversation: " + rootCauseMessage(exception),
                    exception
            );
        }
    }

    public List<ReportListItemResponse> listReports(Long memberId, boolean trashed) {
        List<ConversationReport> reports = memberId == null
                ? (trashed
                ? conversationReportRepository.findByMemberIdIsNullAndTrashedTrueOrderByTrashedAtDesc()
                : conversationReportRepository.findByMemberIdIsNullAndTrashedFalseOrderByCreatedAtDesc())
                : (trashed
                ? conversationReportRepository.findByMemberIdAndTrashedTrueOrderByTrashedAtDesc(memberId)
                : conversationReportRepository.findByMemberIdAndTrashedFalseOrderByCreatedAtDesc(memberId));
        return reports.stream().map(this::toListItem).toList();
    }

    public ReportStatusResponse getStatus(Long reportId) {
        ConversationReport report = getReport(reportId);
        boolean completed = "COMPLETED".equals(report.getStatus());
        return new ReportStatusResponse(
                report.getId(),
                safeStatus(report),
                completed ? 100 : 50,
                completed ? "분석이 완료되었습니다." : "분석을 처리하는 중입니다.",
                safeAnalysisMode(report.getAnalysisMode()),
                report.getWarning()
        );
    }

    @Transactional
    public ReportListItemResponse moveToTrash(Long reportId) {
        ConversationReport report = getReport(reportId);
        report.moveToTrash();
        return toListItem(report);
    }

    @Transactional
    public ReportListItemResponse restore(Long reportId) {
        ConversationReport report = getReport(reportId);
        report.restoreFromTrash();
        return toListItem(report);
    }

    @Transactional
    public void deleteReport(Long reportId) {
        ConversationReport report = getReport(reportId);
        conversationReportRepository.delete(report);
    }

    @Transactional
    public void emptyTrash(Long memberId) {
        List<ConversationReport> trashedReports = memberId == null
                ? conversationReportRepository.findByMemberIdIsNullAndTrashedTrueOrderByTrashedAtDesc()
                : conversationReportRepository.findByMemberIdAndTrashedTrueOrderByTrashedAtDesc(memberId);
        conversationReportRepository.deleteAll(trashedReports);
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

    private ReportListItemResponse toListItem(ConversationReport report) {
        return new ReportListItemResponse(
                report.getId(),
                report.getMemberId(),
                safeTitle(report),
                safeCategory(report),
                safeSourceType(report),
                report.getMessageCount(),
                report.getUploadedFileCount(),
                safeStatus(report),
                safeAnalysisMode(report.getAnalysisMode()),
                readInterestScore(report),
                report.getDescription(),
                report.isTrashed(),
                report.getCreatedAt(),
                report.getTrashedAt()
        );
    }

    private String safeTitle(ConversationReport report) {
        if (report.getTitle() != null && !report.getTitle().isBlank()) {
            return report.getTitle();
        }
        String category = report.getCategory() == null || report.getCategory().isBlank()
                ? "대화"
                : report.getCategory();
        return category + " 분석 리포트";
    }

    private String safeCategory(ConversationReport report) {
        return report.getCategory() == null || report.getCategory().isBlank()
                ? "일반 분석"
                : report.getCategory();
    }

    private String safeSourceType(ConversationReport report) {
        return report.getSourceType() == null || report.getSourceType().isBlank()
                ? "TXT"
                : report.getSourceType();
    }

    private String safeStatus(ConversationReport report) {
        return report.getStatus() == null || report.getStatus().isBlank()
                ? "COMPLETED"
                : report.getStatus();
    }

    private ReportAnalysisMode safeAnalysisMode(String analysisMode) {
        if (analysisMode == null || analysisMode.isBlank()) {
            return ReportAnalysisMode.FLEXIBLE;
        }
        try {
            return ReportAnalysisMode.valueOf(analysisMode);
        } catch (IllegalArgumentException exception) {
            return ReportAnalysisMode.FLEXIBLE;
        }
    }

    private int readInterestScore(ConversationReport report) {
        try {
            if (report.getSummaryJson() == null || "{}".equals(report.getSummaryJson())) {
                return 0;
            }
            return objectMapper.readTree(report.getSummaryJson()).path("interestScore").asInt(0);
        } catch (Exception ignored) {
            return 0;
        }
    }
}
