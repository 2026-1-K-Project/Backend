package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterestScoreAnalysisService {

    private final ReplyTimeAnalysisService replyTimeAnalysisService;

    public InterestScoreAnalysisService(ReplyTimeAnalysisService replyTimeAnalysisService) {
        this.replyTimeAnalysisService = replyTimeAnalysisService;
    }

    public int calculate(ReportAnalysisContext context) {
        List<ReportMessage> targetMessages = context.messages().stream()
                .filter(message -> context.isOther(message.sender()))
                .toList();

        if (targetMessages.isEmpty()) {
            targetMessages = context.messages();
        }

        if (targetMessages.isEmpty()) {
            return 0;
        }

        int questionCount = (int) targetMessages.stream()
                .filter(message -> ReportTextUtils.isQuestion(message.content()))
                .count();
        int positiveCount = (int) targetMessages.stream()
                .filter(message -> ReportTextUtils.hasPositiveTone(message.content()))
                .count();
        int laughCount = (int) targetMessages.stream()
                .filter(message -> ReportTextUtils.hasLaugh(message.content()))
                .count();
        int proposalCount = (int) targetMessages.stream()
                .filter(message -> ReportTextUtils.hasProposal(message.content()))
                .count();

        double questionScore = ratio(questionCount, targetMessages.size());
        double positiveScore = ratio(positiveCount, targetMessages.size());
        double laughScore = ratio(laughCount, targetMessages.size());
        double proposalScore = ratio(proposalCount, targetMessages.size());

        ReplyTimeAnalysisService.ReplyTimeAnalysisResult replyTimeResult = replyTimeAnalysisService.calculate(context);
        double replySpeedScore = replySpeedScore(replyTimeResult.otherReplyMinutes());
        double replyLengthScore = replyLengthScore(targetMessages);
        double mutualityScore = mutualityScore(context, targetMessages);

        int score = ReportTextUtils.INTEREST_BASE_SCORE
                + weighted(questionScore, ReportTextUtils.INTEREST_QUESTION_WEIGHT)
                + weighted(positiveScore, ReportTextUtils.INTEREST_POSITIVE_WEIGHT)
                + weighted(laughScore, ReportTextUtils.INTEREST_LAUGH_WEIGHT)
                + weighted(proposalScore, ReportTextUtils.INTEREST_PROPOSAL_WEIGHT)
                + weighted(replySpeedScore, ReportTextUtils.INTEREST_REPLY_SPEED_WEIGHT)
                + weighted(replyLengthScore, ReportTextUtils.INTEREST_REPLY_LENGTH_WEIGHT)
                + weighted(mutualityScore, ReportTextUtils.INTEREST_MUTUALITY_WEIGHT);

        return ReportTextUtils.clamp(score, 0, 100);
    }

    public int estimateFromContents(List<String> contents) {
        List<String> analyzableContents = contents.stream()
                .filter(ReportTextUtils::hasText)
                .toList();
        if (analyzableContents.isEmpty()) {
            return 0;
        }

        int questionCount = (int) analyzableContents.stream().filter(ReportTextUtils::isQuestion).count();
        int positiveCount = (int) analyzableContents.stream().filter(ReportTextUtils::hasPositiveTone).count();
        int laughCount = (int) analyzableContents.stream().filter(ReportTextUtils::hasLaugh).count();
        int proposalCount = (int) analyzableContents.stream().filter(ReportTextUtils::hasProposal).count();

        double questionScore = ratio(questionCount, analyzableContents.size());
        double positiveScore = ratio(positiveCount, analyzableContents.size());
        double laughScore = ratio(laughCount, analyzableContents.size());
        double proposalScore = ratio(proposalCount, analyzableContents.size());
        double lengthScore = ReportTextUtils.clamp(
                analyzableContents.stream().mapToInt(content -> ReportTextUtils.safeText(content).length()).average().orElse(0) / 35.0,
                0,
                1
        );
        double densityBonus = ReportTextUtils.clamp(analyzableContents.size() / 12.0, 0, 1);

        int score = ReportTextUtils.INTEREST_BASE_SCORE
                + weighted(questionScore, 12)
                + weighted(positiveScore, 20)
                + weighted(laughScore, 10)
                + weighted(proposalScore, 12)
                + weighted(lengthScore, 6)
                + weighted(densityBonus, 8);

        return ReportTextUtils.clamp(score, 0, 100);
    }

    private double ratio(int count, int total) {
        if (total <= 0) {
            return 0;
        }
        return count * 1.0 / total;
    }

    private int weighted(double score, int weight) {
        return (int) Math.round(score * weight);
    }

    private double replySpeedScore(int otherReplyMinutes) {
        if (otherReplyMinutes <= 0) {
            return 0.4;
        }
        if (otherReplyMinutes <= 5) {
            return 1.0;
        }
        if (otherReplyMinutes <= 15) {
            return 0.8;
        }
        if (otherReplyMinutes <= 60) {
            return 0.6;
        }
        if (otherReplyMinutes <= 180) {
            return 0.4;
        }
        return 0.2;
    }

    private double replyLengthScore(List<ReportMessage> messages) {
        double averageLength = messages.stream()
                .mapToInt(message -> ReportTextUtils.safeText(message.content()).length())
                .average()
                .orElse(0);
        return ReportTextUtils.clamp(averageLength / 30.0, 0, 1);
    }

    private double mutualityScore(ReportAnalysisContext context, List<ReportMessage> targetMessages) {
        long meCount = context.messages().stream()
                .filter(message -> context.isMe(message.sender()))
                .count();
        if (meCount <= 0) {
            return 0;
        }
        return ReportTextUtils.clamp(targetMessages.size() * 1.0 / meCount, 0, 1);
    }
}
