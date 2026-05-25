package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterestScoreAnalysisServiceTest {

    private final InterestScoreAnalysisService interestScoreAnalysisService =
            new InterestScoreAnalysisService(new ReplyTimeAnalysisService());

    @Test
    void calculateHigherInterestWhenPositiveQuestionsAndFastRepliesExist() {
        ReportAnalysisContext context = new ReportAnalysisContext(
                "썸/연애",
                List.of("나", "상대방"),
                List.of("상대방"),
                "나",
                List.of(
                        new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 0), "오늘 뭐해?"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 2), "집이지 ㅋㅋ 너는?"),
                        new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 3), "카페 갈까?"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 5), "좋아 같이 가자!")
                ),
                ""
        );

        int score = interestScoreAnalysisService.calculate(context);

        assertThat(score).isGreaterThanOrEqualTo(70);
    }
}
