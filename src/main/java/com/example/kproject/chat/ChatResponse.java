package com.example.kproject.chat;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatResponse(
        @Schema(description = "모델이 생성한 응답 메시지", example = "안녕하세요. 간단한 질문에 답변해드릴 수 있어요.")
        String reply
) {
}
