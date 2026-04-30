package com.example.kproject.service.report;

import com.example.kproject.domain.KakaoChatParsedDocument;
import com.example.kproject.dto.KakaoChatMessageDto;
import com.example.kproject.dto.KakaoChatSpecialType;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.service.analysis.DecisiveMomentAnalysisService;
import com.example.kproject.service.analysis.EmotionTimelineAnalysisService;
import com.example.kproject.service.analysis.InterestScoreAnalysisService;
import com.example.kproject.service.analysis.KeywordExtractionService;
import com.example.kproject.service.analysis.PersonalityPsychologyAnalysisService;
import com.example.kproject.service.analysis.QualitativeSignalsAnalysisService;
import com.example.kproject.service.insight.ActionableInsightService;
import com.example.kproject.util.KakaoChatParsingUtils;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class FlexibleTextReportService {

    private static final String DEFAULT_COUNTERPART_NAME = "상대방";

    private final ReportPersistenceService reportPersistenceService;
    private final KeywordExtractionService keywordExtractionService;
    private final InterestScoreAnalysisService interestScoreAnalysisService;
    private final PersonalityPsychologyAnalysisService personalityPsychologyAnalysisService;
    private final EmotionTimelineAnalysisService emotionTimelineAnalysisService;
    private final DecisiveMomentAnalysisService decisiveMomentAnalysisService;
    private final ActionableInsightService actionableInsightService;
    private final QualitativeSignalsAnalysisService qualitativeSignalsAnalysisService;

    public FlexibleTextReportService(
            ReportPersistenceService reportPersistenceService,
            KeywordExtractionService keywordExtractionService,
            InterestScoreAnalysisService interestScoreAnalysisService,
            PersonalityPsychologyAnalysisService personalityPsychologyAnalysisService,
            EmotionTimelineAnalysisService emotionTimelineAnalysisService,
            DecisiveMomentAnalysisService decisiveMomentAnalysisService,
            ActionableInsightService actionableInsightService,
            QualitativeSignalsAnalysisService qualitativeSignalsAnalysisService
    ) {
        this.reportPersistenceService = reportPersistenceService;
        this.keywordExtractionService = keywordExtractionService;
        this.interestScoreAnalysisService = interestScoreAnalysisService;
        this.personalityPsychologyAnalysisService = personalityPsychologyAnalysisService;
        this.emotionTimelineAnalysisService = emotionTimelineAnalysisService;
        this.decisiveMomentAnalysisService = decisiveMomentAnalysisService;
        this.actionableInsightService = actionableInsightService;
        this.qualitativeSignalsAnalysisService = qualitativeSignalsAnalysisService;
    }

    public ReportResponse generate(
            String category,
            String targetName,
            KakaoChatParsedDocument parsedDocument,
            String warning
    ) {
        List<String> contents = extractFlexibleContents(parsedDocument);
        List<String> keywords = keywordExtractionService.extractFromContents(contents);
        int interestScore = interestScoreAnalysisService.estimateFromContents(contents);
        ReportResponse.QualitativeSignals qualitativeSignals =
                qualitativeSignalsAnalysisService.analyzeFlexible(contents, keywords);

        ReportResponse.Summary summary = new ReportResponse.Summary(
                interestScore,
                buildHeadline(interestScore, qualitativeSignals.relationshipSummary())
        );
        ReportResponse.RelationshipDynamics relationshipDynamics = new ReportResponse.RelationshipDynamics(
                new ReportResponse.TalkRatio(0, 0),
                0,
                0,
                keywords
        );
        ReportResponse.PersonalityPsychology personalityPsychology =
                personalityPsychologyAnalysisService.analyzeFlexible(contents);
        List<ReportResponse.EmotionTimelinePoint> emotionTimeline =
                emotionTimelineAnalysisService.analyzeFlexible(contents);
        List<ReportResponse.DecisiveMoment> decisiveMoments =
                decisiveMomentAnalysisService.analyzeFlexible(contents);
        ReportResponse.ActionableInsights actionableInsights =
                actionableInsightService.generateFlexible(category, interestScore, keywords, qualitativeSignals.relationshipSummary());

        ReportResponse draftResponse = new ReportResponse(
                null,
                category,
                ReportAnalysisMode.FLEXIBLE,
                false,
                warning,
                summary,
                relationshipDynamics,
                personalityPsychology,
                qualitativeSignals,
                emotionTimeline,
                decisiveMoments,
                actionableInsights
        );

        return reportPersistenceService.persist(
                draftResponse,
                category,
                resolveParticipants(parsedDocument, targetName)
        );
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

    private String buildHeadline(int interestScore, String relationshipSummary) {
        if (interestScore >= 70) {
            return "텍스트 전반의 기류는 비교적 밝아 보여요. 흐름을 자연스럽게 이어가면 좋겠습니다.";
        }
        if (interestScore >= 55) {
            return "반응 신호는 나쁘지 않아 보여요. 가벼운 연결 질문이 잘 맞을 가능성이 있습니다.";
        }
        if (interestScore >= 40) {
            return "아직은 확정적 신호보다 탐색 단계에 가까워 보여요. 무리하지 않는 편이 좋겠습니다.";
        }
        if (ReportTextUtils.hasText(relationshipSummary)) {
            return relationshipSummary;
        }
        return "형식이 불완전해 자유 분석으로 전환했어요. 텍스트 흐름 기준으로 핵심만 추정했습니다.";
    }

    private List<String> resolveParticipants(KakaoChatParsedDocument parsedDocument, String targetName) {
        String resolvedTargetName = StringUtils.hasText(targetName)
                ? targetName.trim()
                : StringUtils.hasText(parsedDocument.meta().roomName()) ? parsedDocument.meta().roomName() : DEFAULT_COUNTERPART_NAME;

        return List.of("나", resolvedTargetName);
    }
}
