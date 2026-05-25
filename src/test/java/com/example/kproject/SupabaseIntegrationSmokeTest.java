package com.example.kproject;

import com.example.kproject.domain.AnalysisResult;
import com.example.kproject.domain.AnalysisType;
import com.example.kproject.domain.ChatSourceType;
import com.example.kproject.domain.ConversationReport;
import com.example.kproject.domain.ReportStatus;
import com.example.kproject.repository.AnalysisResultRepository;
import com.example.kproject.repository.ConversationReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SupabaseIntegrationSmokeTest {

    @Autowired
    private ConversationReportRepository conversationReportRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Test
    void saveAndReadReportAndAnalysisFromSupabase() {
        ConversationReport report = new ConversationReport(
                "integration-smoke",
                ChatSourceType.TXT,
                "raw text",
                "{\"messages\":[]}",
                "[\"Alice\",\"Bob\"]",
                0,
                ReportStatus.COMPLETED,
                "FULL",
                null
        );

        ConversationReport savedReport = conversationReportRepository.save(report);
        assertTrue(conversationReportRepository.findById(savedReport.getId()).isPresent());

        AnalysisResult result = new AnalysisResult(
                savedReport.getId(),
                AnalysisType.SUMMARY,
                "{\"ok\":true}"
        );
        AnalysisResult savedResult = analysisResultRepository.save(result);

        Optional<AnalysisResult> loaded = analysisResultRepository.findByReportIdAndAnalysisType(
                savedReport.getId(),
                AnalysisType.SUMMARY.name()
        );

        assertTrue(loaded.isPresent());
        assertEquals(savedReport.getId(), loaded.get().getReportId());

        analysisResultRepository.deleteById(savedResult.getId());
        conversationReportRepository.deleteById(savedReport.getId());
    }
}
