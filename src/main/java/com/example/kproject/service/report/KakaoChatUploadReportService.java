package com.example.kproject.service.report;

import com.example.kproject.domain.ReportMessage;
import com.example.kproject.dto.KakaoChatMessageDto;
import com.example.kproject.dto.KakaoChatSpecialType;
import com.example.kproject.dto.KakaoChatUploadResponse;
import com.example.kproject.dto.report.ReportGenerateRequest;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.exception.ReportGenerationException;
import com.example.kproject.service.KakaoChatFileParserService;
import com.example.kproject.util.KakaoChatParsingUtils;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KakaoChatUploadReportService {

    private static final String DEFAULT_CATEGORY = "일반 분석";
    private static final String ME_LABEL = "나";
    private static final String DEFAULT_OTHER_LABEL = "상대방";

    private final KakaoChatFileParserService kakaoChatFileParserService;
    private final ReportGenerationService reportGenerationService;

    public KakaoChatUploadReportService(
            KakaoChatFileParserService kakaoChatFileParserService,
            ReportGenerationService reportGenerationService
    ) {
        this.kakaoChatFileParserService = kakaoChatFileParserService;
        this.reportGenerationService = reportGenerationService;
    }

    public ReportResponse generateFromKakaoTxt(MultipartFile file, String category, String targetName) {
        KakaoChatUploadResponse parsedResponse = kakaoChatFileParserService.parse(file);
        ResolvedCounterpart resolvedCounterpart = resolveCounterpart(parsedResponse, targetName);

        List<ReportGenerateRequest.MessageDto> reportMessages = parsedResponse.messages().stream()
                .filter(message -> message.specialType() == KakaoChatSpecialType.TEXT)
                .map(message -> toReportMessage(message, resolvedCounterpart))
                .filter(message -> StringUtils.hasText(message.content()))
                .toList();

        if (reportMessages.isEmpty()) {
            throw new ReportGenerationException("No analyzable text messages were found in the uploaded file.");
        }

        String resolvedCategory = StringUtils.hasText(category) ? category.trim() : DEFAULT_CATEGORY;
        List<String> participants = buildParticipants(reportMessages, resolvedCounterpart);
        String analysisText = buildAnalysisText(reportMessages);

        return reportGenerationService.generate(new ReportGenerateRequest(
                resolvedCategory,
                participants,
                reportMessages,
                analysisText
        ));
    }

    public KakaoChatUploadResponse parseOnly(MultipartFile file, String category, String targetName) {
        KakaoChatUploadResponse parsedResponse = kakaoChatFileParserService.parse(file);
        return parsedResponse.withMeta(parsedResponse.meta().withCategoryAndTargetName(category, targetName));
    }

    private ReportGenerateRequest.MessageDto toReportMessage(
            KakaoChatMessageDto message,
            ResolvedCounterpart resolvedCounterpart
    ) {
        String normalizedSender = normalizeSender(message.sender(), resolvedCounterpart);
        String normalizedContent = KakaoChatParsingUtils.normalizeForAnalysis(message.content());

        return new ReportGenerateRequest.MessageDto(
                normalizedSender,
                LocalDateTime.parse(message.dateTime()),
                normalizedContent
        );
    }

    private String buildAnalysisText(List<ReportGenerateRequest.MessageDto> reportMessages) {
        List<ReportMessage> normalizedMessages = reportMessages.stream()
                .map(message -> new ReportMessage(message.sender(), message.dateTime(), message.content()))
                .toList();
        return ReportTextUtils.buildAnalysisText(normalizedMessages);
    }

    private List<String> buildParticipants(
            List<ReportGenerateRequest.MessageDto> reportMessages,
            ResolvedCounterpart resolvedCounterpart
    ) {
        boolean hasOtherParticipant = reportMessages.stream()
                .anyMatch(message -> !ME_LABEL.equals(message.sender()));

        if (!hasOtherParticipant) {
            return List.of(ME_LABEL);
        }

        return List.of(ME_LABEL, resolvedCounterpart.displayName());
    }

    private String normalizeSender(String sender, ResolvedCounterpart resolvedCounterpart) {
        if (resolvedCounterpart.originalSender() != null && resolvedCounterpart.originalSender().equals(sender)) {
            return resolvedCounterpart.displayName();
        }
        return ME_LABEL;
    }

    private ResolvedCounterpart resolveCounterpart(KakaoChatUploadResponse parsedResponse, String targetName) {
        List<String> distinctSenders = parsedResponse.messages().stream()
                .filter(message -> message.specialType() == KakaoChatSpecialType.TEXT)
                .map(KakaoChatMessageDto::sender)
                .distinct()
                .toList();

        String explicitTargetName = trimToNull(targetName);
        String roomName = parsedResponse.meta() == null ? null : trimToNull(parsedResponse.meta().roomName());

        String counterpartSender = matchSender(distinctSenders, explicitTargetName);
        if (counterpartSender == null) {
            counterpartSender = matchSender(distinctSenders, roomName);
        }
        if (counterpartSender == null && distinctSenders.size() == 2) {
            counterpartSender = distinctSenders.get(1);
        }
        if (counterpartSender == null && distinctSenders.size() == 1) {
            counterpartSender = distinctSenders.get(0);
        }

        String displayName;
        if (StringUtils.hasText(explicitTargetName)) {
            displayName = explicitTargetName;
        } else if (StringUtils.hasText(counterpartSender)) {
            displayName = counterpartSender;
        } else if (StringUtils.hasText(roomName)) {
            displayName = roomName;
        } else {
            displayName = DEFAULT_OTHER_LABEL;
        }

        return new ResolvedCounterpart(counterpartSender, displayName);
    }

    private String matchSender(List<String> distinctSenders, String hint) {
        if (!StringUtils.hasText(hint)) {
            return null;
        }

        for (String sender : distinctSenders) {
            if (sender.equals(hint)) {
                return sender;
            }
        }

        for (String sender : distinctSenders) {
            if (sender.contains(hint) || hint.contains(sender)) {
                return sender;
            }
        }

        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record ResolvedCounterpart(
            String originalSender,
            String displayName
    ) {
    }
}
