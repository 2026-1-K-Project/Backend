package com.example.kproject.util;

import com.example.kproject.dto.KakaoChatSpecialType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KakaoChatParsingUtils {

    private static final Pattern MESSAGE_START_PATTERN = Pattern.compile(
            "^\\[(.+?)\\] \\[((오전|오후) (\\d{1,2}):(\\d{2}))\\](?:\\s(.*))?$"
    );
    private static final Pattern DATE_SEPARATOR_PATTERN = Pattern.compile(
            "^-+ (\\d{4})년 (\\d{1,2})월 (\\d{1,2})일 .+ -+$"
    );
    private static final Pattern SAVED_AT_PATTERN = Pattern.compile("^저장한 날짜\\s*:\\s*(.+)$");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("^사진(?: \\d+장)?$");
    private static final DateTimeFormatter KAKAO_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-M-d a h:mm", Locale.KOREAN);

    private KakaoChatParsingUtils() {
    }

    public static String stripBom(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\uFEFF", "");
    }

    public static Optional<String> extractSavedAt(String line) {
        if (line == null) {
            return Optional.empty();
        }

        Matcher matcher = SAVED_AT_PATTERN.matcher(stripBom(line));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1).trim());
    }

    public static String deriveRoomName(String title) {
        if (title == null) {
            return null;
        }
        if (title.endsWith(" 님과 카카오톡 대화")) {
            return title.substring(0, title.length() - " 님과 카카오톡 대화".length());
        }
        if (title.endsWith("과 카카오톡 대화")) {
            return title.substring(0, title.length() - "과 카카오톡 대화".length()).trim();
        }
        return title;
    }

    public static Optional<LocalDate> parseDateSeparator(String line) {
        Matcher matcher = DATE_SEPARATOR_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        return Optional.of(LocalDate.of(year, month, day));
    }

    public static Optional<ParsedMessageStart> parseMessageStart(String line) {
        Matcher matcher = MESSAGE_START_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(new ParsedMessageStart(
                matcher.group(1),
                matcher.group(2),
                matcher.group(6)
        ));
    }

    public static LocalDateTime parseDateTime(LocalDate date, String timeText) {
        return LocalDateTime.parse(
                "%d-%d-%d %s".formatted(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), timeText),
                KAKAO_TIME_FORMATTER
        );
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
        if (normalized.startsWith("파일: ")) {
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
            String inlineContent
    ) {
    }
}
