package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QualitativeSignalsAnalysisService {

    private final KeywordExtractionService keywordExtractionService;

    public QualitativeSignalsAnalysisService(KeywordExtractionService keywordExtractionService) {
        this.keywordExtractionService = keywordExtractionService;
    }

    public ReportResponse.QualitativeSignals analyzeStructured(ReportAnalysisContext context, List<String> keywords) {
        List<String> allContents = context.messages().stream()
                .map(message -> message.content())
                .filter(ReportTextUtils::hasText)
                .toList();

        List<String> targetContents = context.messages().stream()
                .filter(message -> context.isOther(message.sender()))
                .map(message -> message.content())
                .filter(ReportTextUtils::hasText)
                .toList();

        if (targetContents.isEmpty()) {
            targetContents = allContents;
        }

        return buildQualitativeSignals(allContents, targetContents, keywords, false);
    }

    public ReportResponse.QualitativeSignals analyzeFlexible(List<String> contents, List<String> keywords) {
        List<String> analyzableContents = contents.stream()
                .filter(ReportTextUtils::hasText)
                .toList();
        return buildQualitativeSignals(analyzableContents, analyzableContents, keywords, true);
    }

    private ReportResponse.QualitativeSignals buildQualitativeSignals(
            List<String> allContents,
            List<String> targetContents,
            List<String> keywords,
            boolean limitedStructure
    ) {
        String relationshipSummary = summarizeRelationship(allContents, limitedStructure);
        String counterpartyTendency = summarizeCounterpartyTendency(targetContents, limitedStructure);
        List<String> positiveTopics = extractPositiveTopics(allContents, keywords);
        List<String> likelyPreferences = extractLikelyPreferences(targetContents, keywords);
        List<String> likelyDislikes = extractLikelyDislikes(targetContents);
        List<String> recommendedReplies = buildRecommendedReplies(keywords, limitedStructure);

        return new ReportResponse.QualitativeSignals(
                relationshipSummary,
                counterpartyTendency,
                positiveTopics,
                likelyPreferences,
                likelyDislikes,
                recommendedReplies
        );
    }

    private String summarizeRelationship(List<String> contents, boolean limitedStructure) {
        long positiveCount = contents.stream().filter(ReportTextUtils::hasPositiveTone).count();
        long negativeCount = contents.stream().filter(ReportTextUtils::hasNegativeTone).count();
        long proposalCount = contents.stream().filter(ReportTextUtils::hasProposal).count();
        long questionCount = contents.stream().filter(ReportTextUtils::isQuestion).count();

        String prefix = limitedStructure
                ? "전체 텍스트 흐름만 보면 "
                : "대화 흐름상 ";

        if (positiveCount >= negativeCount + 2 && proposalCount > 0) {
            return prefix + "긍정적이고 다음 대화로 이어질 여지가 있어 보입니다.";
        }
        if (positiveCount > negativeCount) {
            return prefix + "무난하게 이어지는 편안한 분위기가 우세한 것으로 보입니다.";
        }
        if (negativeCount > positiveCount) {
            return prefix + "피로감이나 조심스러운 기류가 섞여 있어 속도 조절이 필요해 보입니다.";
        }
        if (questionCount > 0) {
            return prefix + "탐색과 관망이 섞여 있는 단계로 추정됩니다.";
        }
        return prefix + "반응 강도는 크지 않지만 대화 여지는 남아 있는 편으로 보입니다.";
    }

    private String summarizeCounterpartyTendency(List<String> contents, boolean limitedStructure) {
        int empathyCount = contents.stream()
                .mapToInt(content -> ReportTextUtils.countTermMatches(content, ReportTextUtils.EMPATHY_TERMS))
                .sum();
        int planningCount = contents.stream()
                .mapToInt(content -> ReportTextUtils.countTermMatches(content, ReportTextUtils.PLANNING_TERMS))
                .sum();
        int explorationCount = contents.stream()
                .mapToInt(content -> ReportTextUtils.countTermMatches(content, ReportTextUtils.EXPLORATION_TERMS))
                .sum();
        long laughCount = contents.stream().filter(ReportTextUtils::hasLaugh).count();

        String prefix = limitedStructure
                ? "텍스트만 기준으로 보면 "
                : "상대는 ";

        if (empathyCount >= planningCount && empathyCount >= explorationCount && empathyCount >= laughCount) {
            return prefix + "공감 반응이 먼저 나오는 성향으로 추정됩니다.";
        }
        if (planningCount >= empathyCount && planningCount >= explorationCount) {
            return prefix + "즉흥성보다 일정과 맥락을 정리하는 쪽에 가까워 보입니다.";
        }
        if (explorationCount >= empathyCount && explorationCount >= planningCount) {
            return prefix + "새로운 경험이나 흥미 소재에 반응하는 편으로 추정됩니다.";
        }
        if (laughCount > 0) {
            return prefix + "가벼운 농담과 편한 분위기에서 반응이 살아나는 편으로 보입니다.";
        }
        return prefix + "관심 있는 지점에서만 반응이 조금 더 길어지는 경향이 있어 보입니다.";
    }

    private List<String> extractPositiveTopics(List<String> contents, List<String> fallbackKeywords) {
        List<String> positiveLines = contents.stream()
                .filter(content -> ReportTextUtils.hasPositiveTone(content) || ReportTextUtils.hasProposal(content))
                .toList();

        List<String> positiveKeywords = keywordExtractionService.extractFromContents(positiveLines);
        if (!positiveKeywords.isEmpty()) {
            return positiveKeywords.stream().limit(3).toList();
        }
        return fallbackKeywords.stream().limit(3).toList();
    }

    private List<String> extractLikelyPreferences(List<String> contents, List<String> fallbackKeywords) {
        List<String> preferenceLines = contents.stream()
                .filter(content -> ReportTextUtils.hasPositiveTone(content) || ReportTextUtils.hasProposal(content))
                .toList();

        List<String> preferences = keywordExtractionService.extractFromContents(preferenceLines);
        if (!preferences.isEmpty()) {
            return preferences.stream().limit(3).toList();
        }
        return fallbackKeywords.stream().limit(3).toList();
    }

    private List<String> extractLikelyDislikes(List<String> contents) {
        List<String> negativeLines = contents.stream()
                .filter(ReportTextUtils::hasNegativeTone)
                .toList();

        List<String> dislikes = keywordExtractionService.extractFromContents(negativeLines);
        return dislikes.stream().limit(3).toList();
    }

    private List<String> buildRecommendedReplies(List<String> keywords, boolean limitedStructure) {
        String keyword = keywords.isEmpty() ? "그 얘기" : keywords.get(0);
        String prefix = limitedStructure ? "텍스트 흐름상 " : "";

        return List.of(
                prefix + keyword + " 얘기 더 듣고 싶어. 조금만 더 들려줄래?",
                prefix + "그 포인트 재밌다. 네 생각을 조금 더 자세히 듣고 싶어.",
                prefix + "좋다. 부담 없을 때 이어서 얘기해 보자."
        );
    }
}
