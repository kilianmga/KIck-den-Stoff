package com.kickdenstoff.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LmStudioChatResponse(List<Choice> choices) {

    public record Choice(Message message) {
    }

    public record Message(
            String role,
            String content,
            @JsonProperty("reasoning_content")
            String reasoningContent
    ) {
    }
}
