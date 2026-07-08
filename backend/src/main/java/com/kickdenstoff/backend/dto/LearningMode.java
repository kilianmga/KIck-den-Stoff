package com.kickdenstoff.backend.dto;

import java.util.Locale;

public enum LearningMode {
    EXPLAIN("explain", "Erklären"),
    EXERCISES("exercises", "Üben"),
    QUIZ("quiz", "Quiz erstellen"),
    CORRECT("correct", "Korrigieren"),
    CRASH_COURSE("crash-course", "Crashkurs"),
    CHAT("chat", "Freie Frage"),
    SHOW_SOLUTION_PATH("solution-path", "Lösungsweg anzeigen");

    private final String apiValue;
    private final String title;

    LearningMode(String apiValue, String title) {
        this.apiValue = apiValue;
        this.title = title;
    }

    public String apiValue() {
        return apiValue;
    }

    public String title() {
        return title;
    }

    public static LearningMode from(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return EXPLAIN;
        }

        String normalized = rawMode.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "EXPLAIN" -> EXPLAIN;
            case "CREATE_TASKS", "TASKS", "EXERCISES" -> EXERCISES;
            case "CHECK_KNOWLEDGE", "KNOWLEDGE_CHECK", "QUIZ" -> QUIZ;
            case "CORRECT_TEXT", "CORRECT" -> CORRECT;
            case "CRASH_COURSE" -> CRASH_COURSE;
            case "SHOW_SOLUTION_PATH", "SOLUTION_PATH" -> SHOW_SOLUTION_PATH;
            case "FREE_QUESTION", "CHAT" -> CHAT;
            default -> EXPLAIN;
        };
    }
}
