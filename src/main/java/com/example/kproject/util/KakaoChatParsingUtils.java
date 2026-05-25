package com.example.kproject.util;

import com.example.kproject.dto.KakaoChatSpecialType;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KakaoChatParsingUtils {

    private static final List<Pattern> MESSAGE_START_PATTERNS = List.of(
            Pattern.compile("^\\[(.+?)\\]\\s*\\[((?:오전|오후|AM|PM|am|pm)?\\s*\\d{1,2}:\\d{2})\\](?:\\s*(.*))?$"),
            Pattern.compile("^(.+?)\\s*\\[((?:오전|오후|AM|PM|am|pm)?\\s*\\d{1,2}:\\d{2})\\](?:\\s*(.*))?$")
    );
    private static final List<Pattern> FULL_TIMESTAMP_MESSAGE_PATTERNS = List.of(
            Pattern.compile("^(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일\\s*((?:오전|오후|AM|PM|am|pm)?\\s*\\d{1,2}:\\d{2})\\s*,\\s*(.+?)\\s*[:：]\\s*(.*)$"),
            Pattern.compile("^(\\d{4})\\s*[./-]\\s*(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})\\s*((?:오전|오후|AM|PM|am|pm)?\\s*\\d{1,2}:\\d{2})\\s*,\\s*(.+?)\\s*[:：]\\s*(.*)$")
    );
    private static final List<Pattern> DATE_SEPARATOR_PATTERNS = List.of(
            Pattern.compile("^(?:-+\\s*)?(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일(?:\\s+.+)?(?:\\s*-+)?$"),
            Pattern.compile("^(?:-+\\s*)?(\\d{4})\\s*[./-]\\s*(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})(?:\\.|\\s+.+)?(?:\\s*-+)?$")
    );
    private static final List<Pattern> SAVED_AT_PATTERNS = List.of(
            Pattern.compile("^저장한 날짜\\s*:\\s*(.+)$"),
            Pattern.compile("^저장 날짜\\s*:\\s*(.+)$"),
            Pattern.compile("^Saved at\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> SAVED_AT_DATE_PATTERNS = List.of(
            Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})"),
            Pattern.compile("(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일")
    );
    private static final Pattern IMAGE_PATTERN = Pattern.compile("^사진(?: \\d+장)?$");
    private static final Pattern FILE_PATTERN = Pattern.compile("^파일\\s*:\\s*.+$");
    private static final Pattern SIMPLE_SPEAKER_PREFIX_PATTERN = Pattern.compile("^([^:：]{1,30})[:：]\\s*(.+)$");

    private static final DateTimeFormatter KOREAN_MERIDIEM_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-M-d a h:mm", Locale.KOREAN);
    private static final DateTimeFormatter ENGLISH_MERIDIEM_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-M-d a h:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMATTER =
            DateTimeFormatter.ofPattern("H:mm");

    private KakaoChatParsingUtils() {
    }

    public static String stripBom(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\uFEFF", "");
    }

    public static boolean looksLikeConversationTitle(String line) {
        String normalized = stripBom(line);
        return StringUtils.hasText(normalized) && normalized.contains("카카오톡 대화");
    }

    public static boolean isSavedAtLine(String line) {
        return extractSavedAt(line).isPresent();
    }

    public static Optional<String> extractSavedAt(String line) {
        if (!StringUtils.hasText(line)) {
            return Optional.empty();
        }

        String normalized = stripBom(line).trim();
        for (Pattern pattern : SAVED_AT_PATTERNS) {
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.matches()) {
                return Optional.ofNullable(matcher.group(1)).map(String::trim);
            }
        }

        Matcher genericMatcher = Pattern.compile(".*?(\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}).*").matcher(normalized);
        if (genericMatcher.matches()) {
            return Optional.of(genericMatcher.group(1).trim());
        }

        return Optional.empty();
    }

    public static Optional<LocalDate> extractSavedAtDate(String savedAt) {
        if (!StringUtils.hasText(savedAt)) {
            return Optional.empty();
        }

        for (Pattern pattern : SAVED_AT_DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(savedAt);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return Optional.of(LocalDate.of(year, month, day));
            }
        }

        return Optional.empty();
    }

    public static String deriveRoomName(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }

        String normalized = title.trim();
        for (String suffix : List.of(" 님과 카카오톡 대화", "과 카카오톡 대화", " 카카오톡 대화")) {
            if (normalized.endsWith(suffix)) {
                return normalized.substring(0, normalized.length() - suffix.length()).trim();
            }
        }
        return normalized;
    }

    public static Optional<LocalDate> parseDateSeparator(String line) {
        if (!StringUtils.hasText(line)) {
            return Optional.empty();
        }

        String normalized = stripBom(line).trim();
        for (Pattern pattern : DATE_SEPARATOR_PATTERNS) {
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.matches()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return Optional.of(LocalDate.of(year, month, day));
            }
        }

        return Optional.empty();
    }

    public static Optional<ParsedMessageStart> parseMessageStart(String line) {
        if (!StringUtils.hasText(line)) {
            return Optional.empty();
        }

        String normalized = stripBom(line).trim();
        for (Pattern pattern : FULL_TIMESTAMP_MESSAGE_PATTERNS) {
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.matches()) {
                LocalDate date = LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                );
                String timeText = matcher.group(4) == null ? null : matcher.group(4).trim();
                String sender = matcher.group(5) == null ? null : matcher.group(5).trim();
                String inlineContent = matcher.group(6);
                return Optional.of(new ParsedMessageStart(
                        sender,
                        timeText,
                        inlineContent == null ? null : inlineContent.trim(),
                        date
                ));
            }
        }

        for (Pattern pattern : MESSAGE_START_PATTERNS) {
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.matches()) {
                String sender = matcher.group(1) == null ? null : matcher.group(1).trim();
                String timeText = matcher.group(2) == null ? null : matcher.group(2).trim();
                String inlineContent = matcher.group(3);
                return Optional.of(new ParsedMessageStart(
                        sender,
                        timeText,
                        inlineContent == null ? null : inlineContent.trim(),
                        null
                ));
            }
        }

        return Optional.empty();
    }

    public static Optional<String> extractFlexibleContent(String line) {
        if (!StringUtils.hasText(line)) {
            return Optional.empty();
        }

        String normalized = stripBom(line).trim();
        if (looksLikeConversationTitle(normalized) || isSavedAtLine(normalized) || parseDateSeparator(normalized).isPresent()) {
            return Optional.empty();
        }

        Optional<ParsedMessageStart> parsedMessageStart = parseMessageStart(normalized);
        if (parsedMessageStart.isPresent()) {
            return Optional.ofNullable(parsedMessageStart.get().inlineContent()).map(String::trim);
        }

        Matcher matcher = SIMPLE_SPEAKER_PREFIX_PATTERN.matcher(normalized);
        if (matcher.matches()) {
            return Optional.ofNullable(matcher.group(2)).map(String::trim);
        }

        return Optional.of(normalized);
    }

    public static LocalDateTime parseDateTime(LocalDate date, String timeText) {
        if (date == null || !StringUtils.hasText(timeText)) {
            throw new IllegalArgumentException("date and timeText are required");
        }

        String normalized = timeText.trim();
        String composed = "%d-%d-%d %s".formatted(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), normalized);

        if (normalized.startsWith("오전") || normalized.startsWith("오후")) {
            return LocalDateTime.parse(composed, KOREAN_MERIDIEM_FORMATTER);
        }

        if (normalized.regionMatches(true, 0, "am", 0, 2) || normalized.regionMatches(true, 0, "pm", 0, 2)) {
            String englishMeridiem = normalized.substring(0, 2).toUpperCase(Locale.ROOT) + normalized.substring(2);
            return LocalDateTime.parse(
                    "%d-%d-%d %s".formatted(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), englishMeridiem),
                    ENGLISH_MERIDIEM_FORMATTER
            );
        }

        try {
            LocalTime localTime = LocalTime.parse(normalized, TWENTY_FOUR_HOUR_FORMATTER);
            return LocalDateTime.of(date, localTime);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Unsupported time text: " + timeText, exception);
        }
    }

    public static KakaoChatSpecialType classifySpecialType(String content) {
        String normalized = content == null ? "" : content.trim();

        if ("메시지가 삭제되었습니다.".equals(normalized)) {
            return KakaoChatSpecialType.DELETED;
        }
        if (IMAGE_PATTERN.matcher(normalized).matches()) {
            return KakaoChatSpecialType.IMAGE;
        }
        if ("이모티콘".equals(normalized)) {
            return KakaoChatSpecialType.EMOTICON;
        }
        if (FILE_PATTERN.matcher(normalized).matches()) {
            return KakaoChatSpecialType.FILE;
        }
        return KakaoChatSpecialType.TEXT;
    }

    public static String normalizeForAnalysis(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String[] rawLines = content.split("\n", -1);
        StringBuilder normalized = new StringBuilder();
        boolean previousBlank = false;

        for (String rawLine : rawLines) {
            String cleanedLine = rawLine.strip();
            boolean blankLine = cleanedLine.isEmpty();

            if (blankLine && previousBlank) {
                continue;
            }

            if (normalized.length() > 0) {
                normalized.append('\n');
            }
            normalized.append(cleanedLine);
            previousBlank = blankLine;
        }

        return normalized.toString().trim();
    }

    public record ParsedMessageStart(
            String sender,
            String timeText,
            String inlineContent,
            LocalDate date
    ) {
    }
}
