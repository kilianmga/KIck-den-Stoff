# Kick den Stoff

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

## Demo-Flow

1. LM Studio starten.
2. Backend auf Port `8080` starten.
3. Frontend auf Port `5173` starten.
4. Beispiel im Frontend laden und einen Lernmodus ausfuehren.
