package com.example.kproject.service.analysis;

import com.example.kproject.dto.report.ReportPreferencesResponse;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class PreferenceAnalysisService {

    public ReportPreferencesResponse analyze(
            Long reportId,
            String category,
            List<String> keywords,
            ReportResponse.QualitativeSignals qualitativeSignals
    ) {
        LinkedHashSet<String> favoriteTopics = new LinkedHashSet<>();
        if (qualitativeSignals != null) {
            favoriteTopics.addAll(qualitativeSignals.positiveTopics());
            favoriteTopics.addAll(qualitativeSignals.likelyPreferences());
        }
        favoriteTopics.addAll(keywords == null ? List.of() : keywords);

        return new ReportPreferencesResponse(
                reportId,
                likedPhrases(category),
                likedBehaviors(category),
                favoriteTopics.stream().filter(ReportTextUtils::hasText).limit(5).toList(),
                dislikedExpressions(qualitativeSignals),
                burdensomeExpressions(category)
        );
    }

    private List<String> likedPhrases(String category) {
        List<String> phrases = new ArrayList<>();
        phrases.add("고생했어");
        phrases.add("그 얘기 더 듣고 싶어");
        phrases.add("괜찮아, 천천히 해도 돼");

        switch (ReportTextUtils.normalizeCategory(category)) {
            case "romance" -> phrases.add("다음에 같이 해보자");
            case "work" -> phrases.add("제가 정리해서 다시 확인드릴게요");
            case "team" -> phrases.add("역할 나눠서 같이 맞춰보자");
            default -> phrases.add("편할 때 이어서 얘기하자");
        }

        return phrases.stream().distinct().limit(5).toList();
    }

    private List<String> likedBehaviors(String category) {
        List<String> behaviors = new ArrayList<>();
        behaviors.add("상대 일정과 컨디션을 먼저 확인하기");
        behaviors.add("상대가 꺼낸 주제를 다시 이어가기");
        behaviors.add("답장 길이와 말투를 비슷하게 맞추기");

        switch (ReportTextUtils.normalizeCategory(category)) {
            case "romance" -> behaviors.add("부담 없는 작은 제안 던지기");
            case "work" -> behaviors.add("요청 사항을 짧고 명확하게 정리하기");
            case "team" -> behaviors.add("일정과 역할을 선택지로 제안하기");
            default -> behaviors.add("가벼운 공감 뒤에 질문 하나만 붙이기");
        }

        return behaviors.stream().distinct().limit(5).toList();
    }

    private List<String> dislikedExpressions(ReportResponse.QualitativeSignals qualitativeSignals) {
        LinkedHashSet<String> expressions = new LinkedHashSet<>();
        if (qualitativeSignals != null) {
            expressions.addAll(qualitativeSignals.likelyDislikes());
        }
        expressions.add("단답");
        expressions.add("무관심한 반응");
        expressions.add("맥락 없는 주제 전환");
        return expressions.stream().filter(ReportTextUtils::hasText).limit(5).toList();
    }

    private List<String> burdensomeExpressions(String category) {
        List<String> expressions = new ArrayList<>();
        expressions.add("왜 답장 안 해?");
        expressions.add("지금 바로 정해줘");
        expressions.add("너는 항상 그래");

        switch (ReportTextUtils.normalizeCategory(category)) {
            case "romance" -> expressions.add("우리 무슨 사이야?");
            case "work" -> expressions.add("대충 알아서 해주세요");
            case "team" -> expressions.add("난 잘 모르겠으니까 맡아줘");
            default -> expressions.add("읽었으면 답해");
        }

        return expressions.stream().distinct().limit(5).toList();
    }
}
