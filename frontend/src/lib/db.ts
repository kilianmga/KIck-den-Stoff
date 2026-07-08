const STORAGE_KEY = 'kick-den-stoff-lernkurve-v1';

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

function readProgress(): ProgressRecord[] {
  if (typeof localStorage === 'undefined') {
    return [];
  }

  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return [];
    }

    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }

    return parsed
      .filter((record): record is ProgressRecord => Boolean(record && record.timestamp))
      .sort((a, b) => a.timestamp - b.timestamp);
  } catch {
    return [];
  }
}

function writeProgress(records: ProgressRecord[]) {
  if (typeof localStorage === 'undefined') {
    return;
  }

  localStorage.setItem(STORAGE_KEY, JSON.stringify(records.slice(-80)));
}

export async function addProgress(input: ProgressInput): Promise<ProgressRecord> {
  const records = readProgress();
  const record: ProgressRecord = {
    id: input.id ?? createId(),
    timestamp: input.timestamp ?? Date.now(),
    mode: input.mode,
    topic: input.topic || 'Unbekanntes Thema',
    isQuiz: input.isQuiz,
    quizScore: input.quizScore ?? null,
    quizTotal: input.quizTotal ?? null,
    selfAssessment: input.selfAssessment || 'Noch nicht eingeschätzt',
    aiAssessment: input.aiAssessment || 'Noch keine Gesamteinschätzung vorhanden.'
  };

  records.push(record);
  writeProgress(records);
  return record;
}

export async function updateProgressRecord(
  id: string,
  patch: Partial<Omit<ProgressRecord, 'id' | 'timestamp'>>
): Promise<ProgressRecord | null> {
  const records = readProgress();
  const index = records.findIndex((record) => record.id === id);

  if (index === -1) {
    return null;
  }

  records[index] = {
    ...records[index],
    ...patch
  };
  writeProgress(records);
  return records[index];
}

export async function getProgress(): Promise<ProgressRecord[]> {
  return readProgress();
}

export async function getProgressLast7Days(): Promise<ProgressRecord[]> {
  const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
  return readProgress().filter((record) => record.timestamp >= sevenDaysAgo);
}
