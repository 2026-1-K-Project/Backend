package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.service.ai.ReportNarrativeAiService;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class DecisiveMomentAnalysisService {

    private final ReportNarrativeAiService reportNarrativeAiService;

    public DecisiveMomentAnalysisService(ReportNarrativeAiService reportNarrativeAiService) {
        this.reportNarrativeAiService = reportNarrativeAiService;
    }

    public List<ReportResponse.DecisiveMoment> analyze(ReportAnalysisContext context) {
        return context.messages().stream()
                .filter(message -> ReportTextUtils.hasText(message.content()))
                .map(message -> buildMoment(context, message))
                .filter(momentWithImpact -> momentWithImpact.impact() > 14)
                .sorted(Comparator.comparingInt(MomentWithImpact::impact).reversed())
                .limit(3)
                .map(MomentWithImpact::moment)
                .toList();
    }

    public List<ReportResponse.DecisiveMoment> analyzeFlexible(List<String> contents) {
        return contents.stream()
                .filter(ReportTextUtils::hasText)
                .map(this::buildFlexibleMoment)
                .filter(momentWithImpact -> momentWithImpact.impact() > 14)
                .sorted(Comparator.comparingInt(MomentWithImpact::impact).reversed())
                .limit(3)
                .map(MomentWithImpact::moment)
                .toList();
    }

    private MomentWithImpact buildMoment(ReportAnalysisContext context, ReportMessage message) {
        int impact = impactScore(message.content());
        String fallbackDescription = descriptionFor(message.content());
        ReportResponse.DecisiveMoment baseMoment = new ReportResponse.DecisiveMoment(
                "결정적 순간",
                message.dateTime().toString(),
                ReportTextUtils.safeText(message.content()),
                fallbackDescription
        );
        String description = reportNarrativeAiService.generateMomentDescription(context, baseMoment, fallbackDescription)
                .orElse(fallbackDescription);

        return new MomentWithImpact(
                new ReportResponse.DecisiveMoment(
                        baseMoment.title(),
                        baseMoment.dateTime(),
                        baseMoment.message(),
                        description
                ),
                impact
        );
    }

    private MomentWithImpact buildFlexibleMoment(String content) {
        String normalizedContent = ReportTextUtils.safeText(content);
        return new MomentWithImpact(
                new ReportResponse.DecisiveMoment(
                        "결정적 순간",
                        null,
                        normalizedContent,
                        descriptionFor(normalizedContent)
                ),
                impactScore(normalizedContent)
        );
    }

    private int impactScore(String content) {
        int impact = 0;
        if (ReportTextUtils.hasProposal(content)) {
            impact += 25;
        }
        if (ReportTextUtils.hasPositiveTone(content)) {
            impact += 18;
        }
        if (ReportTextUtils.hasNegativeTone(content)) {
            impact += 16;
        }
        if (ReportTextUtils.isQuestion(content)) {
            impact += 10;
        }
        return impact;
    }

    private String descriptionFor(String content) {
        if (ReportTextUtils.hasProposal(content)) {
            return "함께하자는 제안 이후 분위기가 더 적극적으로 바뀔 여지가 있어 보입니다.";
        }
        if (ReportTextUtils.hasPositiveTone(content)) {
            return "긍정 표현이 늘어나면서 대화 온도가 올라간 구간으로 추정됩니다.";
        }
        if (ReportTextUtils.hasNegativeTone(content)) {
            return "이 구간에서는 대화의 긴장도나 피로도가 잠시 높아진 것으로 보입니다.";
        }
        if (ReportTextUtils.isQuestion(content)) {
            return "질문이 대화 흐름을 다시 열어 준 구간으로 추정됩니다.";
        }
        return "이 문장이 대화 흐름의 변곡점 역할을 한 것으로 보입니다.";
    }

    private record MomentWithImpact(
            ReportResponse.DecisiveMoment moment,
            int impact
    ) {
    }
}
