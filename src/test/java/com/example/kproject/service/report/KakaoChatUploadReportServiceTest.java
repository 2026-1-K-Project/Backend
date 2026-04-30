package com.example.kproject.service.report;

import com.example.kproject.domain.ConversationReport;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.repository.ConversationReportRepository;
import com.example.kproject.service.KakaoChatFileParserService;
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
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class KakaoChatUploadReportServiceTest {

    @Test
    void generateReportDirectlyFromUploadedKakaoTxt() throws Exception {
        ConversationReportRepository repository = Mockito.mock(ConversationReportRepository.class);
        Mockito.when(repository.save(any(ConversationReport.class)))
                .thenAnswer(invocation -> {
                    ConversationReport report = invocation.getArgument(0);
                    if (report.getId() == null) {
                        Field field = ConversationReport.class.getDeclaredField("id");
                        field.setAccessible(true);
                        field.set(report, 1L);
                    }
                    return report;
                });

        ReplyTimeAnalysisService replyTimeAnalysisService = new ReplyTimeAnalysisService();
        NoOpReportNarrativeAiService aiService = new NoOpReportNarrativeAiService();
        ReportGenerationService reportGenerationService = new ReportGenerationService(
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

        KakaoChatUploadReportService service = new KakaoChatUploadReportService(
                new KakaoChatFileParserService(),
                reportGenerationService
        );

        ReportResponse response = service.generateFromKakaoTxt(
                file("""
                        상대방 님과 카카오톡 대화
                        저장한 날짜 : 2026-04-29 18:13:04
                        --------------- 2026년 4월 29일 수요일 ---------------
                        [나의이름] [오전 10:48] 오늘 뭐해?
                        [상대방] [오전 10:49] 집이지 ㅋㅋ 너는?
                        [나의이름] [오전 10:50] 내일 카페 갈래?
                        [상대방] [오전 10:51] 좋아 같이 가자!
                        """),
                "썸/연애",
                null
        );

        assertThat(response.reportId()).isEqualTo(1L);
        assertThat(response.category()).isEqualTo("썸/연애");
        assertThat(response.summary().headline()).isNotBlank();
        assertThat(response.relationshipDynamics().talkRatio().me()).isEqualTo(50);
        assertThat(response.relationshipDynamics().talkRatio().other()).isEqualTo(50);
        assertThat(response.relationshipDynamics().keywords()).isNotEmpty();
        assertThat(response.actionableInsights().recommendedQuestions()).hasSizeGreaterThanOrEqualTo(3);
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile(
                "file",
                "kakao-chat.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
