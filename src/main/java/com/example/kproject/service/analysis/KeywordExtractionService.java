package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.util.ReportTextUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeywordExtractionService {

    public List<String> extract(ReportAnalysisContext context) {
        List<String> contents = context.messages().stream()
                .map(message -> message.content())
                .filter(ReportTextUtils::hasText)
                .toList();

        return ReportTextUtils.distinctTopTokens(contents, 5);
    }
}
