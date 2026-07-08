package com.kickdenstoff.backend.service;

import com.kickdenstoff.backend.dto.LearningMode;
import com.kickdenstoff.backend.dto.LearningRequest;
import com.kickdenstoff.backend.dto.LearningResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LearningService {

    private final PromptService promptService;
    private final LmStudioClient lmStudioClient;
    private final PdfTextExtractor pdfTextExtractor;

    public LearningService(PromptService promptService, LmStudioClient lmStudioClient, PdfTextExtractor pdfTextExtractor) {
        this.promptService = promptService;
        this.lmStudioClient = lmStudioClient;
        this.pdfTextExtractor = pdfTextExtractor;
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

    public LearningResponse learnWithPdf(LearningMode mode, LearningRequest request, MultipartFile pdfFile) {
        try {
            String pdfText = pdfTextExtractor.extractText(pdfFile);
            LearningRequest requestWithPdf = withPdfText(request, pdfFile.getOriginalFilename(), pdfText);
            return learn(mode, requestWithPdf);
        } catch (PdfProcessingException exception) {
            return LearningResponse.failure(mode, exception.getMessage());
        }
    }

    private LearningRequest withPdfText(LearningRequest request, String filename, String pdfText) {
        LearningRequest safeRequest = request == null
                ? new LearningRequest(null, null, null, null, null, null, null, null, null)
                : request;

        String existingMaterial = safeRequest.effectiveInputText();
        String combinedMaterial = """
                %s

                Inhalt aus PDF "%s":
                %s
                """.formatted(
                existingMaterial == null || existingMaterial.isBlank() ? "" : existingMaterial,
                filename == null || filename.isBlank() ? "hochgeladene Datei" : filename,
                pdfText
        ).trim();

        return new LearningRequest(
                safeRequest.subject(),
                safeRequest.grade(),
                safeRequest.gradeLevel(),
                safeRequest.topic(),
                combinedMaterial,
                null,
                safeRequest.userQuestion(),
                safeRequest.style(),
                safeRequest.mode()
        );
    }
}
