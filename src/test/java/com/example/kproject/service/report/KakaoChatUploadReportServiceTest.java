package com.example.kproject.service.report;

import com.example.kproject.domain.ConversationReport;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.dto.report.ReportResponse;
import com.example.kproject.exception.ChatUploadException;
import com.example.kproject.repository.ConversationReportRepository;
import com.example.kproject.service.KakaoChatFileParserService;
import com.example.kproject.service.ai.NoOpReportNarrativeAiService;
import com.example.kproject.service.analysis.DecisiveMomentAnalysisService;
import com.example.kproject.service.analysis.EmotionTimelineAnalysisService;
import com.example.kproject.service.analysis.InterestScoreAnalysisService;
import com.example.kproject.service.analysis.KeywordExtractionService;
import com.example.kproject.service.analysis.LanguageSyncAnalysisService;
import com.example.kproject.service.analysis.PersonalityPsychologyAnalysisService;
import com.example.kproject.service.analysis.QualitativeSignalsAnalysisService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class KakaoChatUploadReportServiceTest {

    @Test
    void useStructuredModeWhenStructuredParsingSucceeds() throws Exception {
        KakaoChatUploadReportService service = buildUploadReportService();

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

        assertThat(response.analysisMode()).isEqualTo(ReportAnalysisMode.STRUCTURED);
        assertThat(response.structuredParsingAvailable()).isTrue();
        assertThat(response.relationshipDynamics().averageReplyMinutes()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void fallBackToFlexibleModeWhenFormatIsImperfectButTextExists() throws Exception {
        KakaoChatUploadReportService service = buildUploadReportService();

        ReportResponse response = service.generateFromKakaoTxt(
                file("""
                        상대방 님과 카카오톡 대화
                        저장한 날짜 : 2026-04-29 18:13:04
                        대충 저장한 대화 메모
                        [나의이름] [오전 10:48] 오늘 뭐해?
                        이 다음 줄은 구조가 애매하고
                        중간에 형식도 흔들리고
                        상대방: 나중에 보자 ㅎㅎ
                        형식이 일정하지 않네
                        """),
                "일반 분석",
                null
        );

        assertThat(response.analysisMode()).isEqualTo(ReportAnalysisMode.FLEXIBLE);
        assertThat(response.structuredParsingAvailable()).isFalse();
        assertThat(response.warning()).isNotBlank();
        assertThat(response.qualitativeSignals().relationshipSummary()).isNotBlank();
        assertThat(response.actionableInsights().recommendedQuestions()).isNotEmpty();
    }

    @Test
    void useFlexibleModeForFreeformText() throws Exception {
        KakaoChatUploadReportService service = buildUploadReportService();

        ReportResponse response = service.generateFromKakaoTxt(
                file("""
                        오늘은 좀 피곤했어.
                        그래도 너랑 얘기하면 마음이 편해지더라.
                        다음에 시간 되면 카페 가자.
                        """),
                "친구/우정",
                "상대방"
        );

        assertThat(response.analysisMode()).isEqualTo(ReportAnalysisMode.FLEXIBLE);
        assertThat(response.structuredParsingAvailable()).isFalse();
        assertThat(response.summary().headline()).isNotBlank();
        assertThat(response.qualitativeSignals().recommendedReplies()).hasSize(3);
    }

    @Test
    void rejectOnlyWhenFileIsEmpty() throws Exception {
        KakaoChatUploadReportService service = buildUploadReportService();

        assertThatThrownBy(() -> service.generateFromKakaoTxt(
                file(""),
                "일반 분석",
                null
        )).isInstanceOf(ChatUploadException.class);
    }

    private KakaoChatUploadReportService buildUploadReportService() throws Exception {
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
        KeywordExtractionService keywordExtractionService = new KeywordExtractionService();
        NoOpReportNarrativeAiService aiService = new NoOpReportNarrativeAiService();
        ReportPersistenceService reportPersistenceService = new ReportPersistenceService(repository, new ObjectMapper());
        ReportGenerationService reportGenerationService = new ReportGenerationService(
                reportPersistenceService,
                aiService,
                new TalkRatioAnalysisService(),
                replyTimeAnalysisService,
                new InterestScoreAnalysisService(replyTimeAnalysisService),
                new LanguageSyncAnalysisService(),
                keywordExtractionService,
                new PersonalityPsychologyAnalysisService(),
                new EmotionTimelineAnalysisService(),
                new DecisiveMomentAnalysisService(aiService),
                new ActionableInsightService(aiService),
                new QualitativeSignalsAnalysisService(keywordExtractionService)
        );

        FlexibleTextReportService flexibleTextReportService = new FlexibleTextReportService(
                reportPersistenceService,
                keywordExtractionService,
                new InterestScoreAnalysisService(replyTimeAnalysisService),
                new PersonalityPsychologyAnalysisService(),
                new EmotionTimelineAnalysisService(),
                new DecisiveMomentAnalysisService(aiService),
                new ActionableInsightService(aiService),
                new QualitativeSignalsAnalysisService(keywordExtractionService)
        );

        return new KakaoChatUploadReportService(
                new KakaoChatFileParserService(),
                reportGenerationService,
                flexibleTextReportService
        );
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile(
                "file",
                "chat.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
