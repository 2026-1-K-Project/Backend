package com.example.kproject.chat;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value; // 이거 꼭 확인

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

    public String reply(String userMessage) {
        // OpenAI 호출 로직
        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "input", List.of(Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", userMessage))))
        );

        JsonNode response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) throw new RestClientException("Response is empty");

        // 답변 추출
        String aiAnswer = response.path("output_text").asString();

        // [중요] 답변이 비어있으면 다른 경로에서 찾기
        if (!StringUtils.hasText(aiAnswer)) {
            aiAnswer = response.path("output").get(0).path("content").get(0).path("text").asString();
        }

        // [핵심] DB 저장 후 리턴
        if (StringUtils.hasText(aiAnswer)) {
            chatRepository.save(new ChatHistory(userMessage, aiAnswer));
            return aiAnswer; // 이 값이 포스트맨에 뜹니다!
        }

        return "답변을 찾지 못했습니다.";
    }
}