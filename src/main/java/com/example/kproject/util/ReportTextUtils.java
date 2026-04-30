package com.example.kproject.util;

import com.example.kproject.domain.ReportMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ReportTextUtils {

    public static final int INTEREST_BASE_SCORE = 35;
    public static final int INTEREST_QUESTION_WEIGHT = 16;
    public static final int INTEREST_POSITIVE_WEIGHT = 18;
    public static final int INTEREST_LAUGH_WEIGHT = 10;
    public static final int INTEREST_PROPOSAL_WEIGHT = 14;
    public static final int INTEREST_REPLY_SPEED_WEIGHT = 14;
    public static final int INTEREST_REPLY_LENGTH_WEIGHT = 8;
    public static final int INTEREST_MUTUALITY_WEIGHT = 10;

    public static final Set<String> POSITIVE_TERMS = Set.of(
            "좋아", "좋아요", "좋다", "좋네", "좋겠다", "재밌", "재미", "행복", "고마워", "감사",
            "보고싶", "웃겨", "귀엽", "최고", "설렌", "즐거", "기대", "오", "와"
    );
    public static final Set<String> NEGATIVE_TERMS = Set.of(
            "싫", "별로", "바빠", "피곤", "힘들", "짜증", "불편", "부담", "곤란", "미안", "어색"
    );
    public static final Set<String> PROPOSAL_TERMS = Set.of(
            "같이", "보자", "갈래", "먹자", "할래", "만날", "다음에", "언제", "약속", "가자", "보러"
    );
    public static final Set<String> EMPATHY_TERMS = Set.of(
            "괜찮", "이해", "다행", "고생", "수고", "응원", "맞아", "그렇구나", "헉", "아쉽"
    );
    public static final Set<String> PLANNING_TERMS = Set.of(
            "일정", "시간", "정리", "확인", "준비", "먼저", "계획", "약속", "정하", "체크", "공유"
    );
    public static final Set<String> EXPLORATION_TERMS = Set.of(
            "새로운", "여행", "전시", "영화", "아이디어", "궁금", "도전", "추천", "가보고", "해보고"
    );
    public static final Set<String> STOPWORDS = Set.of(
            "오늘", "내일", "지금", "그냥", "진짜", "약간", "엄청", "너무", "나는", "너는", "우리는",
            "그리고", "근데", "그러면", "이거", "그거", "저거", "우리", "저도", "나도", "너도",
            "있어", "없어", "해서", "하면", "하는", "했어", "했네", "해야", "해서요", "근데요",
            "이제", "그럼", "혹시", "에서", "으로", "에게", "이랑", "하고", "같은", "입니다",
            "있는데", "있으면", "ㅋㅋ", "ㅎㅎ"
    );

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[가-힣A-Za-z0-9]+");
    private static final Pattern LAUGH_PATTERN = Pattern.compile("(ㅋ{2,}|ㅎ{2,}|하하|헤헤)");

    private ReportTextUtils() {
    }

    public static String safeText(String content) {
        return content == null ? "" : content.trim();
    }

    public static boolean hasText(String content) {
        return content != null && !content.isBlank();
    }

    public static boolean isQuestion(String content) {
        String normalized = safeText(content);
        if (normalized.contains("?")) {
            return true;
        }

        return normalized.endsWith("까")
                || normalized.endsWith("까?")
                || normalized.endsWith("어때")
                || normalized.endsWith("어때?")
                || normalized.endsWith("뭐해")
                || normalized.endsWith("뭐해?")
                || normalized.endsWith("갈래")
                || normalized.endsWith("갈래?")
                || normalized.endsWith("할래")
                || normalized.endsWith("할래?");
    }

    public static boolean hasLaugh(String content) {
        return LAUGH_PATTERN.matcher(safeText(content)).find();
    }

    public static boolean hasPositiveTone(String content) {
        return containsAny(content, POSITIVE_TERMS) || hasLaugh(content);
    }

    public static boolean hasNegativeTone(String content) {
        return containsAny(content, NEGATIVE_TERMS);
    }

    public static boolean hasProposal(String content) {
        return containsAny(content, PROPOSAL_TERMS);
    }

    public static boolean containsAny(String content, Set<String> terms) {
        String normalized = safeText(content).toLowerCase(Locale.ROOT);
        return terms.stream().anyMatch(normalized::contains);
    }

    public static int countTermMatches(String content, Set<String> terms) {
        String normalized = safeText(content).toLowerCase(Locale.ROOT);
        return (int) terms.stream().filter(normalized::contains).count();
    }

    public static List<String> tokenize(String content) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(safeText(content).toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() < 2 || STOPWORDS.contains(token)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    public static List<String> distinctTopTokens(List<String> contents, int limit) {
        return contents.stream()
                .flatMap(content -> extractKeywordCandidates(content).stream())
                .collect(Collectors.groupingBy(token -> token, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Comparator.<java.util.Map.Entry<String, Long>>comparingLong(java.util.Map.Entry::getValue)
                        .reversed()
                        .thenComparing(entry -> entry.getKey().length(), Comparator.reverseOrder())
                        .thenComparing(java.util.Map.Entry::getKey))
                .map(java.util.Map.Entry::getKey)
                .limit(limit)
                .toList();
    }

    public static List<String> extractKeywordCandidates(String content) {
        List<String> tokens = tokenize(content);
        LinkedHashSet<String> candidates = new LinkedHashSet<>(tokens);

        for (int index = 0; index < tokens.size() - 1; index++) {
            String first = tokens.get(index);
            String second = tokens.get(index + 1);
            if (first.length() < 2 || second.length() < 2) {
                continue;
            }
            candidates.add(first + " " + second);
        }
        return List.copyOf(candidates);
    }

    public static String buildAnalysisText(List<ReportMessage> messages) {
        return messages.stream()
                .filter(message -> hasText(message.content()))
                .map(message -> message.sender() + ": " + safeText(message.content()))
                .collect(Collectors.joining("\n"));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int percentage(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return clamp((int) Math.round((part * 100.0) / total), 0, 100);
    }

    public static int durationMinutes(ReportMessage first, ReportMessage second) {
        if (first == null || second == null) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(first.dateTime(), second.dateTime()).toMinutes());
    }
}
