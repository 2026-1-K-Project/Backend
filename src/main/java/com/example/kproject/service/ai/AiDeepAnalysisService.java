package com.example.kproject.service.ai;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.AiDeepAnalysisResponse;
import com.example.kproject.dto.report.ReportInsightsResponse;
import com.example.kproject.dto.report.ReportPersonalityResponse;
import com.example.kproject.dto.report.ReportRelationshipResponse;
import com.example.kproject.dto.report.ReportSummaryResponse;

import java.util.Optional;

public interface AiDeepAnalysisService {

    Optional<AiDeepAnalysisResponse> analyze(
            ReportAnalysisContext context,
            NormalizedConversationDto normalized,
            ReportSummaryResponse summary,
            ReportRelationshipResponse relationship,
            ReportPersonalityResponse personality,
            ReportInsightsResponse insights,
            String userRequest
    );
}
