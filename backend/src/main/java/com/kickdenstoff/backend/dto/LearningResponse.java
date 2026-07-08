package com.kickdenstoff.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningResponse(
        String mode,
        String title,
        String answer,
        String result,
        boolean success,
        String error
) {

    public static LearningResponse success(LearningMode mode, String answer) {
        return new LearningResponse(mode.apiValue(), mode.title(), answer, answer, true, null);
    }

    public static LearningResponse failure(LearningMode mode, String error) {
        return new LearningResponse(mode.apiValue(), mode.title(), null, null, false, error);
    }
}
