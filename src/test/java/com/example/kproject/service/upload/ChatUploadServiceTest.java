package com.example.kproject.service.upload;

import com.example.kproject.domain.ConversationReport;
import com.example.kproject.domain.NormalizedConversationResult;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.dto.upload.ChatUploadResponse;
import com.example.kproject.repository.ConversationReportRepository;
import com.example.kproject.service.KakaoChatFileParserService;
import com.example.kproject.service.analysis.KeywordExtractionService;
import com.example.kproject.service.normalize.AiConversationNormalizeService;
import com.example.kproject.service.normalize.ConversationNormalizeService;
import com.example.kproject.service.report.ReportStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class ChatUploadServiceTest {

    @Test
    void createReportAfterTxtUpload() throws Exception {
        ConversationReportRepository repository = mockRepositoryWithId(1L);
        ChatUploadService service = buildService(repository);

        ChatUploadResponse response = service.upload(file(
                "chat.txt",
                "text/plain",
                """
                        홍길동 님과 카카오톡 대화
                        저장한 날짜 : 2026-04-29 18:13:04
                        --------------- 2026년 4월 29일 수요일 ---------------
                        [나] [오전 10:48] 오늘 뭐해?
                        [홍길동] [오전 10:49] 집이지 ㅋㅋ 너는?
                        """
        ), "썸/연애", "홍길동");

        ArgumentCaptor<ConversationReport> captor = ArgumentCaptor.forClass(ConversationReport.class);
        Mockito.verify(repository).save(captor.capture());

        assertThat(response.reportId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.analysisMode()).isEqualTo(ReportAnalysisMode.STRUCTURED);
        assertThat(captor.getValue().getRawText()).contains("오늘 뭐해?");
        assertThat(captor.getValue().getNormalizedJson()).contains("홍길동");
        assertThat(captor.getValue().getMessageCount()).isEqualTo(2);
    }

    @Test
    void createFlexibleReportWhenTxtFormatIsIncomplete() throws Exception {
        ConversationReportRepository repository = mockRepositoryWithId(2L);
        ChatUploadService service = buildService(repository);

        ChatUploadResponse response = service.upload(file(
                "chat.txt",
                "text/plain",
                """
                        오늘은 대화 형식이 깨져 있음
                        그래도 저녁이랑 카페 얘기가 있음
                        다음에 보자는 흐름도 있음
                        """
        ), "일반 분석", "상대방");

        assertThat(response.reportId()).isEqualTo(2L);
        assertThat(response.analysisMode()).isEqualTo(ReportAnalysisMode.FLEXIBLE);
        assertThat(response.warning()).isNotBlank();
    }

    @Test
    void createImageReportWithPlaceholderWarning() throws Exception {
        ConversationReportRepository repository = mockRepositoryWithId(3L);
        ChatUploadService service = buildService(repository);

        ChatUploadResponse response = service.upload(file(
                "capture.png",
                "image/png",
                "fake-image-binary"
        ), "썸/연애", "상대방");

        ArgumentCaptor<ConversationReport> captor = ArgumentCaptor.forClass(ConversationReport.class);
        Mockito.verify(repository).save(captor.capture());

        assertThat(response.reportId()).isEqualTo(3L);
        assertThat(response.analysisMode()).isEqualTo(ReportAnalysisMode.FLEXIBLE);
        assertThat(response.warning()).contains("OCR");
        assertThat(captor.getValue().getSourceType()).isEqualTo("IMAGE");
    }

    @Test
    void useAiNormalizationBeforeLocalParsing() throws Exception {
        ConversationReportRepository repository = mockRepositoryWithId(4L);
        AiConversationNormalizeService aiNormalizer = Mockito.mock(AiConversationNormalizeService.class);
        Mockito.when(aiNormalizer.normalizeText(any(), any()))
                .thenReturn(Optional.of(new NormalizedConversationResult(
                        new NormalizedConversationDto(
                                List.of("사용자", "상대방"),
                                List.of(new NormalizedConversationDto.MessageDto(
                                        "상대방",
                                        "2026-04-29T10:48:00",
                                        "AI가 정형화한 메시지",
                                        "TEXT"
                                )),
                                List.of("AI"),
                                "AI raw text"
                        ),
                        ReportAnalysisMode.STRUCTURED,
                        true,
                        "OpenAI를 사용해 비정형 원본을 정형화했습니다."
                )));
        ChatUploadService service = buildService(repository, aiNormalizer);

        ChatUploadResponse response = service.upload(file(
                "chat.txt",
                "text/plain",
                "정형화하기 어려운 원본"
        ), "썸/연애", "상대방");

        ArgumentCaptor<ConversationReport> captor = ArgumentCaptor.forClass(ConversationReport.class);
        Mockito.verify(repository).save(captor.capture());

        assertThat(response.reportId()).isEqualTo(4L);
        assertThat(response.analysisMode()).isEqualTo(ReportAnalysisMode.STRUCTURED);
        assertThat(captor.getValue().getNormalizedJson()).contains("AI가 정형화한 메시지");
    }

    @Test
    void createMixedReportAfterBatchUpload() throws Exception {
        ConversationReportRepository repository = mockRepositoryWithId(5L);
        AiConversationNormalizeService aiNormalizer = Mockito.mock(AiConversationNormalizeService.class);
        Mockito.when(aiNormalizer.normalizeFiles(any(), any(), any()))
                .thenReturn(Optional.of(new NormalizedConversationResult(
                        new NormalizedConversationDto(
                                List.of("사용자", "상대방"),
                                List.of(new NormalizedConversationDto.MessageDto(
                                        "상대방",
                                        "2026-04-29T10:48:00",
                                        "여러 파일에서 정형화한 메시지",
                                        "TEXT"
                                )),
                                List.of("정형화"),
                                "batch raw text"
                        ),
                        ReportAnalysisMode.STRUCTURED,
                        true,
                        "OpenAI를 사용해 여러 업로드 파일을 정형화했습니다."
                )));
        ChatUploadService service = buildService(repository, aiNormalizer);

        ChatUploadResponse response = service.uploadBatch(
                List.of(
                        file("chat.txt", "text/plain", "카카오톡 txt 내용"),
                        file("capture.png", "image/png", "fake-image-binary")
                ),
                "썸",
                "상대방",
                "호감 신호가 있는지 분석해줘"
        );

        ArgumentCaptor<ConversationReport> captor = ArgumentCaptor.forClass(ConversationReport.class);
        Mockito.verify(repository).save(captor.capture());

        assertThat(response.reportId()).isEqualTo(5L);
        assertThat(response.analysisMode()).isEqualTo(ReportAnalysisMode.STRUCTURED);
        assertThat(captor.getValue().getSourceType()).isEqualTo("MIXED");
        assertThat(captor.getValue().getUploadedFileCount()).isEqualTo(2);
        assertThat(captor.getValue().getDescription()).contains("호감 신호");
        assertThat(captor.getValue().getNormalizedJson()).contains("여러 파일에서 정형화한 메시지");
    }

    private ChatUploadService buildService(ConversationReportRepository repository) {
        return buildService(repository, new EmptyAiConversationNormalizeService());
    }

    private ChatUploadService buildService(
            ConversationReportRepository repository,
            AiConversationNormalizeService aiConversationNormalizeService
    ) {
        KeywordExtractionService keywordExtractionService = new KeywordExtractionService();
        return new ChatUploadService(
                new KakaoChatFileParserService(),
                aiConversationNormalizeService,
                new ConversationNormalizeService(keywordExtractionService),
                new ImageTextExtractionService(),
                new ReportStorageService(repository, new ObjectMapper())
        );
    }

    private ConversationReportRepository mockRepositoryWithId(Long id) throws Exception {
        ConversationReportRepository repository = Mockito.mock(ConversationReportRepository.class);
        Mockito.when(repository.save(any(ConversationReport.class)))
                .thenAnswer(invocation -> {
                    ConversationReport report = invocation.getArgument(0);
                    Field field = ConversationReport.class.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(report, id);
                    return report;
                });
        return repository;
    }

    private MockMultipartFile file(String filename, String contentType, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static class EmptyAiConversationNormalizeService implements AiConversationNormalizeService {

        @Override
        public Optional<NormalizedConversationResult> normalizeText(String rawText, String targetName) {
            return Optional.empty();
        }

        @Override
        public Optional<NormalizedConversationResult> normalizeImage(org.springframework.web.multipart.MultipartFile file, String targetName) {
            return Optional.empty();
        }
    }
}
