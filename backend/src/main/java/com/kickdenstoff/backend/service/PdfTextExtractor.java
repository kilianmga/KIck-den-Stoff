package com.kickdenstoff.backend.service;

import java.io.IOException;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfTextExtractor {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final int MAX_EXTRACTED_CHARACTERS = 12_000;

    public String extractText(MultipartFile file) {
        validatePdf(file);

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document);
            String normalizedText = normalize(text);

            if (normalizedText.isBlank()) {
                throw new PdfProcessingException(
                        "Die PDF enthaelt keinen auslesbaren Text. Bitte lade eine Text-PDF hoch, keine gescannte Bild-PDF."
                );
            }

            if (normalizedText.length() > MAX_EXTRACTED_CHARACTERS) {
                return normalizedText.substring(0, MAX_EXTRACTED_CHARACTERS)
                        + "\n\n[Hinweis: Die PDF war lang. Fuer die Anfrage wurden die ersten "
                        + MAX_EXTRACTED_CHARACTERS
                        + " Zeichen verwendet.]";
            }

            return normalizedText;
        } catch (IOException exception) {
            throw new PdfProcessingException(
                    "Die PDF konnte nicht gelesen werden. Bitte pruefe, ob die Datei nicht beschaedigt ist.",
                    exception
            );
        }
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PdfProcessingException("Bitte waehle eine PDF-Datei aus.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new PdfProcessingException("Die PDF ist zu gross. Bitte lade maximal 10 MB hoch.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);

        if (!filename.endsWith(".pdf") || !contentType.equals("application/pdf")) {
            throw new PdfProcessingException("Bitte lade nur echte PDF-Dateien hoch. Bilder werden nicht verarbeitet.");
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
