package com.example.kproject.service.ai;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.dto.report.ReportResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NoOpReportNarrativeAiService implements ReportNarrativeAiService {

    @Override
    public Optional<String> generateHeadline(ReportAnalysisContext context, int interestScore, String fallbackHeadline) {
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> generateRecommendedQuestions(
            ReportAnalysisContext context,
            List<String> keywords,
            List<String> fallbackQuestions
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<String> generateMomentDescription(
            ReportAnalysisContext context,
            ReportResponse.DecisiveMoment moment,
            String fallbackDescription
    ) {
        return Optional.empty();
    }
}
