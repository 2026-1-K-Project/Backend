package com.example.kproject.service.report;

import com.example.kproject.domain.AnalysisResult;
import com.example.kproject.domain.AnalysisType;
import com.example.kproject.domain.ConversationReport;
import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.AppReportResultResponse;
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

    public AppReportResultResponse getAppResult(Long reportId) {
        ConversationReport report = reportStorageService.getReport(reportId);
        ReportSummaryResponse summary = getSummary(reportId);
        ReportRelationshipResponse relationship = getRelationship(reportId);
        ReportPersonalityResponse personality = getPersonality(reportId);
        ReportInsightsResponse insights = getInsights(reportId);

        ReportResponse.DecisiveMoment decisiveMoment = firstOrNull(insights.decisiveMoments());
        return new AppReportResultResponse(
                String.valueOf(report.getId()),
                report.getCategory() + " 분석 리포트",
                report.getCreatedAt().toLocalDate().toString(),
                relationship.interestScore(),
                relationship.talkRatio() == null ? 50 : relationship.talkRatio().me(),
                relationship.talkRatio() == null ? 50 : relationship.talkRatio().other(),
                replyTimeText(relationship.averageReplyMinutes()),
                relationship.languageSync(),
                safeList(summary.keywords()).isEmpty() ? safeList(relationship.keywords()) : safeList(summary.keywords()),
                personality.mbti() == null ? "분석중" : personality.mbti().type(),
                personality.attachmentType() == null ? "분석중" : personality.attachmentType().type(),
                toAppBigFive(personality.bigFive()),
                decisiveMoment == null ? summary.headline() : decisiveMoment.description(),
                firstOrDefault(insights.tips(), "상대방의 반응이 좋았던 주제를 자연스럽게 이어가 보세요."),
                firstOrDefault(insights.warnings(), report.getWarning()),
                report.getDescription(),
                analysisSummary(summary, relationship, personality, insights),
                evidence(summary, relationship, insights, report.getMessageCount()),
                riskSignals(relationship, insights, report.getMessageCount()),
                safeList(insights.recommendedQuestions()),
                safeList(insights.recommendedReplies())
        );
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

    private AppReportResultResponse.BigFive toAppBigFive(ReportResponse.BigFive bigFive) {
        if (bigFive == null) {
            return new AppReportResultResponse.BigFive(50, 50, 50, 50, 50);
        }
        return new AppReportResultResponse.BigFive(
                bigFive.openness(),
                bigFive.conscientiousness(),
                bigFive.extraversion(),
                bigFive.agreeableness(),
                bigFive.neuroticism()
        );
    }

    private ReportResponse.DecisiveMoment firstOrNull(List<ReportResponse.DecisiveMoment> moments) {
        return moments == null || moments.isEmpty() ? null : moments.get(0);
    }

    private String firstOrDefault(List<String> values, String fallback) {
        if (values != null) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return StringUtils.hasText(fallback) ? fallback : "";
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(StringUtils::hasText)
                .toList();
    }

    private String analysisSummary(
            ReportSummaryResponse summary,
            ReportRelationshipResponse relationship,
            ReportPersonalityResponse personality,
            ReportInsightsResponse insights
    ) {
        List<String> parts = new ArrayList<>();
        parts.add(StringUtils.hasText(summary.headline())
                ? summary.headline()
                : scoreSummary(relationship.interestScore()));

        if (StringUtils.hasText(personality.counterpartyTendency())) {
            parts.add("상대방 성향: " + personality.counterpartyTendency());
        }

        String firstTip = firstOrDefault(insights.tips(), "");
        if (StringUtils.hasText(firstTip)) {
            parts.add("추천 방향: " + firstTip);
        }

        return String.join(" ", parts);
    }

    private List<String> evidence(
            ReportSummaryResponse summary,
            ReportRelationshipResponse relationship,
            ReportInsightsResponse insights,
            int messageCount
    ) {
        List<String> evidence = new ArrayList<>();
        evidence.add(scoreEvidence(relationship.interestScore()));

        if (relationship.talkRatio() != null) {
            int gap = Math.abs(relationship.talkRatio().me() - relationship.talkRatio().other());
            if (gap <= 20) {
                evidence.add("대화 비율이 크게 한쪽으로 쏠리지 않아 상호작용 균형이 비교적 좋습니다.");
            } else {
                evidence.add("대화 비율 차이가 커서 한쪽이 대화를 더 많이 끌고 가는 흐름이 보입니다.");
            }
        }

        if (relationship.languageSync() >= 70) {
            evidence.add("표현 방식과 대화 리듬이 비슷하게 맞는 구간이 많아 대화 싱크가 높게 잡혔습니다.");
        } else if (relationship.languageSync() <= 40) {
            evidence.add("대화 싱크가 낮아 서로의 말투나 반응 속도가 아직 충분히 맞지 않는 편입니다.");
        }

        if (relationship.averageReplyMinutes() > 0) {
            evidence.add("평균 답장 속도는 " + replyTimeText(relationship.averageReplyMinutes()) + "로 계산됐습니다.");
        }
        if (messageCount > 0) {
            evidence.add("분석에 사용된 메시지는 총 " + messageCount + "개입니다.");
        }

        List<String> keywords = safeList(summary.keywords());
        if (!keywords.isEmpty()) {
            evidence.add("반복적으로 드러난 핵심 키워드: " + String.join(", ", keywords.stream().limit(5).toList()));
        }

        ReportResponse.DecisiveMoment moment = firstOrNull(insights.decisiveMoments());
        if (moment != null && StringUtils.hasText(moment.description())) {
            evidence.add("결정적 장면: " + moment.description());
        }

        return evidence.stream().distinct().limit(7).toList();
    }

    private List<String> riskSignals(
            ReportRelationshipResponse relationship,
            ReportInsightsResponse insights,
            int messageCount
    ) {
        List<String> risks = new ArrayList<>(safeList(insights.warnings()));
        if (relationship.interestScore() < 45) {
            risks.add("관계 지수가 낮아 아직 확신을 전제로 한 표현은 부담이 될 수 있습니다.");
        }
        if (relationship.averageReplyMinutes() >= 180) {
            risks.add("평균 답장 간격이 긴 편이라 빠른 관계 진전보다 자연스러운 템포 조절이 필요합니다.");
        }
        if (relationship.languageSync() <= 35) {
            risks.add("대화 싱크가 낮아 상대의 말투와 관심사에 맞춘 질문이 필요합니다.");
        }
        if (messageCount > 0 && messageCount < 20) {
            risks.add("대화량이 아직 적어 결과를 단정하기보다는 참고 지표로 보는 것이 좋습니다.");
        }
        if (risks.isEmpty()) {
            risks.add("큰 위험 신호는 적지만, 과한 확신 표현보다는 가벼운 제안부터 이어가는 편이 안전합니다.");
        }
        return risks.stream().distinct().limit(5).toList();
    }

    private String scoreSummary(int interestScore) {
        if (interestScore >= 80) {
            return "관심 신호가 뚜렷하고 대화를 이어가려는 흐름이 강하게 나타납니다.";
        }
        if (interestScore >= 65) {
            return "긍정적인 신호가 있지만 확신보다 자연스러운 관계 진전이 더 적합합니다.";
        }
        if (interestScore >= 45) {
            return "대화는 이어지고 있으나 호감 신호는 아직 혼재되어 있습니다.";
        }
        return "현재 대화만으로는 호감 신호가 약하므로 신중한 접근이 필요합니다.";
    }

    private String scoreEvidence(int interestScore) {
        if (interestScore >= 80) {
            return "관계 지수가 " + interestScore + "%로 높아 질문, 반응, 대화 지속성에서 긍정 신호가 우세합니다.";
        }
        if (interestScore >= 65) {
            return "관계 지수가 " + interestScore + "%로 긍정적이지만 아직 확정적인 단계는 아닙니다.";
        }
        if (interestScore >= 45) {
            return "관계 지수가 " + interestScore + "%로 중간 수준이라 긍정/소극 신호가 함께 보입니다.";
        }
        return "관계 지수가 " + interestScore + "%로 낮아 현재 대화에서는 적극적인 관심 신호가 제한적입니다.";
    }

    private String replyTimeText(int averageReplyMinutes) {
        if (averageReplyMinutes <= 0) {
            return "분석중";
        }
        if (averageReplyMinutes < 60) {
            return averageReplyMinutes + "분";
        }
        int hours = averageReplyMinutes / 60;
        int minutes = averageReplyMinutes % 60;
        return minutes == 0 ? hours + "시간" : hours + "시간 " + minutes + "분";
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
