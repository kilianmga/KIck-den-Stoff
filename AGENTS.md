# AGENTS.md – Kick den Stoff

## Projektkontext

**Kick den Stoff** ist ein Hackathon-Prototyp für eine lokale KI-Lernapp. Die App hilft Schülern, Schulstoff schnell zu verstehen, zu üben und zu testen. Die KI läuft lokal über **LM Studio** und wird vom Backend über eine OpenAI-kompatible Schnittstelle angesprochen.

Die App soll aus eingegebenem oder hochgeladenem Lernstoff folgende Lernhilfen erzeugen:

- einfache Erklärungen
- Schritt-für-Schritt-Lösungswege
- Übungsaufgaben
- Wissenschecks
- Textkorrekturen
- kompakte Klausurvorbereitung
- personalisierte Lernhinweise

## Ziel des Prototyps

Der Prototyp soll in kurzer Zeit zeigen, dass lokale KI sinnvoll in eine Lernplattform integriert werden kann. Der Fokus liegt nicht auf perfekter Skalierung, sondern auf einem überzeugenden Demo-Flow.

Der wichtigste Demo-Flow:

1. Schüler gibt ein Thema oder Lernmaterial ein.
2. Schüler wählt eine Aktion, zum Beispiel „Einfach erklären“ oder „Aufgaben erstellen“.
3. Das Spring-Boot-Backend sendet eine strukturierte Anfrage an LM Studio.
4. LM Studio erzeugt lokal eine Antwort.
5. Das Svelte-Frontend zeigt die Antwort als Lernkarte, Aufgabenliste oder Feedback an.

## Tech Stack

### Frontend

- Svelte
- TypeScript
- Vite
- Fetch API für Backend-Kommunikation
- Komponentenbasierte UI

### Backend

- Java
- Spring Boot
- Spring Web
- Optional: Spring Validation
- Optional: Spring Data JPA für spätere Persistenz

### KI-Schnittstelle

- LM Studio
- OpenAI-kompatible Local Server API
- Standard-Endpunkt typischerweise: `http://localhost:1234/v1/chat/completions`

### Lokale Speicherung im MVP

Für den Hackathon reicht zunächst In-Memory-Speicherung oder lokale JSON-Dateien. Eine Datenbank ist optional.

Spätere Option:

- SQLite
- PostgreSQL
- H2 für Demo-Zwecke

## Architektur

```text
Svelte Frontend
      |
      | HTTP REST
      v
Spring Boot Backend
      |
      | OpenAI-compatible HTTP request
      v
LM Studio Local Server
      |
      v
Lokales Sprachmodell
```

## Rollen der Agenten

Dieses Projekt kann in mehrere Agenten oder Arbeitsbereiche aufgeteilt werden.

## Agent: Frontend

### Aufgabe

Der Frontend-Agent baut die Svelte-Oberfläche für den Lernprototyp.

### Verantwortlichkeiten

- Startseite mit App-Namen und Kurzbeschreibung
- Eingabefeld für Schulstoff, Thema oder Aufgabe
- Aktionsauswahl für Lernmodus
- Anzeige von KI-Antworten
- Ladezustände und Fehlermeldungen
- einfache, klare Hackathon-UI

### Wichtige Screens

#### 1. Home Screen

Elemente:

- Titel: „Kick den Stoff“
- Untertitel: „Lokaler KI-Lerncoach für deinen Schulstoff“
- großes Texteingabefeld
- Auswahl für Fach oder Thema
- Buttons für Lernaktionen

#### 2. Lernaktion Screen

Buttons:

- Einfach erklären
- Aufgaben erstellen
- Wissen testen
- Text korrigieren
- Klausur-Crashkurs
- Lösungsweg anzeigen

#### 3. Ergebnis Screen

Elemente:

- generierte KI-Antwort
- optional kopierbarer Text
- optional Button „Noch einfacher erklären“
- optional Button „Quiz daraus machen“

### Frontend API Contract

