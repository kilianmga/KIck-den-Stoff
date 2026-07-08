package com.kickdenstoff.backend.dto;

import java.util.List;

public record LmStudioChatResponse(List<Choice> choices) {

    public record Choice(Message message) {
    }

    public record Message(String role, String content) {
    }
}
