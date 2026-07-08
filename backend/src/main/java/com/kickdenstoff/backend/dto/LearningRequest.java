package com.kickdenstoff.backend.dto;

import jakarta.validation.constraints.Size;

public record LearningRequest(
        @Size(max = 40, message = "Das Fach darf maximal 40 Zeichen lang sein.")
        String subject,

        @Size(max = 20, message = "Die Klassenstufe darf maximal 20 Zeichen lang sein.")
        String grade,

        @Size(max = 20, message = "Die Klassenstufe darf maximal 20 Zeichen lang sein.")
        String gradeLevel,

        @Size(max = 100, message = "Das Thema darf maximal 100 Zeichen lang sein.")
        String topic,

        @Size(max = 8_000, message = "Das Material darf maximal 8000 Zeichen lang sein.")
        String inputText,

        @Size(max = 8_000, message = "Das Material darf maximal 8000 Zeichen lang sein.")
        String input,

        @Size(max = 2_000, message = "Die Frage darf maximal 2000 Zeichen lang sein.")
        String userQuestion,

        @Size(max = 40, message = "Der Stil darf maximal 40 Zeichen lang sein.")
        String style,

        @Size(max = 40, message = "Der Modus darf maximal 40 Zeichen lang sein.")
        String mode
) {

    public String effectiveGrade() {
        return firstNonBlank(grade, gradeLevel, "nicht angegeben");
    }

    public String effectiveInputText() {
        return firstNonBlank(inputText, input, "");
    }

    public String effectiveQuestion() {
        return firstNonBlank(userQuestion, style, "");
    }

    public boolean hasLearningInput() {
        return !effectiveInputText().isBlank() || !effectiveQuestion().isBlank() || !blankToEmpty(topic).isBlank();
    }

    public String safeSubject() {
        return firstNonBlank(subject, "nicht angegeben");
    }

    public String safeTopic() {
        return firstNonBlank(topic, "nicht angegeben");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