Das Frontend ruft das Backend über folgenden Endpoint auf:

```http
POST /api/learn
Content-Type: application/json
```

Request:

```json
{
  "mode": "EXPLAIN",
  "subject": "Mathe",
  "gradeLevel": "8",
  "input": "Lineare Funktionen erklären",
  "style": "simple"
}
```

Response:

```json
{
  "mode": "EXPLAIN",
  "title": "Lineare Funktionen einfach erklärt",
  "result": "Eine lineare Funktion beschreibt eine gerade Linie..."
}
```

## Agent: Backend

### Aufgabe

Der Backend-Agent baut die Spring-Boot-API, validiert Eingaben und verbindet die App mit LM Studio.

### Verantwortlichkeiten

- REST Endpoint `/api/learn`
- DTOs für Request und Response
- Prompt-Erstellung je Lernmodus
- HTTP-Client für LM Studio
- Fehlerbehandlung, falls LM Studio nicht erreichbar ist
- CORS-Konfiguration für das lokale Svelte-Frontend

### Empfohlene Backend-Struktur

```text
src/main/java/com/kickdenstoff/app
├── KickDenStoffApplication.java
├── controller
│   └── LearnController.java
├── dto
│   ├── LearnRequest.java
│   ├── LearnResponse.java
│   ├── LmStudioRequest.java
│   └── LmStudioResponse.java
├── service
│   ├── LearnService.java
│   ├── PromptBuilder.java
│   └── LmStudioClient.java
└── config
    └── CorsConfig.java
```

### Backend Endpoint

```http
POST /api/learn
```

### Learn Modes

```text
EXPLAIN
CREATE_TASKS
CHECK_KNOWLEDGE
CORRECT_TEXT
CRASH_COURSE
SHOW_SOLUTION_PATH
```

### Prompt-Regeln

Alle Prompts sollen:

- auf Deutsch antworten
- altersgerecht formulieren
- keine erfundenen Fakten behaupten
- bei unklarem Input Rückfragen oder Annahmen nennen
- Ergebnisse gut strukturiert ausgeben
- Schüler nicht nur die Lösung geben, sondern Verständnis fördern

### Beispiel System Prompt

```text
Du bist ein lokaler KI-Lerncoach für Schüler. Antworte auf Deutsch, verständlich und altersgerecht. Erkläre Schulstoff klar, ohne unnötige Fachsprache. Wenn Informationen fehlen, nenne sinnvolle Annahmen. Gib bei Aufgaben nicht nur Ergebnisse, sondern auch Lernhinweise.
```

### Beispiel User Prompt für EXPLAIN

```text
Fach: Mathe
Klassenstufe: 8
Modus: Einfach erklären
Lernstoff:
Lineare Funktionen erklären

Erkläre den Stoff einfach, mit einem kurzen Beispiel und einer Mini-Zusammenfassung.
```

## Agent: LM Studio Integration

### Aufgabe

Der LM-Studio-Agent stellt sicher, dass das Backend mit dem lokalen Modell kommuniziert.

### Voraussetzungen

- LM Studio ist installiert
- ein lokales Chat-Modell ist geladen
- Local Server ist gestartet
- OpenAI-kompatible API ist aktiv
- Backend kennt die Base URL

### Empfohlene Umgebungsvariable

```text
LM_STUDIO_BASE_URL=http://localhost:1234/v1
LM_STUDIO_MODEL=local-model
```

### Chat Completion Request

```json
{
  "model": "local-model",
  "messages": [
    {
      "role": "system",
      "content": "Du bist ein lokaler KI-Lerncoach für Schüler."
    },
    {
      "role": "user",
      "content": "Erkläre lineare Funktionen einfach."
    }
  ],
  "temperature": 0.4,
  "max_tokens": 800
}
```

### Erwartete Antwortverarbeitung

Das Backend extrahiert aus der LM-Studio-Antwort den Inhalt aus:

```text
choices[0].message.content
```

