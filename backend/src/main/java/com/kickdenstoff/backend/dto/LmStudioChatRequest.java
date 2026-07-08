package com.kickdenstoff.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LmStudioChatRequest(
        String model,
        List<Message> messages,
        double temperature,
        @JsonProperty("max_tokens")
        int maxTokens
) {

    public record Message(String role, String content) {
    }
}
