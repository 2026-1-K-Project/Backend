package com.example.kproject.service.report;

import com.example.kproject.domain.ConversationReport;
import com.example.kproject.dto.report.ReportGenerateRequest;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.repository.ConversationReportRepository;
import com.example.kproject.service.ai.NoOpReportNarrativeAiService;
import com.example.kproject.service.analysis.DecisiveMomentAnalysisService;
import com.example.kproject.service.analysis.EmotionTimelineAnalysisService;
import com.example.kproject.service.analysis.InterestScoreAnalysisService;
import com.example.kproject.service.analysis.KeywordExtractionService;
import com.example.kproject.service.analysis.LanguageSyncAnalysisService;
import com.example.kproject.service.analysis.PersonalityPsychologyAnalysisService;
import com.example.kproject.service.analysis.ReplyTimeAnalysisService;
import com.example.kproject.service.analysis.TalkRatioAnalysisService;
import com.example.kproject.service.insight.ActionableInsightService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class ReportGenerationServiceTest {

    @Test
    void generateUiReadyReportResponse() {
        ConversationReportRepository repository = Mockito.mock(ConversationReportRepository.class);
        Mockito.when(repository.save(any(ConversationReport.class)))
                .thenAnswer(invocation -> {
                    ConversationReport report = invocation.getArgument(0);
                    if (report.getId() == null) {
                        java.lang.reflect.Field field = ConversationReport.class.getDeclaredField("id");
                        field.setAccessible(true);
                        field.set(report, 1L);
                    }
                    return report;
                });

        ReplyTimeAnalysisService replyTimeAnalysisService = new ReplyTimeAnalysisService();
        NoOpReportNarrativeAiService aiService = new NoOpReportNarrativeAiService();

        ReportGenerationService service = new ReportGenerationService(
                repository,
                new ObjectMapper(),
                aiService,
                new TalkRatioAnalysisService(),
                replyTimeAnalysisService,
                new InterestScoreAnalysisService(replyTimeAnalysisService),
                new LanguageSyncAnalysisService(),
                new KeywordExtractionService(),
                new PersonalityPsychologyAnalysisService(),
                new EmotionTimelineAnalysisService(),
                new DecisiveMomentAnalysisService(aiService),
                new ActionableInsightService(aiService)
        );

        ReportResponse response = service.generate(new ReportGenerateRequest(
                "썸/연애",
                List.of("나", "상대방"),
                List.of(
                        new ReportGenerateRequest.MessageDto("나", LocalDateTime.of(2026, 4, 29, 10, 48), "오늘 뭐해?"),
                        new ReportGenerateRequest.MessageDto("상대방", LocalDateTime.of(2026, 4, 29, 10, 49), "집이지 ㅋㅋ 너는?"),
                        new ReportGenerateRequest.MessageDto("나", LocalDateTime.of(2026, 4, 29, 10, 50), "내일 카페 갈래?"),
                        new ReportGenerateRequest.MessageDto("상대방", LocalDateTime.of(2026, 4, 29, 10, 51), "좋아 같이 가자!")
                ),
                null
        ));

        assertThat(response.reportId()).isEqualTo(1L);
        assertThat(response.category()).isEqualTo("썸/연애");
        assertThat(response.summary().interestScore()).isBetween(0, 100);
        assertThat(response.summary().headline()).isNotBlank();
        assertThat(response.relationshipDynamics().talkRatio().me()).isEqualTo(50);
        assertThat(response.relationshipDynamics().averageReplyMinutes()).isGreaterThanOrEqualTo(0);
        assertThat(response.relationshipDynamics().keywords()).isNotEmpty();
        assertThat(response.personalityPsychology().mbti().type()).hasSize(4);
        assertThat(response.emotionTimeline()).isNotEmpty();
        assertThat(response.decisiveMoments()).isNotEmpty();
        assertThat(response.actionableInsights().recommendedQuestions()).hasSizeGreaterThanOrEqualTo(3);
    }
}
