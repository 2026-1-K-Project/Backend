package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonalityPsychologyAnalysisService {

    public ReportResponse.PersonalityPsychology analyze(
            ReportAnalysisContext context,
            int averageReplyMinutes,
            int languageSync
    ) {
        List<String> targetContents = context.messages().stream()
                .filter(message -> context.isOther(message.sender()))
                .map(message -> message.content())
                .toList();

        if (targetContents.isEmpty()) {
            targetContents = context.messages().stream()
                    .map(message -> message.content())
                    .toList();
        }

        int extraversion = scoreExtraversion(targetContents);
        int agreeableness = scoreAgreeableness(targetContents, languageSync);
        int conscientiousness = scoreConscientiousness(targetContents, averageReplyMinutes);
        int openness = scoreOpenness(targetContents);
        int neuroticism = scoreNeuroticism(targetContents, averageReplyMinutes);

        ReportResponse.BigFive bigFive = new ReportResponse.BigFive(
                openness,
                conscientiousness,
                extraversion,
                agreeableness,
                neuroticism
        );

        ReportResponse.Estimate mbti = buildMbtiEstimate(
                extraversion,
                openness,
                agreeableness,
                conscientiousness,
                false
        );
        ReportResponse.Estimate attachmentType = buildStructuredAttachmentEstimate(
                averageReplyMinutes,
                agreeableness,
                neuroticism,
                extraversion
        );

        return new ReportResponse.PersonalityPsychology(mbti, attachmentType, bigFive);
    }

    public ReportResponse.PersonalityPsychology analyzeFlexible(List<String> contents) {
        List<String> analyzableContents = contents.stream()
                .filter(ReportTextUtils::hasText)
                .toList();

        int extraversion = scoreExtraversion(analyzableContents);
        int agreeableness = scoreAgreeableness(analyzableContents, 45);
        int conscientiousness = scoreConscientiousness(analyzableContents, 90);
        int openness = scoreOpenness(analyzableContents);
        int neuroticism = scoreNeuroticism(analyzableContents, 90);

        ReportResponse.BigFive bigFive = new ReportResponse.BigFive(
                openness,
                conscientiousness,
                extraversion,
                agreeableness,
                neuroticism
        );

        ReportResponse.Estimate mbti = buildMbtiEstimate(
                extraversion,
                openness,
                agreeableness,
                conscientiousness,
                true
        );
        ReportResponse.Estimate attachmentType = buildFlexibleAttachmentEstimate(
                analyzableContents,
                agreeableness,
                neuroticism
        );

        return new ReportResponse.PersonalityPsychology(mbti, attachmentType, bigFive);
    }

    private int scoreExtraversion(List<String> contents) {
        double length = averageLength(contents);
        long laughCount = contents.stream().filter(ReportTextUtils::hasLaugh).count();
        long proposalCount = contents.stream().filter(ReportTextUtils::hasProposal).count();
        long questionCount = contents.stream().filter(ReportTextUtils::isQuestion).count();

        int score = 35
                + (int) Math.round(Math.min(length, 30) * 0.7)
                + (int) Math.round(laughCount * 6)
                + (int) Math.round(proposalCount * 8)
                + (int) Math.round(questionCount * 4);

        return ReportTextUtils.clamp(score, 0, 100);
    }

    private int scoreAgreeableness(List<String> contents, int languageSync) {
        int empathyCount = contents.stream()
                .mapToInt(content -> ReportTextUtils.countTermMatches(content, ReportTextUtils.EMPATHY_TERMS))
                .sum();
        int positiveCount = contents.stream()
                .mapToInt(content -> ReportTextUtils.hasPositiveTone(content) ? 1 : 0)
                .sum();
        int score = 40 + (empathyCount * 8) + (positiveCount * 5) + (int) Math.round(languageSync * 0.2);
        return ReportTextUtils.clamp(score, 0, 100);
    }

    private int scoreConscientiousness(List<String> contents, int averageReplyMinutes) {
        int planningCount = contents.stream()
                .mapToInt(content -> ReportTextUtils.countTermMatches(content, ReportTextUtils.PLANNING_TERMS))
                .sum();
        int responsivenessBonus = averageReplyMinutes <= 15 ? 18 : averageReplyMinutes <= 60 ? 10 : 4;
        int score = 35 + (planningCount * 10) + responsivenessBonus;
        return ReportTextUtils.clamp(score, 0, 100);
    }

    private int scoreOpenness(List<String> contents) {
        int explorationCount = contents.stream()
                .mapToInt(content -> ReportTextUtils.countTermMatches(content, ReportTextUtils.EXPLORATION_TERMS))
                .sum();
        int questionCount = (int) contents.stream().filter(ReportTextUtils::isQuestion).count();
        int score = 38 + (explorationCount * 11) + (questionCount * 4);
        return ReportTextUtils.clamp(score, 0, 100);
    }

    private int scoreNeuroticism(List<String> contents, int averageReplyMinutes) {
        int negativeCount = contents.stream()
                .mapToInt(content -> ReportTextUtils.countTermMatches(content, ReportTextUtils.NEGATIVE_TERMS))
                .sum();
        long questionCount = contents.stream().filter(ReportTextUtils::isQuestion).count();
        int score = 28 + (negativeCount * 10) + (int) Math.round(questionCount * 2) + (averageReplyMinutes > 180 ? 6 : 0);
        return ReportTextUtils.clamp(score, 0, 100);
    }

    private ReportResponse.Estimate buildMbtiEstimate(
            int extraversion,
            int openness,
            int agreeableness,
            int conscientiousness,
            boolean lowConfidenceMode
    ) {
        char ei = extraversion >= 55 ? 'E' : 'I';
        char sn = openness >= 55 ? 'N' : 'S';
        char tf = agreeableness >= 55 ? 'F' : 'T';
        char jp = conscientiousness >= 55 ? 'J' : 'P';
        String type = "" + ei + sn + tf + jp;

        double confidence = confidenceFromThresholds(extraversion, openness, agreeableness, conscientiousness);
        if (lowConfidenceMode) {
            confidence = ReportTextUtils.clamp(confidence - 0.18, 0.34, 0.62);
        }

        String prefix = lowConfidenceMode
                ? "전체 텍스트 흐름만 기준으로 "
                : "대화 반응 패턴상 ";

        String description = switch (type) {
            case "ENFP" -> prefix + "활발하고 감정 표현이 자연스러운 경향이 추정됩니다.";
            case "ENFJ" -> prefix + "관계를 챙기고 흐름을 주도하려는 경향이 추정됩니다.";
            case "INFP" -> prefix + "감정의 결을 섬세하게 살피는 경향이 추정됩니다.";
            case "INFJ" -> prefix + "의미와 분위기를 함께 읽으려는 경향이 추정됩니다.";
            case "ESFJ" -> prefix + "반응이 빠르고 상대 배려를 중시하는 경향이 추정됩니다.";
            case "ISTJ" -> prefix + "정리와 확인을 중시하는 경향이 추정됩니다.";
            default -> prefix + type + " 경향이 추정됩니다.";
        };

        return new ReportResponse.Estimate(type, confidence, description);
    }

    private ReportResponse.Estimate buildStructuredAttachmentEstimate(
            int averageReplyMinutes,
            int agreeableness,
            int neuroticism,
            int extraversion
    ) {
        String type;
        String description;

        if (agreeableness >= 65 && neuroticism <= 50 && averageReplyMinutes <= 60) {
            type = "안정형 애착";
            description = "대화에서 비교적 편안하고 안정적인 반응 패턴이 보이는 것으로 추정됩니다.";
        } else if (neuroticism >= 60 && extraversion >= 55) {
            type = "불안형 애착";
            description = "확인과 반응 신호에 민감한 경향이 일부 추정됩니다.";
        } else if (averageReplyMinutes >= 180 && extraversion <= 50) {
            type = "회피형 애착";
            description = "거리 조절과 반응 속도에서 신중한 패턴이 추정됩니다.";
        } else {
            type = "혼합형 애착";
            description = "안정 신호와 조심스러운 신호가 함께 보이는 것으로 추정됩니다.";
        }

        double confidence = ReportTextUtils.clamp(
                0.52 + Math.abs(agreeableness - neuroticism) / 200.0,
                0.52,
                0.82
        );

        return new ReportResponse.Estimate(type, confidence, description);
    }

    private ReportResponse.Estimate buildFlexibleAttachmentEstimate(
            List<String> contents,
            int agreeableness,
            int neuroticism
    ) {
        long reassuringCount = contents.stream()
                .filter(content -> ReportTextUtils.containsAny(content, ReportTextUtils.EMPATHY_TERMS))
                .count();
        long negativeCount = contents.stream()
                .filter(ReportTextUtils::hasNegativeTone)
                .count();

        String type;
        if (reassuringCount >= negativeCount + 2 && agreeableness >= 60) {
            type = "안정형 애착";
        } else if (negativeCount >= 2 && neuroticism >= 58) {
            type = "불안형 애착";
        } else {
            type = "혼합형 애착";
        }

        String description = "메시지 구조가 제한적이어서 전체 텍스트 흐름만 기준으로 애착 경향을 추정했습니다.";
        double confidence = ReportTextUtils.clamp(0.36 + Math.abs(agreeableness - neuroticism) / 250.0, 0.36, 0.58);

        return new ReportResponse.Estimate(type, confidence, description);
    }

    private double averageLength(List<String> contents) {
        return contents.stream()
                .mapToInt(content -> ReportTextUtils.safeText(content).length())
                .average()
                .orElse(0);
    }

    private double confidenceFromThresholds(int... values) {
        double averageDistance = 0;
        for (int value : values) {
            averageDistance += Math.abs(value - 50);
        }
        averageDistance /= values.length;
        return ReportTextUtils.clamp(0.5 + (averageDistance / 100.0), 0.5, 0.85);
    }
}
