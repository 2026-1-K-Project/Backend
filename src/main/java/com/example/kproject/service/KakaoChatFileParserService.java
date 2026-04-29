package com.example.kproject.service;

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
        validateFile(file);

        List<String> lines = readAllLines(file);
        if (lines.isEmpty()) {
            throw new ChatUploadException("Uploaded file is empty.");
        }

        List<KakaoChatParseErrorDto> errors = new ArrayList<>();

        String title = KakaoChatParsingUtils.stripBom(lines.get(0));
        String savedAt = extractSavedAt(lines, errors);
        KakaoChatMetaDto meta = new KakaoChatMetaDto(
                title,
                savedAt,
                KakaoChatParsingUtils.deriveRoomName(title),
                null,
                null
        );

        List<KakaoChatMessageDto> messages = parseMessages(lines, errors);
        String analysisText = buildAnalysisText(messages);

        return new KakaoChatUploadResponse(
                meta,
                messages.size(),
                List.copyOf(messages),
                analysisText,
                List.copyOf(errors)
        );
    }

    public String buildAnalysisText(List<KakaoChatMessageDto> messages) {
        return messages.stream()
                .filter(message -> message.specialType() == KakaoChatSpecialType.TEXT)
                .sorted(Comparator.comparing(KakaoChatMessageDto::timestamp))
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

    private List<String> readAllLines(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException exception) {
            throw new ChatUploadException("Failed to read the uploaded txt file.", exception);
        }
    }

    private String extractSavedAt(List<String> lines, List<KakaoChatParseErrorDto> errors) {
        if (lines.size() < 2) {
            errors.add(new KakaoChatParseErrorDto(2, "", "The savedAt metadata line is missing."));
            return null;
        }

        Optional<String> savedAt = KakaoChatParsingUtils.extractSavedAt(lines.get(1));
        if (savedAt.isEmpty()) {
            errors.add(new KakaoChatParseErrorDto(2, lines.get(1), "The savedAt metadata line could not be parsed."));
            return null;
        }
        return savedAt.get();
    }

    private List<KakaoChatMessageDto> parseMessages(List<String> lines, List<KakaoChatParseErrorDto> errors) {
        List<KakaoChatMessageDto> messages = new ArrayList<>();
        LocalDate currentDate = null;
        WorkingMessage currentMessage = null;

        for (int index = 2; index < lines.size(); index++) {
            String rawLine = index == 2 ? KakaoChatParsingUtils.stripBom(lines.get(index)) : lines.get(index);

            Optional<LocalDate> dateSeparator = KakaoChatParsingUtils.parseDateSeparator(rawLine);
            if (dateSeparator.isPresent()) {
                if (currentMessage != null) {
                    messages.add(currentMessage.toDto());
                    currentMessage = null;
                }
                currentDate = dateSeparator.get();
                continue;
            }

            Optional<KakaoChatParsingUtils.ParsedMessageStart> messageStart =
                    KakaoChatParsingUtils.parseMessageStart(rawLine);
            if (messageStart.isPresent()) {
                if (currentMessage != null) {
                    messages.add(currentMessage.toDto());
                }

                if (currentDate == null) {
                    errors.add(new KakaoChatParseErrorDto(
                            index + 1,
                            rawLine,
                            "A message was found before any date separator."
                    ));
                    currentMessage = null;
                    continue;
                }

                currentMessage = WorkingMessage.start(currentDate, messageStart.get());
                continue;
            }

            if (currentMessage != null) {
                currentMessage.append(rawLine);
                continue;
            }

            if (StringUtils.hasText(rawLine)) {
                errors.add(new KakaoChatParseErrorDto(
                        index + 1,
                        rawLine,
                        "A continuation line appeared without a preceding message."
                ));
            }
        }

        if (currentMessage != null) {
            messages.add(currentMessage.toDto());
        }

        return messages;
    }

    private record AnalysisLine(String sender, String content) {
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
                    KakaoChatParsingUtils.classifySpecialType(content),
                    timestamp
            );
        }
    }
}