## Agent: Prompt Engineering

### Aufgabe

Der Prompt-Agent definiert robuste Prompts für die einzelnen Lernmodi.

### Modus: Einfach erklären

Ziel: Schulstoff verständlich erklären.

Struktur der Antwort:

```text
Kurz erklärt:
Beispiel:
Merksatz:
Typischer Fehler:
```

### Modus: Aufgaben erstellen

Ziel: passende Übungen aus dem Stoff erzeugen.

Struktur der Antwort:

```text
Aufgaben:
1.
2.
3.

Lösungen:
1.
2.
3.

Hinweis zum Üben:
```

### Modus: Wissen testen

Ziel: kleinen Test erzeugen.

Struktur der Antwort:

```text
Mini-Test:
1.
2.
3.

Antworten erst nach dem Bearbeiten ansehen:
```

### Modus: Text korrigieren

Ziel: Schülertext verbessern.

Struktur der Antwort:

```text
Korrigierte Version:
Was verbessert wurde:
Nächster Tipp:
```

### Modus: Klausur-Crashkurs

Ziel: kurzfristiger Lernplan.

Struktur der Antwort:

```text
Lernplan:
Wichtigste Regeln:
Typische Aufgaben:
Mini-Test:
Letzte Wiederholung:
```

### Modus: Lösungsweg anzeigen

Ziel: nicht nur Ergebnis, sondern Weg erklären.

Struktur der Antwort:

```text
Gegeben:
Gesucht:
Schritt-für-Schritt-Lösung:
Warum das funktioniert:
Typischer Fehler:
```

## Agent: Demo

### Aufgabe

Der Demo-Agent bereitet einen überzeugenden Hackathon-Ablauf vor.

### Demo-Szenario 1

Input:

```text
Ich schreibe morgen eine Mathearbeit über lineare Funktionen. Ich verstehe m und b nicht.
```

Aktion:

```text
Klausur-Crashkurs
```

Erwartete Ausgabe:

- einfache Erklärung von Steigung und y-Achsenabschnitt
- Beispielaufgabe
- Mini-Test
- Lernplan für 60 Minuten

### Demo-Szenario 2

Input:

```text
Korrigiere meinen Text: Die Französische Revolution war weil arme Leute kein Brot hatten und der König schlecht war.
```

Aktion:

```text
Text korrigieren
```

Erwartete Ausgabe:

- bessere Formulierung
- Hinweis auf Ursache-Wirkung
- altersgerechter Verbesserungstipp

### Demo-Szenario 3

Input:

```text
Erstelle Aufgaben zu Prozentrechnung Klasse 7.
```

Aktion:

```text
Aufgaben erstellen
```

Erwartete Ausgabe:

- leichte, mittlere und schwere Aufgaben
- Lösungen
- kurze Hinweise

## MVP User Stories

### User Story 1

Als Schüler möchte ich ein Thema eingeben, damit die App es mir einfach erklärt.

Akzeptanzkriterien:

- Eingabefeld ist sichtbar
- Aktion „Einfach erklären“ ist auswählbar
- Backend liefert KI-Antwort zurück
- Antwort wird lesbar angezeigt

### User Story 2

Als Schüler möchte ich Aufgaben zu einem Thema erstellen lassen, damit ich üben kann.

Akzeptanzkriterien:

- Aktion „Aufgaben erstellen“ ist auswählbar
- Antwort enthält mindestens drei Aufgaben
- Antwort enthält Lösungen oder Hinweise

### User Story 3

Als Schüler möchte ich meinen Text korrigieren lassen, damit ich bessere Formulierungen lerne.

Akzeptanzkriterien:

- Aktion „Text korrigieren“ ist auswählbar
- Antwort enthält korrigierte Version
- Antwort enthält kurze Erklärung der Änderungen

### User Story 4

Als Schüler möchte ich lokal mit KI lernen, damit meine Schultexte nicht an externe Dienste gesendet werden.

