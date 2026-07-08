# Backend Prompt — KIck den Stoff

Du übernimmst das Backend für eine Hackathon-App namens **KIck den Stoff**.

## Ziel

Baue ein Spring-Boot-Backend, das als API zwischen dem Svelte-Frontend und **LM Studio** funktioniert. LM Studio läuft lokal und stellt eine OpenAI-kompatible API bereit.

Die App soll Schülern helfen, Schulstoff besser zu verstehen. Das Backend nimmt Text vom Frontend entgegen, baut daraus einen passenden Prompt und fragt LM Studio. Die Antwort wird strukturiert an das Frontend zurückgegeben.

## Tech Stack

- Java 25
- Spring Boot 4.1
- Gradle
- Spring Web
- Spring Validation
- Optional: Lombok, nur wenn das Team damit klarkommt
- Keine Datenbank im ersten MVP
- LM Studio als lokale KI-Schnittstelle

## LM Studio Schnittstelle

LM Studio läuft standardmäßig hier:

```env
LM_STUDIO_BASE_URL=http://localhost:1234/v1
LM_STUDIO_MODEL=local-model
```

Das Backend soll diese Werte über `application.properties` oder Umgebungsvariablen lesen können.

Beispiel-Endpoint von LM Studio:

```txt
POST http://localhost:1234/v1/chat/completions
```

Beispiel-Request an LM Studio:

```json
{
  "model": "local-model",
  "messages": [
    {
      "role": "system",
      "content": "Du bist ein hilfreicher Lerncoach für Schüler. Antworte klar, einfach und strukturiert."
    },
    {
      "role": "user",
      "content": "Erkläre lineare Funktionen für Klasse 8."
    }
  ],
  "temperature": 0.4
}
```

Das Backend soll die Antwort aus `choices[0].message.content` lesen.

## Projektstruktur

Lege das Backend ungefähr so an:

```txt
backend/
  pom.xml
  src/main/java/com/kickdenstoff/backend/
    KickDenStoffBackendApplication.java
    controller/
      LearningController.java
    service/
      LmStudioClient.java
      PromptService.java
    dto/
      LearningRequest.java
      LearningResponse.java
      LmStudioChatRequest.java
      LmStudioChatResponse.java
    config/
      LmStudioProperties.java
  src/main/resources/
    application.properties
```

## REST API für das Frontend

Baue diese Endpoints:

```txt
POST /api/learn/explain
POST /api/learn/exercises
POST /api/learn/quiz
POST /api/learn/correct
POST /api/learn/crash-course
POST /api/learn/chat
GET  /api/health
```

## Gemeinsames Request-Format

Alle Lern-Endpoints außer `/api/health` sollen dieses JSON akzeptieren:

```json
{
  "subject": "Mathe",
  "grade": "8",
  "topic": "Lineare Funktionen",
  "inputText": "y = mx + b beschreibt eine Gerade.",
  "userQuestion": "Kannst du mir das einfach erklären?"
}
```

Felder:

- `subject`: Fach, optional aber sinnvoll
- `grade`: Klassenstufe, optional
- `topic`: Thema, optional
- `inputText`: Schulstoff, Text, Aufgabe oder Antwort des Schülers
- `userQuestion`: konkrete Frage oder Aufgabe

## Gemeinsames Response-Format

Alle Lern-Endpoints sollen so antworten:

```json
{
  "mode": "explain",
  "answer": "Hier steht die KI-Antwort.",
  "success": true,
  "error": null
}
```

Bei Fehlern:

```json
{
  "mode": "explain",
  "answer": null,
  "success": false,
  "error": "LM Studio ist nicht erreichbar. Bitte prüfe, ob der lokale Server läuft."
}
```

## Prompt-Logik

Erstelle im `PromptService` je nach Endpoint einen anderen Prompt.

### System Prompt

Nutze immer diesen System Prompt:

```txt
Du bist ein lokaler KI-Lerncoach für Schüler. Du erklärst Schulstoff einfach, korrekt und motivierend. Passe deine Sprache an die angegebene Klassenstufe an. Gib keine endlosen Antworten. Strukturiere deine Antwort mit klaren Abschnitten. Wenn Informationen fehlen, arbeite mit dem vorhandenen Material und sage kurz, was fehlt. Gib keine erfundenen Fakten aus dem Schulmaterial vor.
```

