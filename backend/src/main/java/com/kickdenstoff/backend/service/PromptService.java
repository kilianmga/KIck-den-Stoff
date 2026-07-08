package com.kickdenstoff.backend.service;

import com.kickdenstoff.backend.dto.LearningMode;
import com.kickdenstoff.backend.dto.LearningRequest;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    private static final String SYSTEM_PROMPT = """
            /no_think
            Du bist ein lokaler KI-Lerncoach für Schüler. Du erklärst Schulstoff einfach, korrekt und motivierend.
            Passe deine Sprache an die angegebene Klassenstufe an. Gib keine endlosen Antworten.
            Strukturiere deine Antwort mit klaren Abschnitten. Wenn Informationen fehlen, arbeite mit dem vorhandenen Material
            und sage kurz, was fehlt. Gib keine erfundenen Fakten aus dem Schulmaterial vor.
            Gib ausschließlich die finale Antwort aus, keine Denkprozesse.
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userPrompt(LearningMode mode, LearningRequest request) {
        String context = """
                Fach: %s
                Klasse: %s
                Thema: %s
                Material: %s
                Frage oder Wunsch: %s
                """.formatted(
                request.safeSubject(),
                request.effectiveGrade(),
                request.safeTopic(),
                valueOrMissing(request.effectiveInputText()),
                valueOrMissing(request.effectiveQuestion())
        );

        String prompt = switch (mode) {
            case EXPLAIN -> """
                    Erkläre den folgenden Schulstoff einfach und verständlich.

                    %s
                    Antwortformat:
                    1. Kurz erklärt
                    2. Wichtigste Begriffe
                    3. Beispiel
                    4. Merksatz
                    """.formatted(context);
            case EXERCISES -> """
                    Erstelle passende Übungsaufgaben zum Schulstoff.

                    %s
                    Antwortformat:
                    1. 5 Aufgaben von leicht bis schwer
                    2. Lösungen
                    3. Kurze Lösungswege
                    """.formatted(context);
            case QUIZ -> """
                    Erstelle einen kurzen Wissenstest zum Schulstoff.

                    %s
                    
                    WICHTIG: Antworte AUSSCHLIESSLICH mit einem validen JSON-Array. Kein Markdown, kein Text davor oder danach!
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
                    """.formatted(context);
            case CORRECT -> """
                    Korrigiere oder bewerte die folgende Schülerantwort.

                    %s
                    Antwortformat:
                    1. Was ist richtig?
                    2. Was ist falsch oder unklar?
                    3. Verbesserte Version
                    4. Konkreter Tipp für die nächste Antwort
                    """.formatted(context);
            case CRASH_COURSE -> """
                    Erstelle einen kompakten Lernplan für eine baldige Klassenarbeit.

                    %s
                    Antwortformat:
                    1. Lernziel
                    2. 30-90 Minuten Lernplan
                    3. Wichtigste Regeln oder Fakten
                    4. Typische Aufgaben
                    5. Mini-Test
                    6. Letzte Wiederholung
                    """.formatted(context);
            case SHOW_SOLUTION_PATH -> """
                    Zeige nicht nur das Ergebnis, sondern den Lösungsweg.

                    %s
                    Antwortformat:
                    1. Gegeben
                    2. Gesucht
                    3. Schritt-für-Schritt-Lösung
                    4. Warum das funktioniert
                    5. Typischer Fehler
                    """.formatted(context);
            case CHAT -> """
                    Beantworte die Frage des Schülers als Lerncoach.

                    %s
                    Antworte hilfreich, kurz und schülergerecht.
                    """.formatted(context);
        };

        return """
                /no_think

                %s
                """.formatted(prompt);
    }

    private String valueOrMissing(String value) {
        if (value == null || value.isBlank()) {
            return "nicht angegeben";
        }
        return value;
    }
}
