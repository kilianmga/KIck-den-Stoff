package com.kickdenstoff.backend.service;

import com.kickdenstoff.backend.dto.LearningMode;
import com.kickdenstoff.backend.dto.LearningRequest;
import com.kickdenstoff.backend.dto.LearningResponse;
import org.springframework.stereotype.Service;

@Service
public class LearningService {

    private final PromptService promptService;
    private final LmStudioClient lmStudioClient;

    public LearningService(PromptService promptService, LmStudioClient lmStudioClient) {
        this.promptService = promptService;
        this.lmStudioClient = lmStudioClient;
    }

    public LearningResponse learn(LearningMode mode, LearningRequest request) {
        if (request == null || !request.hasLearningInput()) {
            return LearningResponse.failure(mode, "Bitte gib ein Thema, Material oder eine Frage ein.");
        }

        try {
            String answer = lmStudioClient.chat(
                    promptService.systemPrompt(),
                    promptService.userPrompt(mode, request)
            );
            return LearningResponse.success(mode, answer);
        } catch (LmStudioException exception) {
            return LearningResponse.failure(mode, exception.getMessage());
        }
    }
}
