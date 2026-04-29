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
                ),
                "response_format", Map.of("type", "json_object") // ✨ JSON 강제 출력 설정 추가
        );
        return callOpenAi(requestBody, userMessage);
    }

    // [2] 이미지 + 텍스트 질문 상담 (리포트용)
    public String replyWithImage(MultipartFile imageFile, String userMessage) throws IOException {
        byte[] bytes = imageFile.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(bytes);

        // 질문이 없으면 기본 분석 요청 멘트 사용
        String finalUserMessage = StringUtils.hasText(userMessage) ? userMessage : "이 카톡 캡처 사진 보고 종합 분석 리포트 데이터 뽑아줘.";

        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "developer", "content", getCommonPrompt()),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", finalUserMessage),
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:image/jpeg;base64," + base64Image))
                        ))
                ),
                "response_format", Map.of("type", "json_object") // ✨ JSON 강제 출력 설정 추가
        );

        return callOpenAi(requestBody, "이미지 리포트 분석: " + finalUserMessage);
    }

    // ✨ 프론트 디자인 스펙에 맞춘 JSON 프롬프트 설정
    private String getCommonPrompt() {
        return "너는 연애 심리 분석 앱 '마인드컨트롤'의 전문 상담사야. 사용자의 카톡 대화를 분석하여 반드시 다음 JSON 형식으로만 응답해.\n" +
                "[응답 규칙]\n" +
                "1. 모든 텍스트는 한국어로 작성.\n" +
                "2. 반드시 유효한 JSON 객체 하나만 반환 (마크다운 ```json 태그 포함 금지).\n" +
                "\n" +
                "[JSON 구조]\n" +
                "{\n" +
                "  \"overall_report\": { \"likability_index\": 0-100, \"summary_ment\": \"문자열\" },\n" +
                "  \"relationship_dynamics\": {\n" +
                "    \"chat_share\": { \"user\": 0-100, \"partner\": 0-100 },\n" +
                "    \"avg_reply_time_minutes\": 정수,\n" +
                "    \"language_sync_percent\": 0-100,\n" +
                "    \"keywords\": [\"#키워드1\", \"#키워드2\", \"#키워드3\", \"#키워드4\"]\n" +
                "  },\n" +
                "  \"personality_analysis\": {\n" +
                "    \"predicted_mbti\": \"MBTI타입\",\n" +
                "    \"attachment_type\": \"안정형/불안형/회피형 중 선택\",\n" +
                "    \"big_five_scores\": { \"extraversion\": 1-100, \"conscientiousness\": 1-100, \"agreeableness\": 1-100, \"neuroticism\": 1-100, \"openness\": 1-100 }\n" +
                "  },\n" +
                "  \"emotional_timeline\": [\n" +
                "    { \"time_point\": \"초반\", \"emotion_score\": 1-100 },\n" +
                "    { \"time_point\": \"중반\", \"emotion_score\": 1-100 },\n" +
                "    { \"time_point\": \"후반\", \"emotion_score\": 1-100 }\n" +
                "  ],\n" +
                "  \"insights\": {\n" +
                "    \"decisive_moment\": \"결정적 순간 설명 문자열\",\n" +
                "    \"dating_tip\": \"실전 솔루션 문자열\",\n" +
                "    \"caution_point\": \"주의할 점 문자열\"\n" +
                "  }\n" +
                "}";
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

        // ✨ asString() 대신 asText()를 사용해야 JSON 문자열을 정확히 가져옵니다.
        String aiAnswer = response.path("choices").get(0).path("message").path("content").asText();

        if (StringUtils.hasText(aiAnswer)) {
            aiAnswer = aiAnswer.trim();
            chatRepository.save(new ChatHistory(logMessage, aiAnswer));
            return aiAnswer;
        }
        return "답변을 생성하지 못했습니다.";
    }
}