package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import com.example.kproject.dto.report.ReportResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmotionTimelineAnalysisServiceTest {

    private final EmotionTimelineAnalysisService emotionTimelineAnalysisService = new EmotionTimelineAnalysisService();

    @Test
    void createTimelineScoresForConversationFlow() {
        ReportAnalysisContext context = new ReportAnalysisContext(
                "썸/연애",
                List.of("나", "상대방"),
                List.of("상대방"),
                "나",
                List.of(
                        new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 0), "오늘 어땠어?"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 2), "피곤했어"),
                        new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 4), "고생했네 ㅠ"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 6), "그래도 너랑 얘기하니까 좋다 ㅎㅎ"),
                        new ReportMessage("나", LocalDateTime.of(2026, 4, 29, 10, 8), "내일 같이 영화 볼래?"),
                        new ReportMessage("상대방", LocalDateTime.of(2026, 4, 29, 10, 9), "좋아!")
                ),
                ""
        );

        List<ReportResponse.EmotionTimelinePoint> timeline = emotionTimelineAnalysisService.analyze(context);

        assertThat(timeline).isNotEmpty();
        assertThat(timeline.get(0).score()).isBetween(0, 100);
        assertThat(timeline.get(timeline.size() - 1).score()).isGreaterThan(timeline.get(0).score());
    }
}
