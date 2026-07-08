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
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new LmStudioException("LM Studio hat eine leere Antwort zurückgegeben.");
        }

        return message.content().trim();
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:1234/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
