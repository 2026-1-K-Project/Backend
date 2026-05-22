package com.example.kproject.service.report;

import com.example.kproject.domain.AnalysisResult;
import com.example.kproject.domain.AnalysisType;
import com.example.kproject.domain.ConversationReport;
import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.dto.report.ReportDetailResponse;
import com.example.kproject.dto.report.ReportInsightsResponse;
import com.example.kproject.dto.report.ReportPersonalityResponse;
import com.example.kproject.dto.report.ReportPreferencesResponse;
import com.example.kproject.dto.report.ReportRelationshipResponse;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.dto.report.ReportSummaryResponse;
import com.example.kproject.exception.ReportGenerationException;
import com.example.kproject.repository.AnalysisResultRepository;
import com.example.kproject.service.analysis.DecisiveMomentAnalysisService;
import com.example.kproject.service.analysis.EmotionTimelineAnalysisService;
import com.example.kproject.service.analysis.InterestScoreAnalysisService;
import com.example.kproject.service.analysis.KeywordExtractionService;
import com.example.kproject.service.analysis.LanguageSyncAnalysisService;
import com.example.kproject.service.analysis.PersonalityPsychologyAnalysisService;
import com.example.kproject.service.analysis.PreferenceAnalysisService;
import com.example.kproject.service.analysis.QualitativeSignalsAnalysisService;
import com.example.kproject.service.analysis.ReplyTimeAnalysisService;
import com.example.kproject.service.analysis.TalkRatioAnalysisService;
import com.example.kproject.service.insight.ActionableInsightService;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class ReportSectionQueryService {

    private final ReportStorageService reportStorageService;
    private final AnalysisResultRepository analysisResultRepository;
    private final ObjectMapper objectMapper;
    private final TalkRatioAnalysisService talkRatioAnalysisService;
    private final ReplyTimeAnalysisService replyTimeAnalysisService;
    private final InterestScoreAnalysisService interestScoreAnalysisService;
    private final LanguageSyncAnalysisService languageSyncAnalysisService;
    private final KeywordExtractionService keywordExtractionService;
    private final PersonalityPsychologyAnalysisService personalityPsychologyAnalysisService;
    private final EmotionTimelineAnalysisService emotionTimelineAnalysisService;
    private final DecisiveMomentAnalysisService decisiveMomentAnalysisService;
    private final ActionableInsightService actionableInsightService;
    private final QualitativeSignalsAnalysisService qualitativeSignalsAnalysisService;
    private final PreferenceAnalysisService preferenceAnalysisService;

    public ReportSectionQueryService(
            ReportStorageService reportStorageService,
            AnalysisResultRepository analysisResultRepository,
            ObjectMapper objectMapper,
            TalkRatioAnalysisService talkRatioAnalysisService,
            ReplyTimeAnalysisService replyTimeAnalysisService,
            InterestScoreAnalysisService interestScoreAnalysisService,
            LanguageSyncAnalysisService languageSyncAnalysisService,
            KeywordExtractionService keywordExtractionService,
            PersonalityPsychologyAnalysisService personalityPsychologyAnalysisService,
            EmotionTimelineAnalysisService emotionTimelineAnalysisService,
            DecisiveMomentAnalysisService decisiveMomentAnalysisService,
            ActionableInsightService actionableInsightService,
            QualitativeSignalsAnalysisService qualitativeSignalsAnalysisService,
            PreferenceAnalysisService preferenceAnalysisService
    ) {
        this.reportStorageService = reportStorageService;
        this.analysisResultRepository = analysisResultRepository;
        this.objectMapper = objectMapper;
        this.talkRatioAnalysisService = talkRatioAnalysisService;
        this.replyTimeAnalysisService = replyTimeAnalysisService;
        this.interestScoreAnalysisService = interestScoreAnalysisService;
        this.languageSyncAnalysisService = languageSyncAnalysisService;
        this.keywordExtractionService = keywordExtractionService;
        this.personalityPsychologyAnalysisService = personalityPsychologyAnalysisService;
        this.emotionTimelineAnalysisService = emotionTimelineAnalysisService;
        this.decisiveMomentAnalysisService = decisiveMomentAnalysisService;
        this.actionableInsightService = actionableInsightService;
        this.qualitativeSignalsAnalysisService = qualitativeSignalsAnalysisService;
        this.preferenceAnalysisService = preferenceAnalysisService;
    }

    public ReportDetailResponse getDetail(Long reportId) {
        ConversationReport report = reportStorageService.getReport(reportId);
        return new ReportDetailResponse(
                report.getId(),
                report.getCategory(),
                report.getSourceType(),
                reportStorageService.readParticipants(report),
                report.getMessageCount(),
                report.getStatus(),
                ReportAnalysisMode.valueOf(report.getAnalysisMode()),
                report.getWarning(),
                report.getCreatedAt()
        );
    }

    public ReportSummaryResponse getSummary(Long reportId) {
        ConversationReport report = reportStorageService.getReport(reportId);
        return getOrCreate(report, AnalysisType.SUMMARY, ReportSummaryResponse.class, () -> buildSummary(report));
    }

    public ReportRelationshipResponse getRelationship(Long reportId) {
        ConversationReport report = reportStorageService.getReport(reportId);
        return getOrCreate(report, AnalysisType.RELATIONSHIP, ReportRelationshipResponse.class, () -> buildRelationship(report));
    }

    public ReportPersonalityResponse getPersonality(Long reportId) {
        ConversationReport report = reportStorageService.getReport(reportId);
        return getOrCreate(report, AnalysisType.PERSONALITY, ReportPersonalityResponse.class, () -> buildPersonality(report));
    }

    public ReportPreferencesResponse getPreferences(Long reportId) {
        ConversationReport report = reportStorageService.getReport(reportId);
        return getOrCreate(report, AnalysisType.PREFERENCES, ReportPreferencesResponse.class, () -> buildPreferences(report));
    }

    public ReportInsightsResponse getInsights(Long reportId) {
        ConversationReport report = reportStorageService.getReport(reportId);
        return getOrCreate(report, AnalysisType.INSIGHTS, ReportInsightsResponse.class, () -> buildInsights(report));
    }

    private ReportSummaryResponse buildSummary(ConversationReport report) {
        NormalizedConversationDto normalized = reportStorageService.readNormalizedConversation(report);
        ReportAnalysisContext context = toContext(report, normalized);
        List<String> contents = contents(normalized);
        List<String> keywords = keywords(context, normalized);
        int interestScore = context.messages().isEmpty()
                ? interestScoreAnalysisService.estimateFromContents(contents)
                : interestScoreAnalysisService.calculate(context);
        ReportResponse.QualitativeSignals qualitativeSignals = qualitativeSignals(context, contents, keywords);

        return new ReportSummaryResponse(
                report.getId(),
                interestScore,
                headline(interestScore, qualitativeSignals.relationshipSummary()),
                keywords,
                qualitativeSignals.relationshipSummary(),
                ReportAnalysisMode.valueOf(report.getAnalysisMode()),
                report.getWarning()
        );
    }

    private ReportRelationshipResponse buildRelationship(ConversationReport report) {
        NormalizedConversationDto normalized = reportStorageService.readNormalizedConversation(report);
        ReportAnalysisContext context = toContext(report, normalized);
        List<String> contents = contents(normalized);
        List<String> keywords = keywords(context, normalized);
        int interestScore = context.messages().isEmpty()
                ? interestScoreAnalysisService.estimateFromContents(contents)
                : interestScoreAnalysisService.calculate(context);
        ReportResponse.TalkRatio talkRatio = context.messages().isEmpty()
                ? new ReportResponse.TalkRatio(0, 0)
                : talkRatioAnalysisService.calculate(context);
        ReplyTimeAnalysisService.ReplyTimeAnalysisResult replyTime = replyTimeAnalysisService.calculate(context);
        int languageSync = context.messages().isEmpty() ? 0 : languageSyncAnalysisService.calculate(context);
        List<ReportResponse.EmotionTimelinePoint> timeline = context.messages().isEmpty()
                ? emotionTimelineAnalysisService.analyzeFlexible(contents)
                : emotionTimelineAnalysisService.analyze(context);

        int timelineAverage = (int) Math.round(
                timeline.stream().mapToInt(ReportResponse.EmotionTimelinePoint::score).average().orElse(50)
        );

        return new ReportRelationshipResponse(
                report.getId(),
                interestScore,
                ReportTextUtils.clamp((interestScore + languageSync) / 2, 0, 100),
                ReportTextUtils.clamp((int) Math.round((interestScore * 0.6) + (timelineAverage * 0.4)), 0, 100),
                activenessScore(context, contents),
                continuityScore(interestScore, replyTime.averageReplyMinutes(), context.messages().size()),
                talkRatio,
                replyTime.averageReplyMinutes(),
                languageSync,
                keywords,
                timeline
        );
    }

    private ReportPersonalityResponse buildPersonality(ConversationReport report) {
        NormalizedConversationDto normalized = reportStorageService.readNormalizedConversation(report);
        ReportAnalysisContext context = toContext(report, normalized);
        List<String> contents = contents(normalized);
        List<String> keywords = keywords(context, normalized);
        ReplyTimeAnalysisService.ReplyTimeAnalysisResult replyTime = replyTimeAnalysisService.calculate(context);
        int languageSync = context.messages().isEmpty() ? 0 : languageSyncAnalysisService.calculate(context);
        ReportResponse.PersonalityPsychology personality = context.messages().isEmpty()
                ? personalityPsychologyAnalysisService.analyzeFlexible(contents)
                : personalityPsychologyAnalysisService.analyze(context, replyTime.averageReplyMinutes(), languageSync);
        ReportResponse.QualitativeSignals qualitativeSignals = qualitativeSignals(context, contents, keywords);

        return new ReportPersonalityResponse(
                report.getId(),
                personality.mbti(),
                personality.attachmentType(),
                personality.bigFive(),
                speechStyle(contents),
                emotionalExpressionStyle(contents),
                qualitativeSignals.counterpartyTendency()
        );
    }

    private ReportPreferencesResponse buildPreferences(ConversationReport report) {
        NormalizedConversationDto normalized = reportStorageService.readNormalizedConversation(report);
        ReportAnalysisContext context = toContext(report, normalized);
        List<String> contents = contents(normalized);
        List<String> keywords = keywords(context, normalized);
        ReportResponse.QualitativeSignals qualitativeSignals = qualitativeSignals(context, contents, keywords);
        return preferenceAnalysisService.analyze(report.getId(), report.getCategory(), keywords, qualitativeSignals);
    }

    private ReportInsightsResponse buildInsights(ConversationReport report) {
        NormalizedConversationDto normalized = reportStorageService.readNormalizedConversation(report);
        ReportAnalysisContext context = toContext(report, normalized);
        List<String> contents = contents(normalized);
        List<String> keywords = keywords(context, normalized);
        int interestScore = context.messages().isEmpty()
                ? interestScoreAnalysisService.estimateFromContents(contents)
                : interestScoreAnalysisService.calculate(context);
        ReportResponse.QualitativeSignals qualitativeSignals = qualitativeSignals(context, contents, keywords);
        ReportResponse.ActionableInsights actionableInsights;
        List<ReportResponse.DecisiveMoment> decisiveMoments;

        if (context.messages().isEmpty()) {
            actionableInsights = actionableInsightService.generateFlexible(
                    report.getCategory(),
                    interestScore,
                    keywords,
                    qualitativeSignals.relationshipSummary()
            );
            decisiveMoments = decisiveMomentAnalysisService.analyzeFlexible(contents);
        } else {
            ReportResponse.TalkRatio talkRatio = talkRatioAnalysisService.calculate(context);
            int averageReplyMinutes = replyTimeAnalysisService.calculate(context).averageReplyMinutes();
            int languageSync = languageSyncAnalysisService.calculate(context);
            actionableInsights = actionableInsightService.generate(
                    context,
                    interestScore,
                    talkRatio,
                    averageReplyMinutes,
                    languageSync,
                    keywords
            );
            decisiveMoments = decisiveMomentAnalysisService.analyze(context);
        }

        return new ReportInsightsResponse(
                report.getId(),
                actionableInsights.tips(),
                actionableInsights.warnings(),
                decisiveMoments,
                actionableInsights.recommendedQuestions(),
                qualitativeSignals.recommendedReplies()
        );
    }

    private <T> T getOrCreate(
            ConversationReport report,
            AnalysisType analysisType,
            Class<T> responseType,
            Supplier<T> supplier
    ) {
        return analysisResultRepository.findByReportIdAndAnalysisType(report.getId(), analysisType.name())
                .map(result -> readResult(result, responseType))
                .orElseGet(() -> {
                    T response = supplier.get();
                    try {
                        String resultJson = objectMapper.writeValueAsString(response);
                        analysisResultRepository.save(new AnalysisResult(report.getId(), analysisType, resultJson));
                        return response;
                    } catch (Exception exception) {
                        throw new ReportGenerationException("Failed to store analysis result.", exception);
                    }
                });
    }

    private <T> T readResult(AnalysisResult result, Class<T> responseType) {
        try {
            return objectMapper.readValue(result.getResultJson(), responseType);
        } catch (Exception exception) {
            throw new ReportGenerationException("Failed to read analysis result.", exception);
        }
    }

    private ReportAnalysisContext toContext(ConversationReport report, NormalizedConversationDto normalized) {
        List<String> participants = normalized.participants() == null ? List.of() : normalized.participants();
        List<ReportMessage> messages = new ArrayList<>();

        List<NormalizedConversationDto.MessageDto> normalizedMessages =
                normalized.messages() == null ? List.of() : normalized.messages();
        for (int index = 0; index < normalizedMessages.size(); index++) {
            NormalizedConversationDto.MessageDto message = normalizedMessages.get(index);
            if (!"TEXT".equals(message.type()) || !StringUtils.hasText(message.content())) {
                continue;
            }
            messages.add(new ReportMessage(
                    StringUtils.hasText(message.sender()) ? message.sender() : fallbackOtherParticipant(participants),
                    parseTimestamp(message.timestamp(), index),
                    message.content()
            ));
        }

        String meParticipant = participants.isEmpty() ? "사용자" : participants.get(0);
        List<String> otherParticipants = participants.size() > 1
                ? participants.subList(1, participants.size())
                : messages.stream()
                .map(ReportMessage::sender)
                .filter(sender -> !sender.equals(meParticipant))
                .distinct()
                .toList();

        return new ReportAnalysisContext(
                report.getCategory(),
                List.copyOf(participants),
                List.copyOf(otherParticipants),
                meParticipant,
                messages.stream().sorted(java.util.Comparator.comparing(ReportMessage::dateTime)).toList(),
                ReportTextUtils.buildAnalysisText(messages)
        );
    }

    private List<String> contents(NormalizedConversationDto normalized) {
        List<NormalizedConversationDto.MessageDto> normalizedMessages =
                normalized.messages() == null ? List.of() : normalized.messages();
        List<String> contents = normalizedMessages.stream()
                .filter(message -> "TEXT".equals(message.type()))
                .map(NormalizedConversationDto.MessageDto::content)
                .filter(ReportTextUtils::hasText)
                .toList();
        if (!contents.isEmpty()) {
            return contents;
        }
        if (!StringUtils.hasText(normalized.rawText())) {
            return List.of();
        }
        return normalized.rawText().lines().filter(ReportTextUtils::hasText).toList();
    }

    private List<String> keywords(ReportAnalysisContext context, NormalizedConversationDto normalized) {
        if (normalized.keywords() != null && !normalized.keywords().isEmpty()) {
            return normalized.keywords();
        }
        if (!context.messages().isEmpty()) {
            return keywordExtractionService.extract(context);
        }
        return keywordExtractionService.extractFromContents(contents(normalized));
    }

    private ReportResponse.QualitativeSignals qualitativeSignals(
            ReportAnalysisContext context,
            List<String> contents,
            List<String> keywords
    ) {
        if (context.messages().isEmpty()) {
            return qualitativeSignalsAnalysisService.analyzeFlexible(contents, keywords);
        }
        return qualitativeSignalsAnalysisService.analyzeStructured(context, keywords);
    }

    private LocalDateTime parseTimestamp(String timestamp, int index) {
        if (StringUtils.hasText(timestamp)) {
            try {
                return LocalDateTime.parse(timestamp);
            } catch (DateTimeParseException ignored) {
            }
        }
        return LocalDateTime.of(1970, 1, 1, 0, 0).plusMinutes(index);
    }

    private String fallbackOtherParticipant(List<String> participants) {
        return participants.size() > 1 ? participants.get(1) : "상대방";
    }

    private String headline(int interestScore, String relationshipSummary) {
        if (interestScore >= 70) {
            return "긍정적인 반응과 대화 지속 신호가 비교적 뚜렷하게 추정됩니다.";
        }
        if (interestScore >= 55) {
            return "대화 흐름은 무난하며 조금 더 자연스럽게 이어갈 여지가 있어 보입니다.";
        }
        if (interestScore >= 40) {
            return "아직은 탐색 단계에 가까워 보여서 속도 조절이 필요해 보입니다.";
        }
        return ReportTextUtils.hasText(relationshipSummary)
                ? relationshipSummary
                : "분석 가능한 신호가 적어 낮은 확신도로 추정했습니다.";
    }

    private int activenessScore(ReportAnalysisContext context, List<String> contents) {
        long questionCount = contents.stream().filter(ReportTextUtils::isQuestion).count();
        long proposalCount = contents.stream().filter(ReportTextUtils::hasProposal).count();
        int messageBonus = Math.min(context.messages().size(), 20) * 3;
        return ReportTextUtils.clamp(25 + messageBonus + (int) questionCount * 5 + (int) proposalCount * 8, 0, 100);
    }

    private int continuityScore(int interestScore, int averageReplyMinutes, int messageCount) {
        int replyPenalty = averageReplyMinutes <= 0 ? 10 : Math.min(35, averageReplyMinutes / 8);
        int volumeBonus = Math.min(messageCount * 2, 20);
        return ReportTextUtils.clamp(interestScore + volumeBonus - replyPenalty, 0, 100);
    }

    private String speechStyle(List<String> contents) {
        double averageLength = contents.stream().mapToInt(content -> ReportTextUtils.safeText(content).length()).average().orElse(0);
        long laughCount = contents.stream().filter(ReportTextUtils::hasLaugh).count();
        if (laughCount > 0 && averageLength <= 30) {
            return "짧고 가벼운 반응을 선호하는 말투로 추정됩니다.";
        }
        if (averageLength >= 45) {
            return "설명형 문장과 맥락 공유를 비교적 많이 쓰는 말투로 추정됩니다.";
        }
        return "무난한 길이의 반응을 주고받는 말투로 추정됩니다.";
    }

    private String emotionalExpressionStyle(List<String> contents) {
        long positiveCount = contents.stream().filter(ReportTextUtils::hasPositiveTone).count();
        long negativeCount = contents.stream().filter(ReportTextUtils::hasNegativeTone).count();
        if (positiveCount > negativeCount) {
            return "긍정 표현과 가벼운 리액션을 통해 감정을 드러내는 편으로 추정됩니다.";
        }
        if (negativeCount > positiveCount) {
            return "불편함이나 피로감을 비교적 직접적으로 드러내는 구간이 있는 것으로 추정됩니다.";
        }
        return "감정 표현은 강하지 않고 상황 중심으로 반응하는 편으로 추정됩니다.";
    }
}
