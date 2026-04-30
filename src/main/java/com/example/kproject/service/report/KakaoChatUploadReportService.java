package com.example.kproject.service.report;

import com.example.kproject.domain.KakaoChatParsedDocument;
import com.example.kproject.dto.KakaoChatMessageDto;
import com.example.kproject.dto.KakaoChatSpecialType;
import com.example.kproject.dto.KakaoChatUploadResponse;
import com.example.kproject.dto.report.ReportGenerateRequest;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.service.KakaoChatFileParserService;
import com.example.kproject.util.KakaoChatParsingUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KakaoChatUploadReportService {

    private static final double STRUCTURED_SUCCESS_THRESHOLD = 0.45;
    private static final int MIN_STRUCTURED_MESSAGE_COUNT = 2;
    private static final String DEFAULT_CATEGORY = "일반 분석";
    private static final String ME_LABEL = "나";
    private static final String DEFAULT_OTHER_LABEL = "상대방";

    private final KakaoChatFileParserService kakaoChatFileParserService;
    private final ReportGenerationService reportGenerationService;
    private final FlexibleTextReportService flexibleTextReportService;

    public KakaoChatUploadReportService(
            KakaoChatFileParserService kakaoChatFileParserService,
            ReportGenerationService reportGenerationService,
            FlexibleTextReportService flexibleTextReportService
    ) {
        this.kakaoChatFileParserService = kakaoChatFileParserService;
        this.reportGenerationService = reportGenerationService;
        this.flexibleTextReportService = flexibleTextReportService;
    }

    public ReportResponse generateFromKakaoTxt(MultipartFile file, String category, String targetName) {
        KakaoChatParsedDocument parsedDocument = kakaoChatFileParserService.parseDocument(file);
        String resolvedCategory = StringUtils.hasText(category) ? category.trim() : DEFAULT_CATEGORY;
        ResolvedCounterpart resolvedCounterpart = resolveCounterpart(parsedDocument, targetName);
        List<ReportGenerateRequest.MessageDto> structuredMessages = toStructuredMessages(parsedDocument, resolvedCounterpart);

        boolean structuredMode = parsedDocument.supportsStructuredAnalysis(
                STRUCTURED_SUCCESS_THRESHOLD,
                MIN_STRUCTURED_MESSAGE_COUNT
        ) && structuredMessages.size() >= MIN_STRUCTURED_MESSAGE_COUNT;

        if (structuredMode) {
            return reportGenerationService.generate(
                    new ReportGenerateRequest(
                            resolvedCategory,
                            buildParticipants(structuredMessages, resolvedCounterpart),
                            structuredMessages,
                            buildStructuredAnalysisText(structuredMessages)
                    ),
                    buildStructuredWarning(parsedDocument)
            );
        }

        return flexibleTextReportService.generate(
                resolvedCategory,
                resolvedCounterpart.displayName(),
                parsedDocument,
                buildFlexibleWarning(parsedDocument)
        );
    }

    public KakaoChatUploadResponse parseOnly(MultipartFile file, String category, String targetName) {
        KakaoChatUploadResponse parsedResponse = kakaoChatFileParserService.parse(file);
        return parsedResponse.withMeta(parsedResponse.meta().withCategoryAndTargetName(category, targetName));
    }

    private List<ReportGenerateRequest.MessageDto> toStructuredMessages(
            KakaoChatParsedDocument parsedDocument,
            ResolvedCounterpart resolvedCounterpart
    ) {
        return parsedDocument.messages().stream()
                .filter(message -> message.specialType() == KakaoChatSpecialType.TEXT)
                .filter(message -> StringUtils.hasText(message.dateTime()))
                .map(message -> toStructuredMessage(message, resolvedCounterpart))
                .filter(message -> StringUtils.hasText(message.content()))
                .toList();
    }

    private ReportGenerateRequest.MessageDto toStructuredMessage(
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

    private String buildStructuredAnalysisText(List<ReportGenerateRequest.MessageDto> reportMessages) {
        return reportMessages.stream()
                .map(message -> message.sender() + ": " + message.content())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
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

    private ResolvedCounterpart resolveCounterpart(KakaoChatParsedDocument parsedDocument, String targetName) {
        List<String> distinctSenders = parsedDocument.messages().stream()
                .filter(message -> message.specialType() == KakaoChatSpecialType.TEXT)
                .map(KakaoChatMessageDto::sender)
                .distinct()
                .toList();

        String explicitTargetName = trimToNull(targetName);
        String roomName = parsedDocument.meta() == null ? null : trimToNull(parsedDocument.meta().roomName());

        String counterpartSender = matchSender(distinctSenders, explicitTargetName);
        if (counterpartSender == null) {
            counterpartSender = matchSender(distinctSenders, roomName);
        }
        if (counterpartSender == null && distinctSenders.size() >= 2) {
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

    private String buildStructuredWarning(KakaoChatParsedDocument parsedDocument) {
        if (parsedDocument.errors().isEmpty()
                && !parsedDocument.parsingStats().usedSavedAtDateFallback()
                && parsedDocument.parsingStats().successRate() >= 0.95) {
            return null;
        }

        if (parsedDocument.parsingStats().usedSavedAtDateFallback()) {
            return "일부 메시지는 날짜 구분선이 없어 저장 날짜를 기준으로 시각을 보정했습니다.";
        }

        return "일부 줄은 완전히 구조화되지 않아 정량 지표 해석에 약간의 오차가 있을 수 있습니다.";
    }

    private String buildFlexibleWarning(KakaoChatParsedDocument parsedDocument) {
        if (parsedDocument.messages().isEmpty()) {
            return "메시지 구조를 거의 식별하지 못해 전체 텍스트 기반 자유 분석으로 전환했습니다.";
        }

        return "형식이 불완전해 구조화 성공률이 낮았습니다. 정량 지표 대신 텍스트 기반 자유 분석 중심으로 반환합니다.";
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