### Explain Prompt

```txt
Erkläre den folgenden Schulstoff einfach und verständlich.

Fach: {subject}
Klasse: {grade}
Thema: {topic}
Material: {inputText}
Frage: {userQuestion}

Antwortformat:
1. Kurz erklärt
2. Wichtigste Begriffe
3. Beispiel
4. Merksatz
```

### Exercises Prompt

```txt
Erstelle passende Übungsaufgaben zum Schulstoff.

Fach: {subject}
Klasse: {grade}
Thema: {topic}
Material: {inputText}
Wunsch: {userQuestion}

Antwortformat:
1. 5 Aufgaben von leicht bis schwer
2. Lösungen
3. Kurze Lösungswege
```

### Quiz Prompt

```txt
Erstelle einen kurzen Wissenstest zum Schulstoff.

Fach: {subject}
Klasse: {grade}
Thema: {topic}
Material: {inputText}
Wunsch: {userQuestion}

Antwortformat:
1. 5 Quizfragen
2. Antwortmöglichkeiten, wenn sinnvoll
3. Lösungen am Ende
4. Mini-Auswertung, was man können sollte
```

### Correct Prompt

```txt
Korrigiere oder bewerte die folgende Schülerantwort.

Fach: {subject}
Klasse: {grade}
Thema: {topic}
Aufgabe oder Material: {inputText}
Schülerfrage oder Schülerantwort: {userQuestion}

Antwortformat:
1. Was ist richtig?
2. Was ist falsch oder unklar?
3. Verbesserte Version
4. Konkreter Tipp für die nächste Antwort
```

### Crash Course Prompt

```txt
Erstelle einen kompakten Lernplan für eine baldige Klassenarbeit.

Fach: {subject}
Klasse: {grade}
Thema: {topic}
Material: {inputText}
Wunsch oder verfügbare Zeit: {userQuestion}

Antwortformat:
1. Lernziel
2. 30-90 Minuten Lernplan
3. Wichtigste Regeln oder Fakten
4. Typische Aufgaben
5. Mini-Test
6. Letzte Wiederholung
```

### Chat Prompt

```txt
Beantworte die Frage des Schülers als Lerncoach.

Fach: {subject}
Klasse: {grade}
Thema: {topic}
Material: {inputText}
Frage: {userQuestion}

Antworte hilfreich, kurz und schülergerecht.
```

## CORS

Das Svelte-Frontend läuft wahrscheinlich auf:

```txt
http://localhost:5173
```

Aktiviere CORS für diesen Ursprung.

## Health Endpoint

`GET /api/health` soll prüfen, ob das Backend läuft.

Antwort:

```json
{
  "status": "ok",
  "service": "kick-den-stoff-backend"
}
```

Optional kann zusätzlich geprüft werden, ob LM Studio erreichbar ist. Für den MVP reicht aber Backend-Health.

## Fehlerbehandlung

Das Backend soll nicht abstürzen, wenn LM Studio nicht läuft.

Bei Verbindungsfehlern soll eine verständliche Fehlermeldung zurückkommen:

```txt
LM Studio ist nicht erreichbar. Starte LM Studio, lade ein Modell und aktiviere den Local Server.
```

## Akzeptanzkriterien

Das Backend ist fertig, wenn:

- `mvn spring-boot:run` funktioniert
- `GET /api/health` eine OK-Antwort liefert
- `POST /api/learn/explain` eine Antwort von LM Studio zurückgibt
- Fehler sauber als JSON zurückgegeben werden
- Das Frontend von `localhost:5173` zugreifen darf
- Keine Cloud-KI verwendet wird
- Die LM-Studio-URL konfigurierbar ist

## Beispiel-Test mit curl

```bash
curl -X POST http://localhost:8080/api/learn/explain \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Mathe",
    "grade": "8",
    "topic": "Lineare Funktionen",
    "inputText": "Eine lineare Funktion hat die Form y = mx + b.",
    "userQuestion": "Erklär mir das einfach."
  }'
```

## Priorität im Hackathon

Baue zuerst nur diesen Flow stabil:

```txt
/api/learn/explain -> LM Studio -> Antwort zurück
```

Danach die anderen Endpoints ergänzen. Wenn die Zeit knapp ist, können alle Endpoints intern dieselbe Methode verwenden und nur andere Prompts an LM Studio schicken.
