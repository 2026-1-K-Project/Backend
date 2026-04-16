package com.example.kproject.chat;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

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
        // [1] 프롬프트 수정: 각 항목 뒤에 줄바꿈을 넣으라고 명시함
        String developerPrompt =
                "너는 연애 심리 분석 앱 '마인드컨트롤'의 독설 고수 상담사야.\n\n" +
                        "[답변 규칙]\n" +
                        "1. 역질문 절대 금지: 사용자가 답해야 하는 질문은 하지 마.\n" +
                        "2. 3단 구조 고정: 반드시 아래 형식으로만 대답하고, 항목 사이에는 무조건 줄바꿈을 넣어.\n" +
                        "💬 상황판단: [내용]\n" +
                        "⚡ 실전솔루션: [내용]\n" +
                        "💯 관계점수: [점수]\n" +
                        "3. 말투: 친한 친구에게 말하는 반말. 'ㅋㅋ', 'ㅠ' 사용.\n" +
                        "4. 제약 사항: 군더더기 없이 딱 위 세 줄로만 대답해.";

        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "input", List.of(
                        Map.of("role", "system", "content", List.of(Map.of("type", "input_text", "text", "너는 마인드컨트롤 서비스의 메인 AI야."))),
                        Map.of("role", "developer", "content", List.of(Map.of("type", "input_text", "text", developerPrompt))),
                        Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", userMessage)))
                )
        );

        JsonNode response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) throw new RestClientException("Response is empty");

        String aiAnswer = response.path("output_text").asString();

        if (!StringUtils.hasText(aiAnswer)) {
            JsonNode outputNode = response.path("output");
            if (outputNode.isArray() && outputNode.has(0)) {
                aiAnswer = outputNode.get(0).path("content").get(0).path("text").asString();
            }
        }

        // ✨ [핵심 수정] 줄바꿈 제거 로직을 삭제했습니다!
        // 이제 AI가 보낸 \n이 그대로 전달되어 포스트맨에서 3줄로 보일 거예요.
        if (StringUtils.hasText(aiAnswer)) {
            aiAnswer = aiAnswer.trim();
        }

        if (StringUtils.hasText(aiAnswer)) {
            chatRepository.save(new ChatHistory(userMessage, aiAnswer));
            return aiAnswer;
        }

        return "답변을 찾지 못했습니다.";
    }
}