import type { LearningMode, LearningRequest, LearningResponse } from './types';

const endpointByMode: Record<LearningMode, string> = {
  explain: '/api/learn/explain',
  exercises: '/api/learn/exercises',
  quiz: '/api/learn/quiz',
  correct: '/api/learn/correct',
  'crash-course': '/api/learn/crash-course',
  chat: '/api/learn/chat'
};

const backendBaseUrl = import.meta.env.VITE_BACKEND_BASE_URL ?? 'http://10.39.4.137:8080';

export async function sendLearningRequest(
  mode: LearningMode,
  payload: LearningRequest
): Promise<LearningResponse> {
  const endpoint = endpointByMode[mode];

  let response: Response;
  try {
    response = await fetch(`${backendBaseUrl}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });
  } catch {
    throw new Error('Backend nicht erreichbar. Bitte prüfe, ob Spring Boot auf Port 8080 läuft.');
  }

  let data: LearningResponse | null = null;
  try {
    data = (await response.json()) as LearningResponse;
  } catch {
    throw new Error('Das Backend hat keine lesbare Antwort zurückgegeben.');
  }

  if (!response.ok) {
    throw new Error(data?.error ?? `Backend-Fehler ${response.status}.`);
  }

  return data;
}
