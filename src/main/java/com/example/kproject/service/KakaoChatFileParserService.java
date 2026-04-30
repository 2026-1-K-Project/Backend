package com.example.kproject.service;

import com.example.kproject.domain.KakaoChatParsedDocument;
import com.example.kproject.domain.KakaoChatParsingStats;
import com.example.kproject.dto.KakaoChatMessageDto;
import com.example.kproject.dto.KakaoChatMetaDto;
import com.example.kproject.dto.KakaoChatParseErrorDto;
import com.example.kproject.dto.KakaoChatSpecialType;
import com.example.kproject.dto.KakaoChatUploadResponse;
import com.example.kproject.exception.ChatUploadException;
import com.example.kproject.util.KakaoChatParsingUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class KakaoChatFileParserService {

    public KakaoChatUploadResponse parse(MultipartFile file) {
        return parseDocument(file).toUploadResponse();
    }

    public KakaoChatParsedDocument parseDocument(MultipartFile file) {
        validateFile(file);

        RawTextPayload rawTextPayload = readRawText(file);
        if (!StringUtils.hasText(rawTextPayload.rawText())) {
            throw new ChatUploadException("Uploaded file is empty.");
        }

        List<KakaoChatParseErrorDto> errors = new ArrayList<>();
        KakaoChatMetaDto meta = extractMeta(rawTextPayload.lines());
        ParseComputation parseComputation = parseMessages(rawTextPayload.lines(), meta.savedAt(), errors);
        String analysisText = buildAnalysisText(parseComputation.messages());

        return new KakaoChatParsedDocument(
                meta,
                rawTextPayload.rawText(),
                List.copyOf(rawTextPayload.lines()),
                List.copyOf(parseComputation.messages()),
                analysisText,
                List.copyOf(errors),
                parseComputation.stats()
        );
    }

    public String buildAnalysisText(List<KakaoChatMessageDto> messages) {
        return messages.stream()
                .filter(message -> message.specialType() == KakaoChatSpecialType.TEXT)
                .filter(message -> StringUtils.hasText(message.dateTime()))
                .sorted(Comparator.comparing(message -> LocalDateTime.parse(message.dateTime())))
                .map(message -> new AnalysisLine(
                        message.sender(),
                        KakaoChatParsingUtils.normalizeForAnalysis(message.content())
                ))
                .filter(line -> StringUtils.hasText(line.content()))
                .map(line -> "%s: %s".formatted(line.sender(), line.content()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ChatUploadException("A KakaoTalk txt file is required.");
        }
    }

    private RawTextPayload readRawText(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            List<String> lines = new ArrayList<>();
            StringBuilder rawTextBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (rawTextBuilder.length() > 0) {
                    rawTextBuilder.append('\n');
                }
                rawTextBuilder.append(line);
            }

            return new RawTextPayload(rawTextBuilder.toString(), lines);
        } catch (IOException exception) {
            throw new ChatUploadException("Failed to read the uploaded txt file.", exception);
        }
    }

    private KakaoChatMetaDto extractMeta(List<String> lines) {
        String title = lines.stream()
                .map(KakaoChatParsingUtils::stripBom)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);

        String savedAt = lines.stream()
                .map(KakaoChatParsingUtils::extractSavedAt)
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(null);

        return new KakaoChatMetaDto(
                title,
                savedAt,
                KakaoChatParsingUtils.deriveRoomName(title),
                null,
                null
        );
    }

    private ParseComputation parseMessages(
            List<String> lines,
            String savedAt,
            List<KakaoChatParseErrorDto> errors
    ) {
        List<KakaoChatMessageDto> messages = new ArrayList<>();
        Optional<LocalDate> savedAtDate = KakaoChatParsingUtils.extractSavedAtDate(savedAt);
        LocalDate currentDate = savedAtDate.orElse(null);
        boolean usedSavedAtDateFallback = false;
        WorkingMessage currentMessage = null;

        int totalRelevantLines = 0;
        int parsedRelevantLines = 0;
        int messageStartCount = 0;
        int continuationLineCount = 0;
        int dateSeparatorCount = 0;
        int unmatchedLineCount = 0;

        for (int index = 0; index < lines.size(); index++) {
            String rawLine = KakaoChatParsingUtils.stripBom(lines.get(index));

            if (index == 0 && KakaoChatParsingUtils.looksLikeConversationTitle(rawLine)) {
                continue;
            }
            if (KakaoChatParsingUtils.isSavedAtLine(rawLine)) {
                continue;
            }

            if (!StringUtils.hasText(rawLine)) {
                if (currentMessage != null) {
                    currentMessage.append("");
                    continuationLineCount++;
                    parsedRelevantLines++;
                }
                continue;
            }

            totalRelevantLines++;

            Optional<LocalDate> dateSeparator = KakaoChatParsingUtils.parseDateSeparator(rawLine);
            if (dateSeparator.isPresent()) {
                if (currentMessage != null) {
                    messages.add(currentMessage.toDto());
                    currentMessage = null;
                }
                currentDate = dateSeparator.get();
                dateSeparatorCount++;
                parsedRelevantLines++;
                continue;
            }

            Optional<KakaoChatParsingUtils.ParsedMessageStart> messageStart =
                    KakaoChatParsingUtils.parseMessageStart(rawLine);
            if (messageStart.isPresent()) {
                if (currentMessage != null) {
                    messages.add(currentMessage.toDto());
                }

                LocalDate resolvedDate = currentDate;
                if (resolvedDate == null && savedAtDate.isPresent()) {
                    resolvedDate = savedAtDate.get();
                    usedSavedAtDateFallback = true;
                }

                if (resolvedDate == null) {
                    errors.add(new KakaoChatParseErrorDto(
                            index + 1,
                            rawLine,
                            "Could not infer a date for this message."
                    ));
                    unmatchedLineCount++;
                    continue;
                }

                try {
                    currentMessage = WorkingMessage.start(resolvedDate, messageStart.get());
                    messageStartCount++;
                    parsedRelevantLines++;
                    continue;
                } catch (IllegalArgumentException exception) {
                    errors.add(new KakaoChatParseErrorDto(
                            index + 1,
                            rawLine,
                            "Could not parse the message time."
                    ));
                    currentMessage = null;
                    unmatchedLineCount++;
                    continue;
                }
            }

            if (currentMessage != null) {
                currentMessage.append(rawLine);
                continuationLineCount++;
                parsedRelevantLines++;
            } else {
                errors.add(new KakaoChatParseErrorDto(
                        index + 1,
                        rawLine,
                        "This line could not be attached to a structured message."
                ));
                unmatchedLineCount++;
            }
        }

        if (currentMessage != null) {
            messages.add(currentMessage.toDto());
        }

        double successRate = totalRelevantLines == 0
                ? 0.0
                : parsedRelevantLines / (double) totalRelevantLines;

        KakaoChatParsingStats stats = new KakaoChatParsingStats(
                totalRelevantLines,
                parsedRelevantLines,
                messageStartCount,
                continuationLineCount,
                dateSeparatorCount,
                unmatchedLineCount,
                successRate,
                usedSavedAtDateFallback
        );

        return new ParseComputation(messages, stats);
    }

    private record AnalysisLine(String sender, String content) {
    }

    private record RawTextPayload(
            String rawText,
            List<String> lines
    ) {
    }

    private record ParseComputation(
            List<KakaoChatMessageDto> messages,
            KakaoChatParsingStats stats
    ) {
    }

    private static final class WorkingMessage {
        private final String sender;
        private final String timeText;
        private final LocalDateTime timestamp;
        private final List<String> contentLines = new ArrayList<>();

        private WorkingMessage(String sender, String timeText, LocalDateTime timestamp) {
            this.sender = sender;
            this.timeText = timeText;
            this.timestamp = timestamp;
        }

        static WorkingMessage start(LocalDate currentDate, KakaoChatParsingUtils.ParsedMessageStart messageStart) {
            LocalDateTime timestamp = KakaoChatParsingUtils.parseDateTime(currentDate, messageStart.timeText());
            WorkingMessage workingMessage = new WorkingMessage(
                    messageStart.sender(),
                    messageStart.timeText(),
                    timestamp
            );
            if (messageStart.inlineContent() != null) {
                workingMessage.contentLines.add(messageStart.inlineContent());
            }
            return workingMessage;
        }

        void append(String continuationLine) {
            contentLines.add(continuationLine);
        }

        KakaoChatMessageDto toDto() {
            String content = String.join("\n", contentLines);
            return new KakaoChatMessageDto(
                    sender,
                    timestamp.toLocalDate().toString(),
                    timeText,
                    timestamp.toString(),
                    content,
                    KakaoChatParsingUtils.classifySpecialType(content)
            );
        }
    }
}
