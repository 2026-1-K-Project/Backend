package com.example.kproject.service.report;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.dto.report.ReportGenerateRequest;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.exception.ReportGenerationException;
import com.example.kproject.service.ai.ReportNarrativeAiService;
import com.example.kproject.service.analysis.DecisiveMomentAnalysisService;
import com.example.kproject.service.analysis.EmotionTimelineAnalysisService;
import com.example.kproject.service.analysis.InterestScoreAnalysisService;
import com.example.kproject.service.analysis.KeywordExtractionService;
import com.example.kproject.service.analysis.LanguageSyncAnalysisService;
import com.example.kproject.service.analysis.PersonalityPsychologyAnalysisService;
import com.example.kproject.service.analysis.QualitativeSignalsAnalysisService;
import com.example.kproject.service.analysis.ReplyTimeAnalysisService;
import com.example.kproject.service.analysis.TalkRatioAnalysisService;
import com.example.kproject.service.insight.ActionableInsightService;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReportGenerationService {

    private final ReportPersistenceService reportPersistenceService;
    private final ReportNarrativeAiService reportNarrativeAiService;
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

    public ReportGenerationService(
            ReportPersistenceService reportPersistenceService,
            ReportNarrativeAiService reportNarrativeAiService,
            TalkRatioAnalysisService talkRatioAnalysisService,
            ReplyTimeAnalysisService replyTimeAnalysisService,
            InterestScoreAnalysisService interestScoreAnalysisService,
            LanguageSyncAnalysisService languageSyncAnalysisService,
            KeywordExtractionService keywordExtractionService,
            PersonalityPsychologyAnalysisService personalityPsychologyAnalysisService,
            EmotionTimelineAnalysisService emotionTimelineAnalysisService,
            DecisiveMomentAnalysisService decisiveMomentAnalysisService,
            ActionableInsightService actionableInsightService,
            QualitativeSignalsAnalysisService qualitativeSignalsAnalysisService
    ) {
        this.reportPersistenceService = reportPersistenceService;
        this.reportNarrativeAiService = reportNarrativeAiService;
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
    }

    public ReportResponse generate(ReportGenerateRequest request) {
        return generate(request, null);
    }

    public ReportResponse generate(ReportGenerateRequest request, String warning) {
        ReportAnalysisContext context = toContext(request);

        ReportResponse.TalkRatio talkRatio = talkRatioAnalysisService.calculate(context);
        ReplyTimeAnalysisService.ReplyTimeAnalysisResult replyTime = replyTimeAnalysisService.calculate(context);
        int interestScore = interestScoreAnalysisService.calculate(context);
        int languageSync = languageSyncAnalysisService.calculate(context);
        List<String> keywords = keywordExtractionService.extract(context);

        ReportResponse.Summary summary = new ReportResponse.Summary(
                interestScore,
                buildHeadline(context, interestScore)
        );
        ReportResponse.RelationshipDynamics relationshipDynamics = new ReportResponse.RelationshipDynamics(
                talkRatio,
                replyTime.averageReplyMinutes(),
                languageSync,
                keywords
        );
        ReportResponse.PersonalityPsychology personalityPsychology = personalityPsychologyAnalysisService.analyze(
                context,
                replyTime.averageReplyMinutes(),
                languageSync
        );
        ReportResponse.QualitativeSignals qualitativeSignals =
                qualitativeSignalsAnalysisService.analyzeStructured(context, keywords);
        List<ReportResponse.EmotionTimelinePoint> emotionTimeline = emotionTimelineAnalysisService.analyze(context);
        List<ReportResponse.DecisiveMoment> decisiveMoments = decisiveMomentAnalysisService.analyze(context);
        ReportResponse.ActionableInsights actionableInsights = actionableInsightService.generate(
                context,
                interestScore,
                talkRatio,
                replyTime.averageReplyMinutes(),
                languageSync,
                keywords
        );

        ReportResponse draftResponse = new ReportResponse(
                null,
                context.category(),
                ReportAnalysisMode.STRUCTURED,
                true,
                warning,
                summary,
                relationshipDynamics,
                personalityPsychology,
                qualitativeSignals,
                emotionTimeline,
                decisiveMoments,
                actionableInsights
        );

        return reportPersistenceService.persist(draftResponse, context.category(), context.participants());
    }

    private ReportAnalysisContext toContext(ReportGenerateRequest request) {
        List<ReportMessage> messages = request.messages().stream()
                .map(message -> new ReportMessage(
                        message.sender(),
                        message.dateTime(),
                        message.content()
                ))
                .sorted(Comparator.comparing(ReportMessage::dateTime))
                .toList();

        if (messages.isEmpty()) {
            throw new ReportGenerationException("At least one message is required.");
        }

        List<String> participants = request.participants() == null ? List.of() : request.participants().stream()
                .filter(StringUtils::hasText)
                .toList();

        String meParticipant = participants.isEmpty() ? messages.get(0).sender() : participants.get(0);
        List<String> otherParticipants = new ArrayList<>();
        if (participants.size() > 1) {
            otherParticipants.addAll(participants.subList(1, participants.size()));
        }
        if (otherParticipants.isEmpty()) {
            otherParticipants.addAll(messages.stream()
                    .map(ReportMessage::sender)
                    .filter(sender -> !sender.equals(meParticipant))
                    .distinct()
                    .toList());
        }

        String analysisText = StringUtils.hasText(request.analysisText())
                ? request.analysisText().trim()
                : ReportTextUtils.buildAnalysisText(messages);

        return new ReportAnalysisContext(
                request.category(),
                List.copyOf(participants),
                List.copyOf(otherParticipants),
                meParticipant,
                List.copyOf(messages),
                analysisText
        );
    }

    private String buildHeadline(ReportAnalysisContext context, int interestScore) {
        String fallbackHeadline;
        if (interestScore >= 75) {
            fallbackHeadline = "서로의 반응이 꽤 잘 맞는 편으로 보여요. 다음 대화도 자연스럽게 이어질 가능성이 있습니다.";
        } else if (interestScore >= 60) {
            fallbackHeadline = "긍정적인 기류가 이어지는 편이에요. 조금만 더 자연스럽게 확장해 보세요.";
        } else if (interestScore >= 45) {
            fallbackHeadline = "관심 신호와 탐색 신호가 함께 보입니다. 속도를 올리기보다 흐름을 다지는 편이 좋겠습니다.";
        } else {
            fallbackHeadline = "아직은 반응 강도가 뚜렷하지 않아 보여요. 부담 없는 주제로 다시 온도를 올려보는 편이 안전합니다.";
        }

        return reportNarrativeAiService.generateHeadline(context, interestScore, fallbackHeadline)
                .orElse(fallbackHeadline);
    }
}
