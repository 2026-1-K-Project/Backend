package com.example.kproject.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Chat", description = "마인드컨트롤 채팅 및 이미지 분석 API")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final OpenAiChatService openAiChatService;

    public ChatController(OpenAiChatService openAiChatService) {
        this.openAiChatService = openAiChatService;
    }

    @Operation(summary = "텍스트 상담", description = "질문을 보내면 AI 상담사가 답변을 반환합니다.")
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(openAiChatService.reply(request.message()));
    }

    @Operation(summary = "카톡 캡처 분석", description = "이미지 파일과 추가 질문을 보내면 AI가 분석해 답변을 반환합니다.")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse chatWithImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "message", required = false) String message
    ) throws IOException {
        // 이미지와 메시지를 함께 서비스로 전달합니다.
        return new ChatResponse(openAiChatService.replyWithImage(image, message));
    }
}