package com.kickdenstoff.backend.service;

import com.kickdenstoff.backend.dto.LearningMode;
import com.kickdenstoff.backend.dto.LearningRequest;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    private static final String SYSTEM_PROMPT = """
            /no_think
            Du bist ein lokaler KI-Lerncoach für Schüler. Du erklärst Schulstoff einfach, korrekt und motivierend.
            Passe deine Sprache an die angegebene Klassenstufe an. Antworte kurz, praktisch und direkt.
            Arbeite nur mit den angegebenen Informationen. Wenn wichtige Informationen fehlen, sage kurz, was fehlt,
            statt Inhalte zu erfinden.
            Formatiere die Antwort als einfaches Markdown mit Überschriften und Aufzählungen.
            Verwende keine Codeblöcke, keine Tabellen und keine Meta-Kommentare wie "nicht geprüft" oder "als KI".
            Gib ausschließlich die finale Antwort aus, keine Denkprozesse.
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userPrompt(LearningMode mode, LearningRequest request) {
        String learningContext = """
                Fach: %s
                Klasse: %s
                Thema: %s
                """.formatted(
                request.safeSubject(),
                request.effectiveGrade(),
                request.safeTopic()
        );
        String material = valueOrMissing(request.effectiveInputText());
        String task = valueOrMissing(request.effectiveQuestion());
        String fullContext = context(learningContext, material, task);

        String prompt = switch (mode) {
            case EXPLAIN -> """
                    Erkläre den folgenden Schulstoff einfach und verständlich.

                    %s
                    Schulstoff oder Material:
                    %s

                    Wunsch des Schülers:
                    %s

                    Ausgabeformat:
                    ## Kurz erklärt
                    ## Wichtigste Begriffe
                    ## Beispiel
                    ## Merksatz
                    """.formatted(learningContext, material, task);
            case EXERCISES -> """
                    Erstelle passende Übungsaufgaben zum Schulstoff.

                    %s

                    Ausgabeformat:
                    ## Aufgaben
                    - 5 Aufgaben von leicht bis schwer
                    ## Lösungen
                    - kurze Lösungswege
                    """.formatted(fullContext);
            case QUIZ -> """
                    Erstelle einen kurzen Wissenstest zum Schulstoff.

                    %s

                    WICHTIG: Antworte AUSSCHLIESSLICH mit einem validen JSON-Array. Kein Markdown, kein Text davor oder danach.
                    Format-Beispiel:
                    [
                      {
                        "question": "Frage 1?",
                        "options": ["A", "B", "C", "D"],
                        "correctIndex": 0,
                        "explanation": "Erklärung für Antwort A"
                      }
                    ]

                    Erstelle 5 Quizfragen.
                    """.formatted(fullContext);
            case CORRECT -> """
                    Korrigiere den folgenden Schülertext.

                    %s
                    Zu korrigierender Schülertext:
                    %s

                    Auftrag:
                    %s

                    Wichtige Regeln:
                    - Der Auftrag ist nicht der Schülertext.
                    - Korrigiere nur den Text unter "Zu korrigierender Schülertext".
                    - Wenn dort kein sinnvoller Satz oder weniger als sechs Wörter stehen, erfinde keine Lösung. Bitte dann um den konkreten Text.
                    - Erfinde keine Formeln, Fakten oder Beispieltexte, die nicht im Schülertext stehen.

                    Ausgabeformat:
                    ## Korrigierte Version
                    ## Was verbessert wurde
                    - kurze Punkte
                    ## Tipp
                    - ein konkreter nächster Schritt
                    """.formatted(learningContext, material, task);
            case CRASH_COURSE -> """
                    Erstelle einen kompakten Lernplan für eine baldige Klassenarbeit.

                    %s
                    Schulstoff oder Material:
                    %s

                    Wunsch oder verfügbare Zeit:
                    %s

                    Ausgabeformat:
                    ## Lernziel
                    ## Lernplan
                    ## Wichtigste Regeln
                    ## Typische Aufgaben
                    ## Mini-Test
                    ## Letzte Wiederholung
                    """.formatted(learningContext, material, task);
            case SHOW_SOLUTION_PATH -> """
                    Zeige nicht nur das Ergebnis, sondern den Lösungsweg.

                    %s
                    Aufgabe oder Material:
                    %s

                    Frage:
                    %s

                    Ausgabeformat:
                    ## Gegeben
                    ## Gesucht
                    ## Schritt-für-Schritt-Lösung
                    ## Warum das funktioniert
                    ## Typischer Fehler
                    """.formatted(learningContext, material, task);
            case CHAT -> """
                    Beantworte die Frage des Schülers als Lerncoach.

                    %s
                    Schulstoff oder Material:
                    %s

                    Frage:
                    %s

                    Antworte hilfreich, kurz und schülergerecht.
                    """.formatted(learningContext, material, task);
        };

        return """
                /no_think

                %s
                """.formatted(prompt);
    }

    private String context(String learningContext, String material, String task) {
        return """
                %s
                Schulstoff oder Material:
                %s

                Wunsch des Schülers:
                %s
                """.formatted(learningContext, material, task);
    }

    private String valueOrMissing(String value) {
        if (value == null || value.isBlank()) {
            return "nicht angegeben";
        }
        return value;
    }
}
