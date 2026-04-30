package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;

@Service
public class TalkRatioAnalysisService {

    public ReportResponse.TalkRatio calculate(ReportAnalysisContext context) {
        long totalCount = context.messages().size();
        long meCount = context.messages().stream()
                .filter(message -> context.isMe(message.sender()))
                .count();

        int mePercentage = ReportTextUtils.percentage(meCount, totalCount);
        return new ReportResponse.TalkRatio(mePercentage, 100 - mePercentage);
    }
}
