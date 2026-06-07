package com.example.kproject.service.normalize;

import com.example.kproject.domain.NormalizedConversationResult;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.service.ai.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OpenAiConversationNormalizeService implements AiConversationNormalizeService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiConversationNormalizeService.class);

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiConversationNormalizeService(
            RestClient openAiRestClient,
            OpenAiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.openAiRestClient = openAiRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<NormalizedConversationResult> normalizeText(String rawText, String targetName, String myName) {
        if (!StringUtils.hasText(rawText) || !hasApiKey()) {
            return Optional.empty();
        }

        return requestNormalizedConversation(List.of(
                Map.of("type", "input_text", "text", normalizationPrompt(targetName, myName)),
                Map.of("type", "input_text", "text", rawText)
        ));
    }

    @Override
    public Optional<NormalizedConversationResult> normalizeImage(MultipartFile file, String targetName, String myName) {
        if (file == null || file.isEmpty() || !hasApiKey()) {
            return Optional.empty();
        }

        try {
            String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : MediaType.IMAGE_PNG_VALUE;
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String dataUrl = "data:%s;base64,%s".formatted(contentType, base64);

            return requestNormalizedConversation(List.of(
                    Map.of("type", "input_text", "text", normalizationPrompt(targetName, myName)),
                    Map.of("type", "input_image", "image_url", dataUrl)
            ));
        } catch (IOException exception) {
            log.warn("Failed to read image for OpenAI normalization.", exception);
            return Optional.empty();
        }
    }

    @Override
    public Optional<NormalizedConversationResult> normalizeFiles(
            List<MultipartFile> files,
            String targetName,
            String myName,
            String description
    ) {
        if (files == null || files.isEmpty() || !hasApiKey()) {
            return Optional.empty();
        }

        try {
            List<Map<String, String>> content = new ArrayList<>();
            content.add(Map.of("type", "input_text", "text", normalizationPrompt(targetName, myName, description)));
            for (int index = 0; index < files.size(); index++) {
                MultipartFile file = files.get(index);
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String filename = StringUtils.hasText(file.getOriginalFilename())
                        ? file.getOriginalFilename().trim()
                        : "upload-" + (index + 1);
                String marker = "[upload %d/%d: %s]".formatted(index + 1, files.size(), filename);
                if (isImage(file)) {
                    String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : MediaType.IMAGE_PNG_VALUE;
                    String base64 = Base64.getEncoder().encodeToString(file.getBytes());
                    content.add(Map.of("type", "input_text", "text", marker + " 같은 카카오톡 대화의 순서 있는 캡처입니다. 위에서 아래로 읽으세요."));
                    content.add(Map.of("type", "input_image", "image_url", "data:%s;base64,%s".formatted(contentType, base64)));
                } else {
                    content.add(Map.of(
                            "type", "input_text",
                            "text", marker + "\n" + new String(file.getBytes(), StandardCharsets.UTF_8)
                    ));
                }
            }
            return requestNormalizedConversation(content);
        } catch (IOException exception) {
            log.warn("Failed to read files for OpenAI normalization.", exception);
            return Optional.empty();
        }
    }

    private Optional<NormalizedConversationResult> requestNormalizedConversation(List<Map<String, String>> content) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", properties.model(),
                    "input", List.of(Map.of(
                            "role", "user",
                            "content", content
                    )),
                    "text", Map.of("format", responseFormat())
            );

            JsonNode response = openAiRestClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String outputText = extractOutputText(response);
            if (!StringUtils.hasText(outputText)) {
                return Optional.empty();
            }

            NormalizedConversationDto conversation = objectMapper.readValue(
                    outputText.getBytes(StandardCharsets.UTF_8),
                    NormalizedConversationDto.class
            );

            if (conversation.messages() == null || conversation.messages().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new NormalizedConversationResult(
                    sanitize(conversation),
                    ReportAnalysisMode.STRUCTURED,
                    true,
                    "OpenAI를 사용해 비정형 원본을 정형화했습니다."
            ));
        } catch (Exception exception) {
            log.warn("OpenAI conversation normalization failed. Falling back to local normalization.", exception);
            return Optional.empty();
        }
    }

    private boolean hasApiKey() {
        return StringUtils.hasText(properties.apiKey());
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return null;
        }

        String outputText = response.path("output_text").asString();
        if (StringUtils.hasText(outputText)) {
            return outputText.trim();
        }

        JsonNode output = response.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    String text = contentItem.path("text").asString();
                    if (StringUtils.hasText(text)) {
                        return text.trim();
                    }
                }
            }
        }

        return null;
    }

    private NormalizedConversationDto sanitize(NormalizedConversationDto conversation) {
        List<String> participants = conversation.participants() == null ? List.of() : conversation.participants().stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        List<NormalizedConversationDto.MessageDto> messages = conversation.messages().stream()
                .filter(message -> message != null && StringUtils.hasText(message.content()))
                .map(message -> new NormalizedConversationDto.MessageDto(
                        StringUtils.hasText(message.sender()) ? message.sender().trim() : "상대방",
                        StringUtils.hasText(message.timestamp()) ? message.timestamp().trim() : null,
                        message.content().trim(),
                        StringUtils.hasText(message.type()) ? message.type().trim() : "TEXT"
                ))
                .toList();
        List<String> keywords = conversation.keywords() == null ? List.of() : conversation.keywords().stream()
                .filter(StringUtils::hasText)
                .distinct()
                .limit(20)
                .toList();

        return new NormalizedConversationDto(
                participants,
                messages,
                keywords,
                conversation.rawText() == null ? "" : conversation.rawText()
        );
    }

    private String normalizationPrompt(String targetName) {
        return normalizationPrompt(targetName, null, null);
    }

    private String normalizationPrompt(String targetName, String myName) {
        return normalizationPrompt(targetName, myName, null);
    }

    private String normalizationPrompt(String targetName, String myName, String description) {
        String targetInstruction = StringUtils.hasText(targetName)
                ? "분석 대상 또는 상대방 이름 힌트: " + targetName.trim()
                : "분석 대상 이름 힌트는 제공되지 않았습니다.";
        String myNameInstruction = StringUtils.hasText(myName)
                ? "사용자 본인 이름 힌트: " + myName.trim()
                : "사용자 본인 이름 힌트는 제공되지 않았습니다.";
        String requestInstruction = StringUtils.hasText(description)
                ? "사용자가 특히 궁금해하는 분석 요청: " + description.trim()
                : "사용자의 추가 분석 요청은 제공되지 않았습니다.";

        return """
                너는 비정형 대화 원본을 정형화하는 엔진이다.
                입력은 카카오톡 txt, 복사된 대화, OCR이 필요한 여러 장의 채팅 캡처 이미지 등 어떤 형식일 수 있다.

                목표:
                - 원본에서 실제 대화 메시지를 최대한 추출한다.
                - 여러 캡처가 들어오면 첨부 순서대로 읽고, 화면이 겹쳐 생기는 중복 메시지는 한 번만 남긴다.
                - txt와 이미지가 함께 들어오면 같은 대화를 보강하는 자료로 보고 충돌하지 않는 메시지를 합친다.
                - 광고, 시스템 문구, 저장 날짜, 날짜 구분선은 메시지로 넣지 않는다.
                - 발신자를 가능한 한 추정한다.
                - 시간이 있으면 ISO-8601 LocalDateTime 형식으로 timestamp를 넣는다.
                - 시간이 없으면 timestamp는 null로 둔다.
                - 사진/이모티콘/파일/삭제 메시지는 type을 IMAGE, EMOTICON, FILE, DELETED로 분류한다.
                - 일반 메시지는 TEXT로 분류한다.
                - 결과는 반드시 지정된 JSON 스키마만 따른다.
                - 성격/관계 분석은 하지 말고 정형화만 한다.
                - participants는 가능하면 [사용자 본인, 분석 대상/상대방] 순서로 반환한다.
                - sender는 원문에 나온 표시명을 유지하되, 이름 힌트와 일치하는 발신자를 정확히 구분한다.

                %s
                %s
                %s
                """.formatted(targetInstruction, myNameInstruction, requestInstruction);
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
            return true;
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp");
    }

    private Map<String, Object> responseFormat() {
        return Map.of(
                "type", "json_schema",
                "name", "normalized_conversation",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("participants", "messages", "keywords", "rawText"),
                        "properties", Map.of(
                                "participants", Map.of(
                                        "type", "array",
                                        "items", Map.of("type", "string")
                                ),
                                "messages", Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "object",
                                                "additionalProperties", false,
                                                "required", List.of("sender", "timestamp", "content", "type"),
                                                "properties", Map.of(
                                                        "sender", Map.of("type", "string"),
                                                        "timestamp", Map.of("type", List.of("string", "null")),
                                                        "content", Map.of("type", "string"),
                                                        "type", Map.of(
                                                                "type", "string",
                                                                "enum", List.of("TEXT", "IMAGE", "EMOTICON", "FILE", "DELETED")
                                                        )
                                                )
                                        )
                                ),
                                "keywords", Map.of(
                                        "type", "array",
                                        "items", Map.of("type", "string")
                                ),
                                "rawText", Map.of("type", "string")
                        )
                )
        );
    }
}
