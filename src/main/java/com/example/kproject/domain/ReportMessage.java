package com.example.kproject.domain;

import java.time.LocalDateTime;

public record ReportMessage(
        String sender,
        LocalDateTime dateTime,
        String content
) {
}
