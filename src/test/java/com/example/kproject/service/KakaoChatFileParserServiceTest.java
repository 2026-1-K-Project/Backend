package com.example.kproject.service;

import com.example.kproject.domain.KakaoChatParsedDocument;
import com.example.kproject.dto.KakaoChatSpecialType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoChatFileParserServiceTest {

    private final KakaoChatFileParserService parserService = new KakaoChatFileParserService();

    @Test
    void parseSingleLineMessages() {
        KakaoChatParsedDocument response = parserService.parseDocument(file("""
                K프로젝트 26-1 님과 카카오톡 대화
                저장한 날짜 : 2026-04-29 18:13:04
                --------------- 2026년 4월 6일 월요일 ---------------
                [노지섭] [오전 10:48] 나만 문자왔어?
                [22 ict 강승재] [오후 1:06] 제출했냐
                """));

        assertThat(response.meta().title()).isEqualTo("K프로젝트 26-1 님과 카카오톡 대화");
        assertThat(response.meta().savedAt()).isEqualTo("2026-04-29 18:13:04");
        assertThat(response.meta().roomName()).isEqualTo("K프로젝트 26-1");
        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(0).sender()).isEqualTo("노지섭");
        assertThat(response.messages().get(0).date()).isEqualTo("2026-04-06");
        assertThat(response.messages().get(0).timeText()).isEqualTo("오전 10:48");
        assertThat(response.messages().get(0).dateTime()).isEqualTo("2026-04-06T10:48");
        assertThat(response.messages().get(0).content()).isEqualTo("나만 문자왔어?");
    }

    @Test
    void parseMultilineContinuationIncludingBlankLines() {
        KakaoChatParsedDocument response = parserService.parseDocument(file("""
                K프로젝트 26-1 님과 카카오톡 대화
                저장한 날짜 : 2026-04-29 18:13:04
                --------------- 2026년 4월 6일 월요일 ---------------
                [노지섭] [오전 10:48] 첫 줄 메시지
                둘째 줄 메시지
                
                셋째 줄 메시지
                [22 ict 강승재] [오후 1:06] 다음 메시지
                """));

        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(0).content()).isEqualTo("첫 줄 메시지\n둘째 줄 메시지\n\n셋째 줄 메시지");
    }

    @Test
    void classifySpecialMessageTypes() {
        KakaoChatParsedDocument response = parserService.parseDocument(file("""
                K프로젝트 26-1 님과 카카오톡 대화
                저장한 날짜 : 2026-04-29 18:13:04
                --------------- 2026년 4월 6일 월요일 ---------------
                [노지섭] [오전 10:48] 사진 3장
                [22 ict 강승재] [오전 10:49] 이모티콘
                [24 ict 최양하] [오전 10:50] 파일: report.pdf
                [노지섭] [오전 10:51] 메시지가 삭제되었습니다.
                [22 ict 강승재] [오전 10:52] 일반 텍스트
                """));

        assertThat(response.messages()).extracting(message -> message.specialType().name())
                .containsExactly("IMAGE", "EMOTICON", "FILE", "DELETED", "TEXT");
    }

    @Test
    void applyUpdatedDateWhenDateSeparatorChanges() {
        KakaoChatParsedDocument response = parserService.parseDocument(file("""
                K프로젝트 26-1 님과 카카오톡 대화
                저장한 날짜 : 2026-04-29 18:13:04
                --------------- 2026년 4월 6일 월요일 ---------------
                [노지섭] [오전 10:48] 첫째 날 메시지
                --------------- 2026년 4월 7일 화요일 ---------------
                [22 ict 강승재] [오전 9:01] 둘째 날 메시지
                """));

        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(0).date()).isEqualTo("2026-04-06");
        assertThat(response.messages().get(1).date()).isEqualTo("2026-04-07");
        assertThat(response.messages().get(1).dateTime()).isEqualTo("2026-04-07T09:01");
    }

    @Test
    void buildAnalysisTextFromTextMessagesOnly() {
        KakaoChatParsedDocument response = parserService.parseDocument(file("""
                K프로젝트 26-1 님과 카카오톡 대화
                저장한 날짜 : 2026-04-29 18:13:04
                --------------- 2026년 4월 6일 월요일 ---------------
                [노지섭] [오전 10:48] 안녕
                [22 ict 강승재] [오전 10:49] 사진
                [노지섭] [오전 10:50] 첫 줄
                  둘째 줄  
                
                [22 ict 강승재] [오전 10:51] 메시지가 삭제되었습니다.
                [24 ict 최양하] [오전 10:52] 좋아요
                """));

        assertThat(response.messages()).extracting(message -> message.specialType())
                .containsExactly(
                        KakaoChatSpecialType.TEXT,
                        KakaoChatSpecialType.IMAGE,
                        KakaoChatSpecialType.TEXT,
                        KakaoChatSpecialType.DELETED,
                        KakaoChatSpecialType.TEXT
                );
        assertThat(response.analysisText()).isEqualTo("""
                노지섭: 안녕
                노지섭: 첫 줄
                둘째 줄
                24 ict 최양하: 좋아요""");
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
