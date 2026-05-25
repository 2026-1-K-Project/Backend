package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import com.example.kproject.dto.report.ReportResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TalkRatioAnalysisServiceTest {

    private final TalkRatioAnalysisService talkRatioAnalysisService = new TalkRatioAnalysisService();

    @Test
    void calculateTalkRatioByMessageCount() {
        ReportAnalysisContext context = context(
                new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 0), "안녕"),
                new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 1), "안녕"),
                new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 2), "오늘 뭐해?"),
                new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 3), "집이야"),
                new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 4), "너는?")
        );

        ReportResponse.TalkRatio talkRatio = talkRatioAnalysisService.calculate(context);

        assertThat(talkRatio.me()).isEqualTo(40);
        assertThat(talkRatio.other()).isEqualTo(60);
    }

    private ReportAnalysisContext context(ReportMessage... messages) {
        return new ReportAnalysisContext(
                "썸/연애",
                List.of("나", "상대방"),
                List.of("상대방"),
                "나",
                List.of(messages),
                ""
        );
    }
}
