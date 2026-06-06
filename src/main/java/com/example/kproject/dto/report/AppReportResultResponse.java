package com.example.kproject.dto.report;

import java.util.List;

public record AppReportResultResponse(
        String id,
        String title,
        String date,
        int resultScore,
        int shareMe,
        int sharePartner,
        String replyTime,
        int syncIndex,
        List<String> keywords,
        String mbti,
        String attachment,
        BigFive bigFive,
        String moment,
        String tips,
        String warning,
        String description,
        String analysisSummary,
        List<String> evidence,
        List<String> riskSignals,
        List<String> recommendedQuestions,
        List<String> recommendedReplies
) {
    public record BigFive(
            int openness,
            int conscientiousness,
            int extraversion,
            int agreeableness,
            int neuroticism
    ) {
    }
}
