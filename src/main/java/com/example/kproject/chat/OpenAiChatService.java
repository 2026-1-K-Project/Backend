package com.example.kproject.chat;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiChatService {

    private final RestClient restClient;
    private final OpenAiProperties properties;

    public OpenAiChatService(RestClient openAiRestClient, OpenAiProperties properties) {
        this.restClient = openAiRestClient;
        this.properties = properties;
    }

    public String reply(String userMessage) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }

        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "input", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "input_text",
                                                "text", userMessage
                                        )
                                )
                        )
                )
        );

        JsonNode response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new RestClientException("OpenAI response body is empty");
        }

        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual() && StringUtils.hasText(outputText.asText())) {
            return outputText.asText();
        }

        JsonNode outputs = response.path("output");
        for (JsonNode output : outputs) {
            if (!"message".equals(output.path("type").asText())) {
                continue;
            }

            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    String text = content.path("text").asText();
                    if (StringUtils.hasText(text)) {
                        return text;
                    }
                }
            }
        }

        throw new RestClientException("OpenAI response did not contain output text");
    }
}
