package com.example.kproject.service.normalize;

import com.example.kproject.domain.KakaoChatParsedDocument;
import com.example.kproject.domain.NormalizedConversationResult;
import com.example.kproject.dto.KakaoChatMessageDto;
import com.example.kproject.dto.KakaoChatSpecialType;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.service.analysis.KeywordExtractionService;
import com.example.kproject.util.KakaoChatParsingUtils;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ConversationNormalizeService {

    private static final double STRUCTURED_SUCCESS_THRESHOLD = 0.45;
    private static final int MIN_STRUCTURED_MESSAGE_COUNT = 2;
    private static final String ME_LABEL = "사용자";
    private static final String DEFAULT_OTHER_LABEL = "상대방";
    private static final LocalDateTime SYNTHETIC_BASE_TIME = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final KeywordExtractionService keywordExtractionService;

    public ConversationNormalizeService(KeywordExtractionService keywordExtractionService) {
        this.keywordExtractionService = keywordExtractionService;
    }

    public NormalizedConversationResult normalize(KakaoChatParsedDocument parsedDocument, String targetName) {
        return normalize(parsedDocument, targetName, null);
    }

    public NormalizedConversationResult normalize(KakaoChatParsedDocument parsedDocument, String targetName, String myName) {
        String meName = resolveMeName(myName);
        ResolvedCounterpart counterpart = resolveCounterpart(parsedDocument, targetName, myName);
        List<NormalizedConversationDto.MessageDto> structuredMessages = toStructuredMessages(parsedDocument, counterpart, meName);
        boolean structuredAvailable = parsedDocument.supportsStructuredAnalysis(
                STRUCTURED_SUCCESS_THRESHOLD,
                MIN_STRUCTURED_MESSAGE_COUNT
        ) && structuredMessages.size() >= MIN_STRUCTURED_MESSAGE_COUNT;

        if (structuredAvailable) {
            NormalizedConversationDto conversation = buildConversation(
                    buildParticipants(structuredMessages, meName, counterpart.displayName()),
                    structuredMessages,
                    parsedDocument.rawText()
            );
            return new NormalizedConversationResult(
                    conversation,
                    ReportAnalysisMode.STRUCTURED,
                    true,
                    structuredWarning(parsedDocument)
            );
        }

        List<String> flexibleContents = extractFlexibleContents(parsedDocument);
        NormalizedConversationDto conversation = buildConversation(
                List.of(meName, counterpart.displayName()),
                toFlexibleMessages(flexibleContents, counterpart.displayName()),
                parsedDocument.rawText()
        );
        return new NormalizedConversationResult(
                conversation,
                ReportAnalysisMode.FLEXIBLE,
                false,
                flexibleWarning(parsedDocument)
        );
    }

    public NormalizedConversationResult normalizeRawText(String rawText, String targetName, String warning) {
        return normalizeRawText(rawText, targetName, null, warning);
    }

    public NormalizedConversationResult normalizeRawText(String rawText, String targetName, String myName, String warning) {
        String resolvedMyName = resolveMeName(myName);
        String resolvedTargetName = StringUtils.hasText(targetName) ? targetName.trim() : DEFAULT_OTHER_LABEL;
        List<String> contents = linesFromRawText(rawText);
        NormalizedConversationDto conversation = buildConversation(
                List.of(resolvedMyName, resolvedTargetName),
                toFlexibleMessages(contents, resolvedTargetName),
                rawText
        );
        return new NormalizedConversationResult(conversation, ReportAnalysisMode.FLEXIBLE, false, warning);
    }

    private NormalizedConversationDto buildConversation(
            List<String> participants,
            List<NormalizedConversationDto.MessageDto> messages,
            String rawText
    ) {
        List<String> textContents = messages.stream()
                .filter(message -> KakaoChatSpecialType.TEXT.name().equals(message.type()))
                .map(NormalizedConversationDto.MessageDto::content)
                .filter(ReportTextUtils::hasText)
                .toList();
        List<String> keywords = keywordExtractionService.extractFromContents(textContents);

        return new NormalizedConversationDto(
                participants == null ? List.of() : List.copyOf(participants),
                List.copyOf(messages),
                keywords,
                rawText == null ? "" : rawText
        );
    }

    private List<NormalizedConversationDto.MessageDto> toStructuredMessages(
            KakaoChatParsedDocument parsedDocument,
            ResolvedCounterpart counterpart,
            String meName
    ) {
        return parsedDocument.messages().stream()
                .filter(message -> StringUtils.hasText(message.dateTime()))
                .map(message -> toNormalizedMessage(message, counterpart, meName))
                .filter(message -> ReportTextUtils.hasText(message.content()))
                .toList();
    }

    private NormalizedConversationDto.MessageDto toNormalizedMessage(
            KakaoChatMessageDto message,
            ResolvedCounterpart counterpart,
            String meName
    ) {
        String sender = normalizeSender(message.sender(), counterpart, meName);
        String content = KakaoChatParsingUtils.normalizeForAnalysis(message.content());
        return new NormalizedConversationDto.MessageDto(
                sender,
                message.dateTime(),
                content,
                message.specialType().name()
        );
    }

    private List<NormalizedConversationDto.MessageDto> toFlexibleMessages(List<String> contents, String sender) {
        List<NormalizedConversationDto.MessageDto> messages = new ArrayList<>();
        for (int index = 0; index < contents.size(); index++) {
            String content = KakaoChatParsingUtils.normalizeForAnalysis(contents.get(index));
            if (!StringUtils.hasText(content)) {
                continue;
            }
            messages.add(new NormalizedConversationDto.MessageDto(
                    sender,
                    SYNTHETIC_BASE_TIME.plusMinutes(index).toString(),
                    content,
                    KakaoChatSpecialType.TEXT.name()
            ));
        }
        return messages;
    }

    private List<String> extractFlexibleContents(KakaoChatParsedDocument parsedDocument) {
        LinkedHashSet<String> contents = new LinkedHashSet<>();

        for (KakaoChatMessageDto message : parsedDocument.messages()) {
            if (message.specialType() != KakaoChatSpecialType.TEXT) {
                continue;
            }
            String normalized = KakaoChatParsingUtils.normalizeForAnalysis(message.content());
            if (StringUtils.hasText(normalized)) {
                contents.add(normalized);
            }
        }

        parsedDocument.errors().stream()
                .map(error -> KakaoChatParsingUtils.extractFlexibleContent(error.rawLine()))
                .flatMap(java.util.Optional::stream)
                .map(KakaoChatParsingUtils::normalizeForAnalysis)
                .filter(StringUtils::hasText)
                .forEach(contents::add);

        if (contents.isEmpty()) {
            parsedDocument.lines().stream()
                    .map(KakaoChatParsingUtils::extractFlexibleContent)
                    .flatMap(java.util.Optional::stream)
                    .map(KakaoChatParsingUtils::normalizeForAnalysis)
                    .filter(StringUtils::hasText)
                    .forEach(contents::add);
        }

        if (contents.isEmpty() && StringUtils.hasText(parsedDocument.rawText())) {
            contents.add(parsedDocument.rawText().trim());
        }

        return new ArrayList<>(contents);
    }

    private List<String> linesFromRawText(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return List.of();
        }
        return rawText.lines()
                .map(KakaoChatParsingUtils::extractFlexibleContent)
                .flatMap(java.util.Optional::stream)
                .map(KakaoChatParsingUtils::normalizeForAnalysis)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> buildParticipants(
            List<NormalizedConversationDto.MessageDto> messages,
            String meName,
            String counterpartName
    ) {
        boolean hasCounterpart = messages.stream().anyMatch(message -> !meName.equals(message.sender()));
        if (!hasCounterpart) {
            return List.of(meName);
        }
        return List.of(meName, counterpartName);
    }

    private String normalizeSender(String sender, ResolvedCounterpart counterpart, String meName) {
        if (counterpart.originalSender() != null && counterpart.originalSender().equals(sender)) {
            return counterpart.displayName();
        }
        return meName;
    }

    private ResolvedCounterpart resolveCounterpart(KakaoChatParsedDocument parsedDocument, String targetName) {
        return resolveCounterpart(parsedDocument, targetName, null);
    }

    private ResolvedCounterpart resolveCounterpart(KakaoChatParsedDocument parsedDocument, String targetName, String myName) {
        List<String> distinctSenders = parsedDocument.messages().stream()
                .filter(message -> message.specialType() == KakaoChatSpecialType.TEXT)
                .map(KakaoChatMessageDto::sender)
                .distinct()
                .toList();

        String explicitTargetName = trimToNull(targetName);
        String explicitMyName = trimToNull(myName);
        String roomName = parsedDocument.meta() == null ? null : trimToNull(parsedDocument.meta().roomName());
        String mySender = matchSender(distinctSenders, explicitMyName);
        String counterpartSender = matchSender(distinctSenders, explicitTargetName);

        if (counterpartSender == null) {
            counterpartSender = matchSender(distinctSenders, roomName);
        }
        if (counterpartSender == null && mySender != null) {
            counterpartSender = distinctSenders.stream()
                    .filter(sender -> !sender.equals(mySender))
                    .findFirst()
                    .orElse(null);
        }
        if (counterpartSender == null && distinctSenders.size() >= 2) {
            counterpartSender = distinctSenders.get(1);
        }
        if (counterpartSender == null && distinctSenders.size() == 1) {
            counterpartSender = distinctSenders.get(0);
        }

        String displayName = explicitTargetName;
        if (!StringUtils.hasText(displayName)) {
            displayName = StringUtils.hasText(counterpartSender) ? counterpartSender : roomName;
        }
        if (!StringUtils.hasText(displayName)) {
            displayName = DEFAULT_OTHER_LABEL;
        }

        return new ResolvedCounterpart(counterpartSender, displayName);
    }

    private String resolveMeName(String myName) {
        String explicitMyName = trimToNull(myName);
        return StringUtils.hasText(explicitMyName) ? explicitMyName : ME_LABEL;
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

    private String structuredWarning(KakaoChatParsedDocument parsedDocument) {
        if (parsedDocument.errors().isEmpty()
                && !parsedDocument.parsingStats().usedSavedAtDateFallback()
                && parsedDocument.parsingStats().successRate() >= 0.95) {
            return null;
        }
        if (parsedDocument.parsingStats().usedSavedAtDateFallback()) {
            return "일부 메시지의 날짜 구분선이 없어 저장 날짜를 기준으로 시각을 보정했습니다.";
        }
        return "일부 줄이 완전히 구조화되지 않아 정량 지표에 약간의 오차가 있을 수 있습니다.";
    }

    private String flexibleWarning(KakaoChatParsedDocument parsedDocument) {
        if (parsedDocument.messages().isEmpty()) {
            return "메시지 구조를 거의 식별하지 못해 전체 텍스트 기반 자유 분석으로 전환했습니다.";
        }
        return "파일 형식이 불완전해 구조화 성공률이 낮습니다. 정성 분석 중심으로 결과를 제공합니다.";
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
