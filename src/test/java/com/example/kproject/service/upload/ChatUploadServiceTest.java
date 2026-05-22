package com.example.kproject.service.upload;

import com.example.kproject.domain.ConversationReport;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.dto.upload.ChatUploadResponse;
import com.example.kproject.repository.ConversationReportRepository;
import com.example.kproject.service.KakaoChatFileParserService;
import com.example.kproject.service.analysis.KeywordExtractionService;
import com.example.kproject.service.normalize.ConversationNormalizeService;
import com.example.kproject.service.report.ReportStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

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

    private ChatUploadService buildService(ConversationReportRepository repository) {
        KeywordExtractionService keywordExtractionService = new KeywordExtractionService();
        return new ChatUploadService(
                new KakaoChatFileParserService(),
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
}
