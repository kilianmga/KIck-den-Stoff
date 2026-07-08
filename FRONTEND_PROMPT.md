# Frontend Prompt — Kick den Stoff

Du übernimmst das Frontend für eine Hackathon-App namens **Kick den Stoff**.

## Ziel

Baue ein Svelte-Frontend für eine lokale KI-Lernapp. Schüler sollen Schulstoff eingeben und per Button eine KI-Antwort vom Spring-Boot-Backend bekommen.

Die App nutzt lokal LM Studio über das Backend. Das Frontend spricht niemals direkt mit LM Studio, sondern nur mit dem Backend.

## Tech Stack

- Svelte oder SvelteKit
- TypeScript, falls im Projekt eingerichtet
- Vite
- Fetch API
- Kein komplexes State-Management im MVP
- Keine Authentifizierung im MVP

## Backend API

Das Backend läuft lokal auf:

```env
VITE_BACKEND_BASE_URL=http://localhost:8080
```

Alle Requests gehen an:

```txt
http://localhost:8080/api/learn/...
```

## Hauptfunktionen

Die Oberfläche soll diese Aktionen anbieten:

```txt
Einfach erklären
Aufgaben erstellen
Wissen testen
Text korrigieren
Klausur-Crashkurs
Freie Frage
```

Diese Buttons mappen auf Backend-Endpunkte:

```txt
Einfach erklären      -> POST /api/learn/explain
Aufgaben erstellen   -> POST /api/learn/exercises
Wissen testen         -> POST /api/learn/quiz
Text korrigieren      -> POST /api/learn/correct
Klausur-Crashkurs     -> POST /api/learn/crash-course
Freie Frage           -> POST /api/learn/chat
```

## Layout

Baue eine einfache, demo-taugliche Oberfläche:

```txt
Header:
  Kick den Stoff
  Lokaler KI-Lerncoach mit LM Studio

Linke oder obere Eingabe-Sektion:
  Fach
  Klasse
  Thema
  Schulstoff / Material
  Frage oder Wunsch

Button-Zeile:
  Einfach erklären
  Aufgaben erstellen
  Wissen testen
  Text korrigieren
  Klausur-Crashkurs
  Freie Frage

Antwort-Sektion:
  Ladezustand
  Fehleranzeige
  KI-Antwort
```

## Request-Format

Das Frontend soll dieses JSON an das Backend senden:

```json
{
  "subject": "Mathe",
  "grade": "8",
  "topic": "Lineare Funktionen",
  "inputText": "Eine lineare Funktion hat die Form y = mx + b.",
  "userQuestion": "Erklär mir das einfach."
}
```

## Response-Format

Das Backend antwortet so:

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
  "error": "LM Studio ist nicht erreichbar."
}
```

## UI-Verhalten

Beim Klick auf einen Button:

```txt
1. Button-Modus setzen
2. Loading anzeigen
3. Request an Backend schicken
4. Antwort anzeigen
5. Fehler anzeigen, falls success false ist oder fetch fehlschlägt
6. Loading beenden
```

Während Loading:

```txt
KI denkt lokal...
```

Bei Backend-Fehler:

```txt
Backend nicht erreichbar. Bitte prüfe, ob Spring Boot auf Port 8080 läuft.
```

Bei LM-Studio-Fehler:

```txt
LM Studio ist nicht erreichbar. Bitte Modell laden und Local Server starten.
```

## Beispiel-Daten für Demo

Fülle die Felder optional mit einem Demo-Beispiel vor:

```txt
Fach: Mathe
Klasse: 8
Thema: Lineare Funktionen
Schulstoff: Eine lineare Funktion hat die Form y = mx + b. m ist die Steigung und b der y-Achsenabschnitt.
Frage: Erklär mir das so, dass ich es vor einer Klassenarbeit verstehe.
```

## Komponenten-Vorschlag

Struktur:

```txt
frontend/
  src/
    App.svelte
    lib/
      api.ts
      types.ts
      ModeButton.svelte
      ResponseBox.svelte
```

Für den Hackathon darf alles auch in `App.svelte` sein, wenn es schneller geht.

## Types

Nutze diese Typen, falls TypeScript aktiv ist:

```ts
export type LearningMode = 'explain' | 'exercises' | 'quiz' | 'correct' | 'crash-course' | 'chat';

export type LearningRequest = {
  subject: string;
  grade: string;
  topic: string;
  inputText: string;
  userQuestion: string;
};

export type LearningResponse = {
  mode: string;
  answer: string | null;
  success: boolean;
  error: string | null;
};
```

## API-Funktion

Erstelle eine zentrale Funktion, die je nach Modus den passenden Endpoint aufruft:

```ts
const endpointByMode = {
  explain: '/api/learn/explain',
  exercises: '/api/learn/exercises',
  quiz: '/api/learn/quiz',
  correct: '/api/learn/correct',
  'crash-course': '/api/learn/crash-course',
  chat: '/api/learn/chat'
};
```

Die Funktion soll bei HTTP-Fehlern eine verständliche Fehlermeldung werfen.

## Design-Richtung

Die App soll modern und einfach aussehen:

- Großer Titel
- Klare Eingabefelder
- Große Buttons
- Antwort als Karte
- Mobile-tauglich
- Keine überladene Oberfläche

Mögliche Texte:

```txt
Kick den Stoff
Dein lokaler KI-Lerncoach
Schulstoff rein, Verständnis raus.
```

## Akzeptanzkriterien

Das Frontend ist fertig, wenn:

- `npm install` funktioniert
- `npm run dev` funktioniert
- Die App auf `http://localhost:5173` erreichbar ist
- Man Fach, Klasse, Thema, Material und Frage eingeben kann
- Mindestens der Button „Einfach erklären“ funktioniert
- Die Antwort vom Backend angezeigt wird
- Ladezustand sichtbar ist
- Fehler verständlich angezeigt werden
- Kein direkter Request an LM Studio gemacht wird

## Priorität im Hackathon

Baue zuerst nur diesen Flow:

```txt
Text eingeben -> Einfach erklären klicken -> Backend antwortet -> Antwort anzeigen
```

Danach die restlichen Buttons anschließen. Alle Buttons können am Anfang denselben UI-Aufbau verwenden und nur unterschiedliche Endpoints aufrufen.

## Demo-Szenario

Für die Präsentation soll folgender Ablauf funktionieren:

```txt
1. App öffnen
2. Mathe-Beispiel ist schon eingetragen
3. Button „Einfach erklären“ klicken
4. Antwort erscheint
5. Button „Aufgaben erstellen“ klicken
6. Aufgaben erscheinen
7. Kurz erklären, dass die KI lokal über LM Studio läuft
```