Akzeptanzkriterien:

- Backend nutzt LM Studio lokal
- App funktioniert bei gestartetem LM-Studio-Server ohne Cloud-KI
- Fehlerhinweis erscheint, wenn LM Studio offline ist

## Minimaler Implementierungsplan

## Phase 1: Backend Skeleton

- Spring Boot Projekt erstellen
- `/api/learn` Endpoint erstellen
- Request und Response DTOs definieren
- statische Testantwort zurückgeben

## Phase 2: Frontend Skeleton

- Svelte Projekt erstellen
- Eingabeformular bauen
- Modus-Auswahl bauen
- Backend Request senden
- Antwort anzeigen

## Phase 3: LM Studio anbinden

- LM Studio Local Server starten
- Backend HTTP Client bauen
- PromptBuilder erstellen
- Antwort aus `choices[0].message.content` extrahieren

## Phase 4: Demo polish

- Beispielinputs vorbereiten
- Ladezustand ergänzen
- Fehlermeldung für Offline-KI ergänzen
- Ergebnis optisch als Lernkarte darstellen

## API Details

### LearnRequest

```json
{
  "mode": "EXPLAIN",
  "subject": "Mathe",
  "gradeLevel": "8",
  "input": "Lineare Funktionen erklären",
  "style": "simple"
}
```

### LearnResponse

```json
{
  "mode": "EXPLAIN",
  "title": "Lineare Funktionen einfach erklärt",
  "result": "..."
}
```

### ErrorResponse

```json
{
  "message": "LM Studio ist nicht erreichbar. Bitte starte den lokalen Server.",
  "details": "Connection refused"
}
```

## Backend-Konfigurationswerte

```properties
server.port=8080
lmstudio.base-url=${LM_STUDIO_BASE_URL:http://localhost:1234/v1}
lmstudio.model=${LM_STUDIO_MODEL:local-model}
```

## Frontend-Konfigurationswerte

```text
VITE_API_BASE_URL=http://localhost:8080
```

## Lokaler Start

### LM Studio

1. Modell in LM Studio laden.
2. Local Server starten.
3. Prüfen, ob der Server auf Port `1234` läuft.

### Backend

```bash
./mvnw spring-boot:run
```

### Frontend

```bash
npm install
npm run dev
```

## Qualitätsregeln

- Keine Cloud-KI für den MVP verwenden.
- Keine Schülerdaten an externe Dienste senden.
- Antworten sollen verständlich und nicht überladen sein.
- Die App soll im Demo-Modus stabiler wirken als sie technisch komplex ist.
- Fehler müssen klar angezeigt werden.
- Der Fokus liegt auf einem starken Lernflow.

## Nice-to-have nach dem MVP

- PDF Upload
- Foto Upload
- OCR
- lokale Vektordatenbank
- Karteikartenmodus
- Lernfortschritt speichern
- mehrere KI-Lehrer-Persönlichkeiten
- Lehrer-Dashboard
- Export als Lernzettel
- Offline-Paket für Schulen

## Hackathon Pitch

**Kick den Stoff** ist ein lokaler KI-Lerncoach für Schüler. Statt sensible Schultexte in die Cloud zu schicken, nutzt die App ein lokales Sprachmodell über LM Studio. Schüler können Lernstoff eingeben und daraus sofort Erklärungen, Übungen, Tests, Korrekturen und Klausurpläne erzeugen. Das macht KI-Lernen datenschutzfreundlicher, schulnah und auch ohne externe KI-Anbieter demonstrierbar.

## Definition of Done für den Hackathon

Der Prototyp gilt als fertig, wenn:

- Svelte-Frontend läuft
- Spring-Boot-Backend läuft
- LM Studio lokal angebunden ist
- mindestens drei Lernmodi funktionieren
- Demo mit echtem KI-Output möglich ist
- Offline- oder Verbindungsfehler verständlich angezeigt werden
- Projektidee in 2 Minuten erklärbar ist
