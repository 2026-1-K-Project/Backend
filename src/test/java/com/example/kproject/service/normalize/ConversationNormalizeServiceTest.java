package com.example.kproject.service.normalize;

import com.example.kproject.domain.KakaoChatParsedDocument;
import com.example.kproject.domain.NormalizedConversationResult;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.service.KakaoChatFileParserService;
import com.example.kproject.service.analysis.KeywordExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationNormalizeServiceTest {

    private final KakaoChatFileParserService parserService = new KakaoChatFileParserService();
    private final ConversationNormalizeService normalizeService =
            new ConversationNormalizeService(new KeywordExtractionService());

    @Test
    void convertRawTextToNormalizedConversation() {
        KakaoChatParsedDocument parsedDocument = parserService.parseDocument(file("""
                홍길동 님과 카카오톡 대화
                저장한 날짜 : 2026-04-29 18:13:04
                --------------- 2026년 4월 29일 수요일 ---------------
                [나] [오전 10:48] 오늘 뭐해?
                [홍길동] [오전 10:49] 집이지 ㅋㅋ 너는?
                """));

        NormalizedConversationResult result = normalizeService.normalize(parsedDocument, "홍길동");

        assertThat(result.analysisMode()).isEqualTo(ReportAnalysisMode.STRUCTURED);
        assertThat(result.structuredParsingAvailable()).isTrue();
        assertThat(result.conversation().participants()).containsExactly("사용자", "홍길동");
        assertThat(result.conversation().messages()).hasSize(2);
        assertThat(result.conversation().messages().get(0).sender()).isEqualTo("사용자");
        assertThat(result.conversation().messages().get(1).sender()).isEqualTo("홍길동");
        assertThat(result.conversation().rawText()).contains("오늘 뭐해?");
    }

    @Test
    void fallbackToFlexibleConversationWhenFormatIsIncomplete() {
        KakaoChatParsedDocument parsedDocument = parserService.parseDocument(file("""
                오늘은 대화 형식이 조금 깨져 있어.
                그래도 카페 얘기랑 퇴근 얘기가 나오고
                다음에 보자는 말도 있어 ㅋㅋ
                """));

        NormalizedConversationResult result = normalizeService.normalize(parsedDocument, "상대방");

        assertThat(result.analysisMode()).isEqualTo(ReportAnalysisMode.FLEXIBLE);
        assertThat(result.structuredParsingAvailable()).isFalse();
        assertThat(result.warning()).isNotBlank();
        assertThat(result.conversation().messages()).isNotEmpty();
        assertThat(result.conversation().messages()).allMatch(message -> "TEXT".equals(message.type()));
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
