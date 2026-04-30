package com.example.kproject.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "채팅", description = "OpenAI 채팅 테스트 API")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final OpenAiChatService openAiChatService;

    public ChatController(OpenAiChatService openAiChatService) {
        this.openAiChatService = openAiChatService;
    }

    @Operation(
            summary = "프롬프트를 OpenAI에 전송",
            description = "설정된 OpenAI 모델이 생성한 단일 응답 메시지를 반환합니다."
    )
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(openAiChatService.reply(request.message()));
    }
}
