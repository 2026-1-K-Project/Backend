package com.example.kproject.service.insight;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.service.ai.ReportNarrativeAiService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ActionableInsightService {

    private final ReportNarrativeAiService reportNarrativeAiService;

    public ActionableInsightService(ReportNarrativeAiService reportNarrativeAiService) {
        this.reportNarrativeAiService = reportNarrativeAiService;
    }

    public ReportResponse.ActionableInsights generate(
            ReportAnalysisContext context,
            int interestScore,
            ReportResponse.TalkRatio talkRatio,
            int averageReplyMinutes,
            int languageSync,
            List<String> keywords
    ) {
        List<String> tips = buildTips(context.category(), interestScore, languageSync, keywords);
        List<String> warnings = buildWarnings(context.category(), talkRatio, averageReplyMinutes, interestScore);
        List<String> questions = buildRecommendedQuestions(context.category(), keywords);

        questions = reportNarrativeAiService.generateRecommendedQuestions(context, keywords, questions)
                .orElse(questions);

        return new ReportResponse.ActionableInsights(
                trimTo(tips, 3),
                trimTo(warnings, 3),
                trimTo(questions, 5)
        );
    }

    private List<String> buildTips(String category, int interestScore, int languageSync, List<String> keywords) {
        List<String> tips = new ArrayList<>();

        if (interestScore >= 65) {
            tips.add("지금의 편한 흐름을 유지하면서 상대가 먼저 꺼낸 주제를 한 단계 더 확장해 보세요.");
        } else {
            tips.add("질문을 한 번에 여러 개 던지기보다 한 가지 주제를 길게 이어가는 편이 안정적입니다.");
        }

        if (languageSync >= 70) {
            tips.add("현재 말투 호흡이 잘 맞는 편이어서, 비슷한 길이와 톤을 유지하는 것이 유리합니다.");
        } else {
            tips.add("상대의 답장 길이에 맞춰 템포를 조정하면 어색함이 덜해질 수 있습니다.");
        }

        String focusKeyword = keywords.isEmpty() ? null : keywords.get(0);
        if (focusKeyword != null) {
            tips.add("지금 반응이 괜찮았던 '" + focusKeyword + "' 주제를 조금 더 구체적으로 파보는 것이 좋겠습니다.");
        }

        switch (normalizeCategory(category)) {
            case "romance" -> tips.add("감정 확인보다는 가벼운 공감과 작은 제안으로 자연스럽게 다음 대화를 여세요.");
            case "friend" -> tips.add("편한 일상 소재에 경험 질문을 섞으면 친구 톤을 유지하면서 대화 폭이 넓어집니다.");
            case "work" -> tips.add("업무 맥락에서는 배경 설명보다 확인 질문을 짧고 명확하게 주는 편이 좋습니다.");
            case "team" -> tips.add("역할과 일정 이야기를 할 때는 선택지를 함께 제시하면 답장이 쉬워집니다.");
            default -> tips.add("상대가 이미 반응한 포인트를 다시 활용하면 대화 성공 확률이 높아질 수 있습니다.");
        }

        return tips;
    }

    private List<String> buildWarnings(String category, ReportResponse.TalkRatio talkRatio, int averageReplyMinutes, int interestScore) {
        List<String> warnings = new ArrayList<>();

        if (talkRatio.me() >= 65) {
            warnings.add("내 쪽 메시지 비중이 높은 편이라 설명이 길어지면 상대가 수동적으로 변할 수 있습니다.");
        }
        if (averageReplyMinutes >= 120) {
            warnings.add("답장 간격이 긴 편이므로 연속 확인 메시지는 부담으로 읽힐 수 있습니다.");
        }
        if (interestScore < 50) {
            warnings.add("반응을 확정적으로 해석하기보다 가벼운 탐색 단계로 보는 편이 안전합니다.");
        }

        switch (normalizeCategory(category)) {
            case "romance" -> warnings.add("감정 압박이나 관계 정의를 서두르면 오히려 템포가 꺾일 수 있습니다.");
            case "work" -> warnings.add("직장/상사 관계에서는 과한 사적 표현이 메시지 의도를 흐릴 수 있습니다.");
            case "team" -> warnings.add("팀플 맥락에서는 감정 표현보다 일정과 역할 확인이 먼저여야 합니다.");
            default -> warnings.add("단답이 이어지는 구간에서는 주제를 급격히 바꾸기보다 맥락을 연결하는 편이 좋습니다.");
        }

        return warnings;
    }

    private List<String> buildRecommendedQuestions(String category, List<String> keywords) {
        String keyword = keywords.isEmpty() ? "요즘 재미있는 일" : keywords.get(0);
        LinkedHashSet<String> questions = new LinkedHashSet<>();

        switch (normalizeCategory(category)) {
            case "romance" -> {
                questions.add(keyword + " 얘기 더 해줄래?");
                questions.add("요즘 제일 가보고 싶은 곳 있어?");
                questions.add("다음에 시간 맞으면 같이 해보고 싶은 거 있어?");
                questions.add("최근에 제일 웃겼던 일 뭐였어?");
            }
            case "friend" -> {
                questions.add("요즘 " + keyword + " 관련해서 재밌었던 거 있어?");
                questions.add("최근에 푹 빠진 거 하나만 꼽으면 뭐야?");
                questions.add("다음에 같이 가볍게 해볼 만한 거 있을까?");
            }
            case "work" -> {
                questions.add(keyword + " 쪽은 어떤 방향으로 정리하면 제일 편하실까요?");
                questions.add("우선순위만 한 번 더 확인해도 될까요?");
                questions.add("다음 단계 진행 전에 제가 준비해둘 것 있을까요?");
            }
            case "team" -> {
                questions.add(keyword + " 관련해서 역할을 어떻게 나누면 좋을까?");
                questions.add("이번 주 안에 먼저 맞춰야 할 일정 있을까?");
                questions.add("각자 맡을 부분을 정하면 진행이 더 편할까?");
            }
            default -> {
                questions.add(keyword + " 쪽은 보통 어떤 걸 좋아해?");
                questions.add("최근에 가장 기억에 남았던 일 하나만 꼽으면 뭐야?");
                questions.add("다음에 이어서 얘기해보고 싶은 주제 있어?");
            }
        }

        return List.copyOf(questions);
    }

    private List<String> trimTo(List<String> values, int limit) {
        return values.stream().distinct().limit(limit).toList();
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return "general";
        }
        if (category.contains("썸") || category.contains("연애")) {
            return "romance";
        }
        if (category.contains("친구") || category.contains("우정")) {
            return "friend";
        }
        if (category.contains("직장") || category.contains("상사")) {
            return "work";
        }
        if (category.contains("팀플") || category.contains("동아리")) {
            return "team";
        }
        return "general";
    }
}
