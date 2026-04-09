package com.example.kproject.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @Schema(description = "User message sent to the model", example = "안녕, 한 줄로 자기소개 해줘")
        @NotBlank(message = "message is required")
        String message
) {
}
