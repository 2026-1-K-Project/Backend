package com.example.kproject.service.analysis;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReplyTimeAnalysisService {

    public ReplyTimeAnalysisResult calculate(ReportAnalysisContext context) {
        List<Integer> allReplyMinutes = new ArrayList<>();
        List<Integer> meReplyMinutes = new ArrayList<>();
        List<Integer> otherReplyMinutes = new ArrayList<>();

        List<ReportMessage> messages = context.messages();
        for (int index = 1; index < messages.size(); index++) {
            ReportMessage previous = messages.get(index - 1);
            ReportMessage current = messages.get(index);

            if (previous.sender().equals(current.sender())) {
                continue;
            }

            int minutes = Math.max(0, (int) java.time.Duration.between(previous.dateTime(), current.dateTime()).toMinutes());
            allReplyMinutes.add(minutes);

            if (context.isMe(current.sender())) {
                meReplyMinutes.add(minutes);
            } else {
                otherReplyMinutes.add(minutes);
            }
        }

        return new ReplyTimeAnalysisResult(
                average(allReplyMinutes),
                average(meReplyMinutes),
                average(otherReplyMinutes)
        );
    }

    private int average(List<Integer> values) {
        if (values.isEmpty()) {
            return 0;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    public record ReplyTimeAnalysisResult(
            int averageReplyMinutes,
            int meReplyMinutes,
            int otherReplyMinutes
    ) {
    }
}
