package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LanguageSyncAnalysisService {

    public int calculate(ReportAnalysisContext context) {
        List<String> myContents = context.messages().stream()
                .filter(message -> context.isMe(message.sender()))
                .map(message -> message.content())
                .toList();
        List<String> otherContents = context.messages().stream()
                .filter(message -> context.isOther(message.sender()))
                .map(message -> message.content())
                .toList();

        if (myContents.isEmpty() || otherContents.isEmpty()) {
            return 0;
        }

        Set<String> myTokens = new HashSet<>(ReportTextUtils.distinctTopTokens(myContents, 10));
        Set<String> otherTokens = new HashSet<>(ReportTextUtils.distinctTopTokens(otherContents, 10));
        Set<String> intersection = new HashSet<>(myTokens);
        intersection.retainAll(otherTokens);
        Set<String> union = new HashSet<>(myTokens);
        union.addAll(otherTokens);

        double tokenOverlapScore = union.isEmpty() ? 0 : (intersection.size() * 1.0 / union.size());
        double laughSimilarity = 1.0 - Math.abs(rateOfLaugh(myContents) - rateOfLaugh(otherContents));
        double questionSimilarity = 1.0 - Math.abs(rateOfQuestion(myContents) - rateOfQuestion(otherContents));
        double lengthSimilarity = similarityByAverageLength(myContents, otherContents);

        int score = (int) Math.round(100 * (
                (tokenOverlapScore * 0.4)
                        + (laughSimilarity * 0.2)
                        + (questionSimilarity * 0.2)
                        + (lengthSimilarity * 0.2)
        ));

        return ReportTextUtils.clamp(score, 0, 100);
    }

    private double rateOfLaugh(List<String> contents) {
        return rateBy(contents, ReportTextUtils::hasLaugh);
    }

    private double rateOfQuestion(List<String> contents) {
        return rateBy(contents, ReportTextUtils::isQuestion);
    }

    private double rateBy(List<String> contents, java.util.function.Predicate<String> predicate) {
        if (contents.isEmpty()) {
            return 0;
        }
        long matched = contents.stream()
                .filter(predicate)
                .count();
        return matched * 1.0 / contents.size();
    }

    private double similarityByAverageLength(List<String> myContents, List<String> otherContents) {
        double myAverage = averageLength(myContents);
        double otherAverage = averageLength(otherContents);
        double max = Math.max(Math.max(myAverage, otherAverage), 1);
        return ReportTextUtils.clamp(1.0 - (Math.abs(myAverage - otherAverage) / max), 0, 1);
    }

    private double averageLength(List<String> contents) {
        return contents.stream()
                .mapToInt(content -> ReportTextUtils.safeText(content).length())
                .average()
                .orElse(0);
    }
}
