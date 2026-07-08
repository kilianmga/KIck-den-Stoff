# KIck den Stoff

Lokaler KI-Lerncoach fuer Schulstoff. Das Svelte-Frontend spricht mit einem Spring-Boot-Backend, das LM Studio ueber die OpenAI-kompatible lokale API anfragt.

## Struktur

```text
backend/   Spring Boot API fuer LM Studio
frontend/  Svelte + TypeScript + Vite UI
```

## Voraussetzungen

- Java 25
- Node.js 24+
- LM Studio mit geladenem Chat-Modell und gestartetem Local Server

## LM Studio

Standardwerte:

```text
LM_STUDIO_BASE_URL=http://localhost:1234/v1
LM_STUDIO_MODEL=local-model
```

`local-model` nutzt in LM Studio normalerweise das aktuell geladene Chat-Modell. Wenn du gezielt ein bestimmtes Modell setzen willst, nimm den Namen aus `GET /v1/models` oder aus der LM-Studio-Modellanzeige:

```bash
LM_STUDIO_MODEL="dein/modellname" ./gradlew bootRun
```

## Backend starten

```bash
cd backend
./gradlew bootRun
```

Healthcheck:

```bash
curl http://localhost:8080/api/health
```

## Frontend starten

```bash
cd frontend
npm install
npm run dev
```

Optional:

```text
VITE_BACKEND_BASE_URL=http://localhost:8080
```

## Im lokalen Netzwerk nutzen

Backend und Frontend sind fuer den Zugriff von anderen Geraeten im selben Netzwerk vorbereitet.

Start:

```bash
cd backend
./gradlew bootRun
```

```bash
cd frontend
npm run dev -- --host 0.0.0.0
```

Dann auf dem anderen Geraet im Browser oeffnen:

```text
http://DEINE-MAC-IP:5173
```

Das Frontend ruft automatisch `http://DEINE-MAC-IP:8080` als Backend auf. Wenn du eine Domain, HTTPS oder einen Tunnel nutzt, setze die Backend-URL explizit:

```bash
VITE_BACKEND_BASE_URL=https://deine-backend-url npm run dev -- --host 0.0.0.0
```

Fuer eine andere Frontend-Origin kannst du CORS beim Backend uebersteuern:

```bash
FRONTEND_ALLOWED_ORIGIN_PATTERNS="https://deine-domain.de" ./gradlew bootRun
```

## Demo-Flow

1. LM Studio starten.
2. Backend auf Port `8080` starten.
3. Frontend auf Port `5173` starten.
4. Beispiel im Frontend laden und einen Lernmodus ausfuehren.
