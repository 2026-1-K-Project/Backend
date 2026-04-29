package com.example.kproject.chat;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiChatService {

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ChatRepository chatRepository;

    public OpenAiChatService(RestClient openAiRestClient, OpenAiProperties properties, ChatRepository chatRepository) {
        this.restClient = openAiRestClient;
        this.properties = properties;
        this.chatRepository = chatRepository;
    }

    // [1] 기존 텍스트 전용 상담
    public String reply(String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "developer", "content", getCommonPrompt()),
                        Map.of("role", "user", "content", userMessage)
                )
        );
        return callOpenAi(requestBody, userMessage);
    }

    // [2] 이미지 + 텍스트 질문 상담 (핵심 수정 부분)
    public String replyWithImage(MultipartFile imageFile, String userMessage) throws IOException {
        byte[] bytes = imageFile.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(bytes);

        // 질문이 없으면 기본 분석 요청 멘트 사용
        String finalUserMessage = StringUtils.hasText(userMessage) ? userMessage : "이 카톡 캡처 사진 보고 상황 분석해줘.";

        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "developer", "content", getCommonPrompt()),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", finalUserMessage),
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:image/jpeg;base64," + base64Image))
                        ))
                )
        );

        return callOpenAi(requestBody, "이미지 분석 요청: " + finalUserMessage);
    }

    // 공통 프롬프트 설정
    private String getCommonPrompt() {
        return "너는 연애 심리 분석 앱 '마인드컨트롤'의 독설 고수 상담사야.\n" +
                "[답변 규칙]\n" +
                "1. 역질문 절대 금지.\n" +
                "2. 3단 구조 고정: 반드시 아래 형식으로만 대답하고 항목 사이 줄바꿈 필수.\n" +
                "💬 상황판단: [내용]\n" +
                "⚡ 실전솔루션: [내용]\n" +
                "💯 관계점수: [점수]\n" +
                "3. 말투: 친한 친구에게 말하는 반말. 'ㅋㅋ', 'ㅠ' 사용.\n" +
                "4. 제약 사항: 군더더기 없이 딱 위 세 줄로만 대답해.";
    }

    // OpenAI API 호출 공통 로직
    private String callOpenAi(Map<String, Object> requestBody, String logMessage) {
        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) throw new RestClientException("Response is empty");

        String aiAnswer = response.path("choices").get(0).path("message").path("content").asString();

        if (StringUtils.hasText(aiAnswer)) {
            aiAnswer = aiAnswer.trim();
            chatRepository.save(new ChatHistory(logMessage, aiAnswer));
            return aiAnswer;
        }
        return "답변을 생성하지 못했습니다.";
    }
}