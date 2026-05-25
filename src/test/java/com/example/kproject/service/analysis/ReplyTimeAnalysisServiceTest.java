package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplyTimeAnalysisServiceTest {

    private final ReplyTimeAnalysisService replyTimeAnalysisService = new ReplyTimeAnalysisService();

    @Test
    void calculateAverageReplyMinutesAcrossTurns() {
        ReportAnalysisContext context = new ReportAnalysisContext(
                "일반 분석",
                List.of("나", "상대방"),
                List.of("상대방"),
                "나",
                List.of(
                        new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 0), "안녕"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 5), "왜?"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 8), "무슨 일 있어?"),
                        new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 20), "그냥 연락했어"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 26), "아하 ㅋㅋ")
                ),
                ""
        );

        ReplyTimeAnalysisService.ReplyTimeAnalysisResult result = replyTimeAnalysisService.calculate(context);

        assertThat(result.averageReplyMinutes()).isEqualTo(8);
        assertThat(result.meReplyMinutes()).isEqualTo(12);
        assertThat(result.otherReplyMinutes()).isEqualTo(6);
    }
}
