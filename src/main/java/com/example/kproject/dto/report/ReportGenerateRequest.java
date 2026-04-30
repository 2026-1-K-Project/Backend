package com.example.kproject.dto.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record ReportGenerateRequest(
        @NotBlank(message = "category is required")
        String category,
        @NotEmpty(message = "participants are required")
        List<@NotBlank(message = "participant name is required") String> participants,
        @NotEmpty(message = "messages are required")
        List<@Valid MessageDto> messages,
        String analysisText
) {
    public record MessageDto(
            @NotBlank(message = "sender is required")
            String sender,
            @NotNull(message = "dateTime is required")
            LocalDateTime dateTime,
            String content
    ) {
    }
}
