package com.kickdenstoff.backend.service;

import java.util.List;

import com.kickdenstoff.backend.config.LmStudioProperties;
import com.kickdenstoff.backend.dto.LmStudioChatRequest;
import com.kickdenstoff.backend.dto.LmStudioChatResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class LmStudioClient {

    private final LmStudioProperties properties;
    private final RestClient restClient;

    public LmStudioClient(LmStudioProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(stripTrailingSlash(properties.baseUrl()))
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        LmStudioChatRequest request = new LmStudioChatRequest(
                properties.model(),
                List.of(
                        new LmStudioChatRequest.Message("system", systemPrompt),
                        new LmStudioChatRequest.Message("user", userPrompt)
                ),
                properties.temperature(),
                properties.maxTokens()
        );

        try {
            LmStudioChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (incomingRequest, incomingResponse) -> {
                        throw new LmStudioException("LM Studio antwortet mit HTTP "
                                + incomingResponse.getStatusCode().value() + ".");
                    })
                    .body(LmStudioChatResponse.class);

            return extractAnswer(response);
        } catch (LmStudioException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new LmStudioException("LM Studio ist nicht erreichbar. Bitte Modell laden und Local Server starten.", exception);
        }
    }

    private String extractAnswer(LmStudioChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new LmStudioException("LM Studio hat keine Antwort zurückgegeben.");
        }

        LmStudioChatResponse.Message message = response.choices().getFirst().message();
        if (message == null) {
            throw new LmStudioException("LM Studio hat keine Chat-Nachricht zurückgegeben.");
        }

        if (message.content() != null && !message.content().isBlank()) {
            return message.content().trim();
        }

        if (message.reasoningContent() != null && !message.reasoningContent().isBlank()) {
            return extractFinalAnswerFromReasoning(message.reasoningContent());
        }

        throw new LmStudioException("LM Studio hat eine leere Antwort zurückgegeben. Bei Qwen-Modellen hilft oft ein höheres max_tokens-Limit oder ein Modell ohne Thinking-Modus.");
    }

    private String extractFinalAnswerFromReasoning(String reasoningContent) {
        String trimmed = reasoningContent.trim();
        List<String> markers = List.of(
                "Finalizing the Output (in German):",
                "Finalizing the Output:",
                "Final answer:",
                "Final Answer:",
                "Finale Antwort:",
                "Antwort:"
        );

        for (String marker : markers) {
            int markerIndex = trimmed.lastIndexOf(marker);
            if (markerIndex >= 0) {
                String candidate = trimmed.substring(markerIndex + marker.length()).trim();
                if (!candidate.isBlank()) {
                    return stripUnfinishedQuote(candidate);
                }
            }
        }

        throw new LmStudioException("LM Studio hat nur Denktext statt einer finalen Antwort geliefert. Stelle in LM Studio das Prompt Template auf ChatML oder nutze ein Modell ohne Thinking-Modus.");
    }

    private String stripUnfinishedQuote(String value) {
        String cleaned = value.trim();
        int lastThoughtStep = cleaned.lastIndexOf("\n\n");
        if (cleaned.endsWith("but") && lastThoughtStep > 0) {
            return cleaned.substring(0, lastThoughtStep).trim();
        }
        return cleaned;
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:1234/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
