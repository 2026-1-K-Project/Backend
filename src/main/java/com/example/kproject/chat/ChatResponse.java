package com.example.kproject.chat;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatResponse(
        @Schema(description = "Model reply text", example = "안녕하세요. 간단한 질문에 답해드릴 수 있어요.")
        String reply
) {
}
