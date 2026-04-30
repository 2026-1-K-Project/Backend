package com.example.kproject.dto.report;

import java.util.List;

public record ReportResponse(
        Long reportId,
        String category,
        ReportAnalysisMode analysisMode,
        boolean structuredParsingAvailable,
        String warning,
        Summary summary,
        RelationshipDynamics relationshipDynamics,
        PersonalityPsychology personalityPsychology,
        QualitativeSignals qualitativeSignals,
        List<EmotionTimelinePoint> emotionTimeline,
        List<DecisiveMoment> decisiveMoments,
        ActionableInsights actionableInsights
) {
    public ReportResponse withReportId(Long reportId) {
        return new ReportResponse(
                reportId,
                category,
                analysisMode,
                structuredParsingAvailable,
                warning,
                summary,
                relationshipDynamics,
                personalityPsychology,
                qualitativeSignals,
                emotionTimeline,
                decisiveMoments,
                actionableInsights
        );
    }

    public record Summary(
            int interestScore,
            String headline
    ) {
    }

    public record RelationshipDynamics(
            TalkRatio talkRatio,
            int averageReplyMinutes,
            int languageSync,
            List<String> keywords
    ) {
    }

    public record TalkRatio(
            int me,
            int other
    ) {
    }

    public record PersonalityPsychology(
            Estimate mbti,
            Estimate attachmentType,
            BigFive bigFive
    ) {
    }

    public record QualitativeSignals(
            String relationshipSummary,
            String counterpartyTendency,
            List<String> positiveTopics,
            List<String> likelyPreferences,
            List<String> likelyDislikes,
            List<String> recommendedReplies
    ) {
    }

    public record Estimate(
            String type,
            double confidence,
            String description
    ) {
    }

    public record BigFive(
            int openness,
            int conscientiousness,
            int extraversion,
            int agreeableness,
            int neuroticism
    ) {
    }

    public record EmotionTimelinePoint(
            int index,
            int score,
            String message
    ) {
    }

    public record DecisiveMoment(
            String title,
            String dateTime,
            String message,
            String description
    ) {
    }

    public record ActionableInsights(
            List<String> tips,
            List<String> warnings,
            List<String> recommendedQuestions
    ) {
    }
}
