package com.example.kproject.service.report;

import com.example.kproject.domain.AnalysisResult;
import com.example.kproject.domain.ChatSourceType;
import com.example.kproject.domain.ConversationReport;
import com.example.kproject.domain.NormalizedConversationResult;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.dto.report.ReportInsightsResponse;
import com.example.kproject.dto.report.ReportPersonalityResponse;
import com.example.kproject.dto.report.ReportPreferencesResponse;
import com.example.kproject.dto.report.ReportRelationshipResponse;
import com.example.kproject.repository.AnalysisResultRepository;
import com.example.kproject.repository.ConversationReportRepository;
import com.example.kproject.service.ai.NoOpReportNarrativeAiService;
import com.example.kproject.service.analysis.DecisiveMomentAnalysisService;
import com.example.kproject.service.analysis.EmotionTimelineAnalysisService;
import com.example.kproject.service.analysis.InterestScoreAnalysisService;
import com.example.kproject.service.analysis.KeywordExtractionService;
import com.example.kproject.service.analysis.LanguageSyncAnalysisService;
import com.example.kproject.service.analysis.PersonalityPsychologyAnalysisService;
import com.example.kproject.service.analysis.PreferenceAnalysisService;
import com.example.kproject.service.analysis.QualitativeSignalsAnalysisService;
import com.example.kproject.service.analysis.ReplyTimeAnalysisService;
import com.example.kproject.service.analysis.TalkRatioAnalysisService;
import com.example.kproject.service.insight.ActionableInsightService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ReportSectionQueryServiceTest {

    @Test
    void generateRelationshipPersonalityPreferencesAndInsightsResponses() throws Exception {
        ConversationReportRepository reportRepository = Mockito.mock(ConversationReportRepository.class);
        AnalysisResultRepository analysisResultRepository = Mockito.mock(AnalysisResultRepository.class);
        Mockito.when(analysisResultRepository.findByReportIdAndAnalysisType(eq(1L), any()))
                .thenReturn(Optional.empty());
        Mockito.when(analysisResultRepository.save(any(AnalysisResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtomicReference<ConversationReport> savedReport = new AtomicReference<>();
        Mockito.when(reportRepository.save(any(ConversationReport.class)))
                .thenAnswer(invocation -> {
                    ConversationReport report = invocation.getArgument(0);
                    Field field = ConversationReport.class.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(report, 1L);
                    savedReport.set(report);
                    return report;
                });

        ReportStorageService storageService = new ReportStorageService(reportRepository, new ObjectMapper());
        ConversationReport report = storageService.createReport(
                "썸/연애",
                ChatSourceType.TXT,
                new NormalizedConversationResult(conversation(), ReportAnalysisMode.STRUCTURED, true, null)
        );
        Mockito.when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportSectionQueryService service = buildSectionQueryService(storageService, analysisResultRepository);

        ReportRelationshipResponse relationship = service.getRelationship(1L);
        ReportPersonalityResponse personality = service.getPersonality(1L);
        ReportPreferencesResponse preferences = service.getPreferences(1L);
        ReportInsightsResponse insights = service.getInsights(1L);

        assertThat(savedReport.get()).isNotNull();
        assertThat(relationship.interestScore()).isBetween(0, 100);
        assertThat(relationship.talkRatio().me()).isEqualTo(50);
        assertThat(personality.mbti().description()).contains("추정");
        assertThat(preferences.likedPhrases()).isNotEmpty();
        assertThat(preferences.favoriteTopics()).isNotEmpty();
        assertThat(insights.tips()).isNotEmpty();
        assertThat(insights.recommendedQuestions()).isNotEmpty();
        assertThat(insights.recommendedReplies()).isNotEmpty();
    }

    private NormalizedConversationDto conversation() {
        return new NormalizedConversationDto(
                List.of("사용자", "상대방"),
                List.of(
                        new NormalizedConversationDto.MessageDto("사용자", "2026-04-29T10:48:00", "오늘 뭐해?", "TEXT"),
                        new NormalizedConversationDto.MessageDto("상대방", "2026-04-29T10:49:00", "집이지 ㅋㅋ 너는?", "TEXT"),
                        new NormalizedConversationDto.MessageDto("사용자", "2026-04-29T10:50:00", "내일 카페 갈래?", "TEXT"),
                        new NormalizedConversationDto.MessageDto("상대방", "2026-04-29T10:51:00", "좋아 같이 가자!", "TEXT")
                ),
                List.of("카페", "내일"),
                "raw text"
        );
    }

    private ReportSectionQueryService buildSectionQueryService(
            ReportStorageService storageService,
            AnalysisResultRepository analysisResultRepository
    ) {
        ReplyTimeAnalysisService replyTimeAnalysisService = new ReplyTimeAnalysisService();
        KeywordExtractionService keywordExtractionService = new KeywordExtractionService();
        NoOpReportNarrativeAiService aiService = new NoOpReportNarrativeAiService();

        return new ReportSectionQueryService(
                storageService,
                analysisResultRepository,
                new ObjectMapper(),
                new TalkRatioAnalysisService(),
                replyTimeAnalysisService,
                new InterestScoreAnalysisService(replyTimeAnalysisService),
                new LanguageSyncAnalysisService(),
                keywordExtractionService,
                new PersonalityPsychologyAnalysisService(),
                new EmotionTimelineAnalysisService(),
                new DecisiveMomentAnalysisService(aiService),
                new ActionableInsightService(aiService),
                new QualitativeSignalsAnalysisService(keywordExtractionService),
                new PreferenceAnalysisService(),
                (context, normalized, summary, relationship, personality, insights, userRequest) -> Optional.empty()
        );
    }
}
