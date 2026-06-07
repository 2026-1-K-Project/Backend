package com.example.kproject.service.ai;

import com.example.kproject.domain.ReportAnalysisContext;
import com.example.kproject.domain.ReportMessage;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.AiDeepAnalysisResponse;
import com.example.kproject.dto.report.ReportInsightsResponse;
import com.example.kproject.dto.report.ReportPersonalityResponse;
import com.example.kproject.dto.report.ReportRelationshipResponse;
import com.example.kproject.dto.report.ReportSummaryResponse;
import com.example.kproject.util.ReportTextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OpenAiDeepAnalysisService implements AiDeepAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiDeepAnalysisService.class);

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiDeepAnalysisService(
            RestClient openAiRestClient,
            OpenAiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.openAiRestClient = openAiRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<AiDeepAnalysisResponse> analyze(
            ReportAnalysisContext context,
            NormalizedConversationDto normalized,
            ReportSummaryResponse summary,
            ReportRelationshipResponse relationship,
            ReportPersonalityResponse personality,
            ReportInsightsResponse insights,
            String userRequest
    ) {
        if (!StringUtils.hasText(properties.apiKey())) {
            return Optional.empty();
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", properties.model(),
                    "input", List.of(Map.of(
                            "role", "user",
                            "content", List.of(Map.of(
                                    "type", "input_text",
                                    "text", prompt(context, normalized, summary, relationship, personality, insights, userRequest)
                            ))
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

            AiDeepAnalysisResponse analysis = objectMapper.readValue(
                    outputText.getBytes(StandardCharsets.UTF_8),
                    AiDeepAnalysisResponse.class
            );
            return Optional.of(sanitize(analysis));
        } catch (Exception exception) {
            log.warn("OpenAI deep report analysis failed. Falling back to deterministic analysis.", exception);
            return Optional.empty();
        }
    }

    private String prompt(
            ReportAnalysisContext context,
            NormalizedConversationDto normalized,
            ReportSummaryResponse summary,
            ReportRelationshipResponse relationship,
            ReportPersonalityResponse personality,
            ReportInsightsResponse insights,
            String userRequest
    ) {
        String messages = sampleMessages(context, normalized);
        return """
                너는 카카오톡 대화 기반 관계 분석 리포트를 작성하는 분석가다.
                반드시 제공된 실제 대화 내용과 계산 지표만 근거로 판단한다.

                중요 규칙:
                - quote 필드는 반드시 아래 실제 대화 내용에 존재하는 문장 일부만 사용한다.
                - 실제 문장 근거가 없으면 quote를 빈 문자열로 둔다.
                - "무조건 좋아한다", "확실하다"처럼 단정하지 말고 가능성/신호로 표현한다.
                - positiveSignals와 riskSignals를 분리한다.
                - requestAnswer는 사용자 요청에 대한 직접 답변으로 작성한다.
                - requestReason은 그 답변을 내린 핵심 이유를 실제 대화 흐름에 근거해 작성한다.
                - requestEvidence는 사용자 요청과 직접 관련 있는 실제 대화 근거를 1~4개로 요약한다.
                - nextActions는 바로 보낼 수 있는 자연스러운 한국어 멘트로 작성한다.
                - avoidMessages는 지금 단계에서 피해야 할 부담스러운 표현과 이유를 작성한다.
                - 개인정보 노출을 줄이기 위해 quote는 80자 이하로 짧게 자른다.
                - 결과는 지정된 JSON 스키마만 따른다.

                사용자 요청:
                %s

                계산 지표:
                - 카테고리: %s
                - 관계 지수: %d
                - 대화 비율: 나 %d%% / 상대 %d%%
                - 평균 답장 속도: %d분
                - 대화 싱크: %d
                - 키워드: %s
                - 요약: %s
                - MBTI 경향: %s
                - 애착 유형: %s
                - 기존 추천 질문: %s
                - 기존 추천 답장: %s
                - 기존 경고: %s

                실제 대화 내용:
                %s
                """.formatted(
                StringUtils.hasText(userRequest) ? userRequest : "추가 요청 없음",
                context.category(),
                relationship.interestScore(),
                relationship.talkRatio() == null ? 50 : relationship.talkRatio().me(),
                relationship.talkRatio() == null ? 50 : relationship.talkRatio().other(),
                relationship.averageReplyMinutes(),
                relationship.languageSync(),
                safeJoin(summary.keywords()),
                summary.headline(),
                personality.mbti() == null ? "분석 중" : personality.mbti().type(),
                personality.attachmentType() == null ? "분석 중" : personality.attachmentType().type(),
                safeJoin(insights.recommendedQuestions()),
                safeJoin(insights.recommendedReplies()),
                safeJoin(insights.warnings()),
                messages
        );
    }

    private String sampleMessages(ReportAnalysisContext context, NormalizedConversationDto normalized) {
        if (!context.messages().isEmpty()) {
            return context.messages().stream()
                    .filter(message -> ReportTextUtils.hasText(message.content()))
                    .limit(120)
                    .map(this::formatMessage)
                    .reduce((first, second) -> first + "\n" + second)
                    .orElse("");
        }
        if (StringUtils.hasText(normalized.rawText())) {
            return normalized.rawText().lines()
                    .filter(ReportTextUtils::hasText)
                    .limit(120)
                    .reduce((first, second) -> first + "\n" + second)
                    .orElse("");
        }
        return "";
    }

    private String formatMessage(ReportMessage message) {
        return message.sender() + ": " + ReportTextUtils.safeText(message.content());
    }

    private String safeJoin(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(", ", values.stream().filter(StringUtils::hasText).limit(8).toList());
    }

    private AiDeepAnalysisResponse sanitize(AiDeepAnalysisResponse analysis) {
        return new AiDeepAnalysisResponse(
                safeText(analysis.verdict(), "분석 결과"),
                clamp(analysis.confidence()),
                safeText(analysis.relationshipStage(), "관계 탐색 단계"),
                safeText(analysis.oneLineSummary(), "대화 흐름을 바탕으로 관계 신호를 분석했습니다."),
                safeText(analysis.requestAnswer(), "요청한 내용은 대화 근거가 더 필요해 단정하기 어렵습니다."),
                safeText(analysis.requestReason(), "대화량과 반응 흐름을 함께 보면 가능성은 있으나 추가 확인이 필요합니다."),
                analysis.requestEvidence() == null ? List.of() : analysis.requestEvidence().stream()
                        .filter(StringUtils::hasText)
                        .limit(4)
                        .toList(),
                analysis.positiveSignals() == null ? List.of() : analysis.positiveSignals().stream().limit(5).toList(),
                analysis.riskSignals() == null ? List.of() : analysis.riskSignals().stream().limit(5).toList(),
                safeText(analysis.counterpartyStyle(), ""),
                safeText(analysis.userPattern(), ""),
                analysis.nextActions() == null ? List.of() : analysis.nextActions().stream().limit(4).toList(),
                analysis.avoidMessages() == null ? List.of() : analysis.avoidMessages().stream().limit(4).toList()
        );
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int clamp(int value) {
        return ReportTextUtils.clamp(value, 0, 100);
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

    private Map<String, Object> responseFormat() {
        Map<String, Object> signal = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("type", "label", "quote", "reason", "strength"),
                "properties", Map.of(
                        "type", Map.of("type", "string", "enum", List.of("POSITIVE", "RISK")),
                        "label", Map.of("type", "string"),
                        "quote", Map.of("type", "string"),
                        "reason", Map.of("type", "string"),
                        "strength", Map.of("type", "integer", "minimum", 0, "maximum", 100)
                )
        );
        Map<String, Object> nextAction = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("title", "message", "why"),
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "message", Map.of("type", "string"),
                        "why", Map.of("type", "string")
                )
        );
        Map<String, Object> avoidMessage = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("message", "why"),
                "properties", Map.of(
                        "message", Map.of("type", "string"),
                        "why", Map.of("type", "string")
                )
        );

        return Map.of(
                "type", "json_schema",
                "name", "ai_deep_relationship_analysis",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of(
                                "verdict",
                                "confidence",
                                "relationshipStage",
                                "oneLineSummary",
                                "requestAnswer",
                                "requestReason",
                                "requestEvidence",
                                "positiveSignals",
                                "riskSignals",
                                "counterpartyStyle",
                                "userPattern",
                                "nextActions",
                                "avoidMessages"
                        ),
                        "properties", Map.ofEntries(
                                Map.entry("verdict", Map.of("type", "string")),
                                Map.entry("confidence", Map.of("type", "integer", "minimum", 0, "maximum", 100)),
                                Map.entry("relationshipStage", Map.of("type", "string")),
                                Map.entry("oneLineSummary", Map.of("type", "string")),
                                Map.entry("requestAnswer", Map.of("type", "string")),
                                Map.entry("requestReason", Map.of("type", "string")),
                                Map.entry("requestEvidence", Map.of("type", "array", "items", Map.of("type", "string"))),
                                Map.entry("positiveSignals", Map.of("type", "array", "items", signal)),
                                Map.entry("riskSignals", Map.of("type", "array", "items", signal)),
                                Map.entry("counterpartyStyle", Map.of("type", "string")),
                                Map.entry("userPattern", Map.of("type", "string")),
                                Map.entry("nextActions", Map.of("type", "array", "items", nextAction)),
                                Map.entry("avoidMessages", Map.of("type", "array", "items", avoidMessage))
                        )
                )
        );
    }
}
