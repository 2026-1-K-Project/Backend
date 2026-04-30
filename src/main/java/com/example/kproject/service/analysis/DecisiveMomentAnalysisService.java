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
            return "함께하자는 제안 이후 대화 온도가 올라간 것으로 추정됩니다.";
        }
        if (ReportTextUtils.hasPositiveTone(content)) {
            return "긍정 표현이 늘어나면서 분위기가 부드러워진 것으로 보입니다.";
        }
        if (ReportTextUtils.hasNegativeTone(content)) {
            return "이 구간에서 대화의 긴장도가 잠시 높아진 것으로 추정됩니다.";
        }
        if (ReportTextUtils.isQuestion(content)) {
            return "상대를 더 알고자 하는 질문이 흐름 전환에 기여한 것으로 보입니다.";
        }
        return "이 문장이 대화 흐름 변곡점으로 작용한 것으로 추정됩니다.";
    }

    private record MomentWithImpact(
            ReportResponse.DecisiveMoment moment,
            int impact
    ) {
    }
}
