import type { LearningMode, QuizPayload } from './types';

const DB_NAME = 'KIckDenStoffDB';
const DB_VERSION = 2;
const STORE_NAME = 'progress';
const LEGACY_STORAGE_KEY = 'kick-den-stoff-lernkurve-v1';
const MIGRATION_KEY = 'kick-den-stoff-indexeddb-migrated-v1';

export interface ProgressRecord {
  id: string;
  timestamp: number;
  mode: string;
  topic: string;
  isQuiz: boolean;
  quizScore: number | null;
  quizTotal: number | null;
  selfAssessment: string;
  aiAssessment: string;
  inputText: string;
  answer: string;
  title: string;
  modeLabel: string;
  pdfName: string | null;
  quiz: QuizPayload | null;
}

export type ProgressInput = Omit<ProgressRecord, 'id' | 'timestamp'> & {
  id?: string;
  timestamp?: number;
};

function createId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function modeLabel(mode: string) {
  const labels: Record<string, string> = {
    explain: 'Erklären',
    exercises: 'Üben',
    quiz: 'Quiz erstellen',
    correct: 'Korrigieren',
    'crash-course': 'Crashkurs',
    chat: 'Frage'
  };

  return labels[mode] ?? mode;
}

function normalizeRecord(input: Partial<ProgressRecord>): ProgressRecord {
  const mode = input.mode ?? 'explain';

  return {
    id: input.id ?? createId(),
    timestamp: input.timestamp ?? Date.now(),
    mode,
    topic: input.topic || 'Unbekanntes Thema',
    isQuiz: Boolean(input.isQuiz),
    quizScore: input.quizScore ?? null,
    quizTotal: input.quizTotal ?? null,
    selfAssessment: input.selfAssessment || 'Noch nicht eingeschätzt',
    aiAssessment: input.aiAssessment || 'Noch keine Gesamteinschätzung vorhanden.',
    inputText: input.inputText || '',
    answer: input.answer || '',
    title: input.title || modeLabel(mode),
    modeLabel: input.modeLabel || modeLabel(mode),
    pdfName: input.pdfName ?? null,
    quiz: input.quiz ?? null
  };
}

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = () => {
      const db = request.result;
      let store: IDBObjectStore;

      if (!db.objectStoreNames.contains(STORE_NAME)) {
        store = db.createObjectStore(STORE_NAME, { keyPath: 'id' });
      } else {
        store = request.transaction!.objectStore(STORE_NAME);
      }

      if (!store.indexNames.contains('timestamp')) {
        store.createIndex('timestamp', 'timestamp', { unique: false });
      }

      if (!store.indexNames.contains('topic')) {
        store.createIndex('topic', 'topic', { unique: false });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function requestToPromise<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function withStore<T>(
  mode: IDBTransactionMode,
  callback: (store: IDBObjectStore) => IDBRequest<T>
): Promise<T> {
  const db = await openDB();

  try {
    const transaction = db.transaction(STORE_NAME, mode);
    const store = transaction.objectStore(STORE_NAME);
    return await requestToPromise(callback(store));
  } finally {
    db.close();
  }
}

async function migrateLegacyProgress() {
  if (typeof localStorage === 'undefined' || localStorage.getItem(MIGRATION_KEY)) {
    return;
  }

  const raw = localStorage.getItem(LEGACY_STORAGE_KEY);
  if (!raw) {
    localStorage.setItem(MIGRATION_KEY, 'true');
    return;
  }

  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      localStorage.setItem(MIGRATION_KEY, 'true');
      return;
    }

    const records = parsed
      .filter((record) => Boolean(record && record.timestamp))
      .map((record) => normalizeRecord(record));

    if (!records.length) {
      localStorage.setItem(MIGRATION_KEY, 'true');
      return;
    }

    const db = await openDB();
    await new Promise<void>((resolve, reject) => {
      const transaction = db.transaction(STORE_NAME, 'readwrite');
      const store = transaction.objectStore(STORE_NAME);

      for (const record of records) {
        store.put(record);
      }

      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
    });
    db.close();
  } catch {
    // Falls alte Daten kaputt sind, startet IndexedDB leer weiter.
  } finally {
    localStorage.setItem(MIGRATION_KEY, 'true');
  }
}

export async function addProgress(input: ProgressInput): Promise<ProgressRecord> {
  await migrateLegacyProgress();
  const record = normalizeRecord({
    ...input,
    id: input.id ?? createId(),
    timestamp: input.timestamp ?? Date.now()
  });

  await withStore('readwrite', (store) => store.put(record));
  return record;
}

export async function updateProgressRecord(
  id: string,
  patch: Partial<Omit<ProgressRecord, 'id' | 'timestamp'>>
): Promise<ProgressRecord | null> {
  await migrateLegacyProgress();
  const existing = await withStore<ProgressRecord | undefined>('readonly', (store) => store.get(id));

  if (!existing) {
    return null;
  }

  const updated = normalizeRecord({
    ...existing,
    ...patch,
    id: existing.id,
    timestamp: existing.timestamp
  });

  await withStore('readwrite', (store) => store.put(updated));
  return updated;
}

export async function deleteProgressRecord(id: string): Promise<void> {
  await migrateLegacyProgress();
  await withStore('readwrite', (store) => store.delete(id));
}

export async function getProgress(): Promise<ProgressRecord[]> {
  await migrateLegacyProgress();
  const records = await withStore<ProgressRecord[]>('readonly', (store) => store.getAll());
  return records.map(normalizeRecord).sort((a, b) => a.timestamp - b.timestamp);
}

export async function getProgressLast7Days(): Promise<ProgressRecord[]> {
  const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
  return (await getProgress()).filter((record) => record.timestamp >= sevenDaysAgo);
}

export function isStoredLearningMode(mode: string): mode is LearningMode {
  return ['explain', 'exercises', 'quiz', 'correct', 'crash-course', 'chat'].includes(mode);
}
