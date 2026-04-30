package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExtractionServiceTest {

    private final KeywordExtractionService keywordExtractionService = new KeywordExtractionService();

    @Test
    void extractFrequentKeywordsWithoutStopwords() {
        ReportAnalysisContext context = new ReportAnalysisContext(
                "일반 분석",
                List.of("나", "상대방"),
                List.of("상대방"),
                "나",
                List.of(
                        new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 0), "내일 카페 갈래?"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 1), "좋아 카페 좋지"),
                        new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 2), "그 카페 디저트가 맛있대"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 3), "디저트 좋다 ㅋㅋ")
                ),
                ""
        );

        List<String> keywords = keywordExtractionService.extract(context);

        assertThat(keywords).contains("카페");
        assertThat(keywords).doesNotContain("오늘");
    }
}
