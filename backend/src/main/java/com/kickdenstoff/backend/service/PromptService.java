package com.kickdenstoff.backend.service;

import com.kickdenstoff.backend.dto.LearningMode;
import com.kickdenstoff.backend.dto.LearningRequest;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    private static final String SYSTEM_PROMPT = """
            /no_think
            Du bist ein lokaler KI-Lerncoach für Schüler. Du erklärst Schulstoff einfach, korrekt und motivierend.
            Leite Thema, Niveau, Ziel und Fach selbst aus der Schülernachricht, dem Material und möglichem PDF-Text ab.
            Falls Fach, Klassenstufe oder Thema fehlen, arbeite trotzdem mit sinnvollen Annahmen und nenne diese kurz.
            Arbeite nur mit den angegebenen Informationen. Wenn wichtige Informationen fehlen, sage kurz, was fehlt,
            statt Inhalte zu erfinden.
            Formatiere normale Antworten als einfaches Markdown mit Überschriften und Aufzählungen.
            Starte normale Antworten mit einer kurzen Zeile: Erkanntes Thema: ...
            Verwende keine Codeblöcke, keine Tabellen und keine Meta-Kommentare wie "nicht geprüft" oder "als KI".
            Gib ausschließlich die finale Antwort aus, keine Denkprozesse.
            Wenn der Modus JSON verlangt, gib ausschließlich valides JSON aus.
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userPrompt(LearningMode mode, LearningRequest request) {
        String learningContext = optionalContext(request);
        String material = valueOrMissing(request.effectiveInputText());
        String task = valueOrMissing(request.effectiveQuestion());
        String fullContext = context(learningContext, material, task);

        String prompt = switch (mode) {
            case EXPLAIN -> """
                    Erkläre die Schülernachricht oder das Material einfach und verständlich.

                    %s

                    Ausgabeformat:
                    Erkanntes Thema: ...
                    ## Kurz erklärt
                    ## Wichtigste Begriffe
                    ## Beispiel
                    ## Merksatz
                    ## Gesamteinschätzung
                    - Was der Schüler schon gut kann
                    - Wo noch eine Lücke sein könnte
                    - Was als Nächstes geübt werden sollte
                    """.formatted(fullContext);
            case EXERCISES -> """
                    Erstelle passende Übungsaufgaben zum erkannten Stoff.

                    %s

                    Ausgabeformat:
                    Erkanntes Thema: ...
                    ## Aufgaben
                    - 5 Aufgaben von leicht bis schwer
                    ## Lösungen
                    - kurze Lösungswege
                    ## Hinweis zum Üben
                    ## Gesamteinschätzung
                    """.formatted(fullContext);
            case QUIZ -> """
                    Erstelle ein Quiz zum erkannten Stoff.

                    %s

                    WICHTIG: Antworte AUSSCHLIESSLICH mit einem validen JSON-Objekt. Kein Markdown, kein Text davor oder danach.
                    Das JSON muss genau diese Felder enthalten:
                    {
                      "title": "Quiz erstellt",
                      "topic": "kurzes erkanntes Thema",
                      "questions": [
                        {
                          "question": "Frage 1?",
                          "options": ["Antwort A", "Antwort B", "Antwort C", "Antwort D"],
                          "correctIndex": 0,
                          "explanation": "Kurze Erklärung, warum die richtige Antwort stimmt."
                        }
                      ],
                      "miniEvaluation": [
                        "0-2 richtig: ...",
                        "3-4 richtig: ...",
                        "5 richtig: ..."
                      ],
                      "overallAssessment": "Was der Schüler schon gut kann, wo Lücken sind und was als Nächstes geübt werden sollte."
                    }

                    Erstelle mindestens 5 Fragen. Nutze Multiple Choice, wenn es sinnvoll ist. Jede Frage braucht Antwortmöglichkeiten,
                    eine richtige Lösung über correctIndex und eine kurze Erklärung.
                    """.formatted(fullContext);
            case CORRECT -> """
                    Korrigiere den folgenden Schülertext.

                    %s

                    Wichtige Regeln:
                    - Korrigiere nur den Schülertext aus Material oder Nachricht.
                    - Wenn dort kein sinnvoller Satz oder weniger als sechs Wörter stehen, erfinde keine Lösung. Bitte dann um den konkreten Text.
                    - Erfinde keine Formeln, Fakten oder Beispieltexte, die nicht im Schülertext stehen.

                    Ausgabeformat:
                    Erkanntes Thema: ...
                    ## Korrigierte Version
                    ## Was verbessert wurde
                    - kurze Punkte
                    ## Tipp
                    - ein konkreter nächster Schritt
                    ## Gesamteinschätzung
                    """.formatted(fullContext);
            case CRASH_COURSE -> """
                    Erstelle einen kompakten Lernplan für eine baldige Klassenarbeit.

                    %s

                    Ausgabeformat:
                    Erkanntes Thema: ...
                    ## Lernziel
                    ## Lernplan
                    ## Wichtigste Regeln
                    ## Typische Aufgaben
                    ## Mini-Test
                    ## Letzte Wiederholung
                    ## Gesamteinschätzung
                    """.formatted(fullContext);
            case SHOW_SOLUTION_PATH -> """
                    Zeige nicht nur das Ergebnis, sondern den Lösungsweg.

                    %s

                    Ausgabeformat:
                    Erkanntes Thema: ...
                    ## Gegeben
                    ## Gesucht
                    ## Schritt-für-Schritt-Lösung
                    ## Warum das funktioniert
                    ## Typischer Fehler
                    ## Gesamteinschätzung
                    """.formatted(fullContext);
            case CHAT -> """
                    Beantworte die Frage des Schülers als Lerncoach.

                    %s

                    Ausgabeformat:
                    Erkanntes Thema: ...
                    ## Antwort
                    ## Nächster Schritt
                    """.formatted(fullContext);
        };

        return """
                /no_think

                %s
                """.formatted(prompt);
    }

    private String context(String learningContext, String material, String task) {
        return """
                Optionaler Kontext aus älteren API-Feldern:
                %s

                Schülernachricht, Schulstoff, Aufgabe oder PDF-Text:
                %s

                Wunsch oder ausgewählter Lernmodus:
                %s

                Leite aus diesen Informationen selbst ein kurzes Thema, ein passendes Niveau und das Lernziel ab.
                """.formatted(learningContext, material, task);
    }

    private String optionalContext(LearningRequest request) {
        String subject = blankToNull(request.safeSubject());
        String grade = blankToNull(request.effectiveGrade());
        String topic = blankToNull(request.safeTopic());

        if (subject == null && grade == null && topic == null) {
            return "nicht angegeben";
        }

        return """
                Fach: %s
                Klasse/Niveau: %s
                Vorab-Thema: %s
                """.formatted(
                subject == null ? "nicht angegeben" : subject,
                grade == null ? "nicht angegeben" : grade,
                topic == null ? "automatisch ableiten" : topic
        );
    }

    private String valueOrMissing(String value) {
        if (value == null || value.isBlank()) {
            return "nicht angegeben";
        }
        return value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank() || "nicht angegeben".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }
}
