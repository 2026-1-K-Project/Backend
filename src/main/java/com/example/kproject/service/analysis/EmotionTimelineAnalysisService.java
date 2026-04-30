package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmotionTimelineAnalysisService {

    public List<ReportResponse.EmotionTimelinePoint> analyze(ReportAnalysisContext context) {
        List<ReportMessage> messages = context.messages();
        if (messages.isEmpty()) {
            return List.of();
        }

        int targetPoints = Math.min(8, Math.max(4, messages.size()));
        int chunkSize = Math.max(1, (int) Math.ceil(messages.size() / (double) targetPoints));
        List<ReportResponse.EmotionTimelinePoint> points = new ArrayList<>();
        int runningScore = 50;
        int pointIndex = 1;

        for (int start = 0; start < messages.size(); start += chunkSize) {
            int end = Math.min(messages.size(), start + chunkSize);
            List<ReportMessage> chunk = messages.subList(start, end);

            int chunkScore = scoreChunk(chunk);
            runningScore = ReportTextUtils.clamp(
                    (int) Math.round((runningScore * 0.45) + (chunkScore * 0.55)),
                    0,
                    100
            );
            points.add(new ReportResponse.EmotionTimelinePoint(
                    pointIndex++,
                    runningScore,
                    strongestMessage(chunk)
            ));
        }

        return List.copyOf(points);
    }

    private int scoreChunk(List<ReportMessage> chunk) {
        if (chunk.isEmpty()) {
            return 50;
        }

        double averageSignal = chunk.stream()
                .mapToInt(message -> emotionSignal(message.content()))
                .average()
                .orElse(0);

        return ReportTextUtils.clamp((int) Math.round(50 + averageSignal), 0, 100);
    }

    private int emotionSignal(String content) {
        int signal = 0;
        if (ReportTextUtils.hasPositiveTone(content)) {
            signal += 18;
        }
        if (ReportTextUtils.hasLaugh(content)) {
            signal += 10;
        }
        if (ReportTextUtils.hasProposal(content)) {
            signal += 15;
        }
        if (ReportTextUtils.isQuestion(content)) {
            signal += 6;
        }
        if (ReportTextUtils.hasNegativeTone(content)) {
            signal -= 16;
        }
        return signal;
    }

    private String strongestMessage(List<ReportMessage> chunk) {
        return chunk.stream()
                .map(ReportMessage::content)
                .filter(ReportTextUtils::hasText)
                .max((left, right) -> Integer.compare(
                        Math.abs(emotionSignal(left)),
                        Math.abs(emotionSignal(right))
                ))
                .map(ReportTextUtils::safeText)
                .orElse("");
    }
}
