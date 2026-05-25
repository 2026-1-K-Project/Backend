package com.example.kproject.service.ai;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.dto.report.ReportResponse;

import java.util.List;
import java.util.Optional;

public interface ReportNarrativeAiService {

    Optional<String> generateHeadline(ReportAnalysisContext context, int interestScore, String fallbackHeadline);

    Optional<List<String>> generateRecommendedQuestions(
            ReportAnalysisContext context,
            List<String> keywords,
            List<String> fallbackQuestions
    );

    Optional<String> generateMomentDescription(
            ReportAnalysisContext context,
            ReportResponse.DecisiveMoment moment,
            String fallbackDescription
    );
}
