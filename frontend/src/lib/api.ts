import type { LearningMode, LearningRequest, LearningResponse } from './types';

const endpointByMode: Record<LearningMode, string> = {
  explain: '/api/learn/explain',
  exercises: '/api/learn/exercises',
  quiz: '/api/learn/quiz',
  correct: '/api/learn/correct',
  'crash-course': '/api/learn/crash-course',
  chat: '/api/learn/chat'
};

function resolveBackendBaseUrl() {
  const configuredBaseUrl =
    import.meta.env.VITE_API_BASE_URL ?? import.meta.env.VITE_BACKEND_BASE_URL;
  if (configuredBaseUrl) {
    return configuredBaseUrl;
  }

  if (typeof window === 'undefined') {
    return 'http://localhost:8080';
  }

  return `${window.location.protocol}//${window.location.hostname}:8080`;
}

const backendBaseUrl = resolveBackendBaseUrl();

export async function sendLearningRequest(
  mode: LearningMode,
  payload: LearningRequest,
  pdfFile?: File | null
): Promise<LearningResponse> {
  const endpoint = endpointByMode[mode];
  const body = pdfFile ? createPdfFormData(payload, pdfFile) : JSON.stringify(payload);
  const headers = pdfFile ? undefined : { 'Content-Type': 'application/json' };

  let response: Response;
  try {
    response = await fetch(`${backendBaseUrl}${endpoint}`, {
      method: 'POST',
      headers,
      body
    });
  } catch {
    throw new Error('Backend nicht erreichbar. Bitte pruefe, ob Spring Boot auf Port 8080 laeuft.');
  }

  let data: LearningResponse | null = null;
  try {
    data = (await response.json()) as LearningResponse;
  } catch {
    throw new Error('Das Backend hat keine lesbare Antwort zurueckgegeben.');
  }

  if (!response.ok) {
    throw new Error(data?.error ?? `Backend-Fehler ${response.status}.`);
  }

  return data;
}

function createPdfFormData(payload: LearningRequest, pdfFile: File) {
  const formData = new FormData();

  Object.entries(payload).forEach(([key, value]) => {
    if (value) {
      formData.append(key, value);
    }
  });

  formData.append('pdf', pdfFile, pdfFile.name);
  return formData;
}
