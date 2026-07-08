// src/lib/db.ts
const DB_NAME = 'KickDenStoffDB';
const DB_VERSION = 1;
const STORE_NAME = 'progress';

export interface ProgressRecord {
  id?: number;
  timestamp: number;
  mode: string;
  subject: string;
}

function getDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'id', autoIncrement: true });
        store.createIndex('timestamp', 'timestamp', { unique: false });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function addProgress(mode: string, subject: string): Promise<void> {
  const db = await getDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    const record: ProgressRecord = {
      timestamp: Date.now(),
      mode,
      subject
    };
    const request = store.add(record);

    request.onsuccess = () => resolve();
    request.onerror = () => reject(request.error);
  });
}

export async function getProgressLast7Days(): Promise<ProgressRecord[]> {
  const db = await getDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const index = store.index('timestamp');
    
    // 7 days ago
    const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
    const range = IDBKeyRange.lowerBound(sevenDaysAgo);
    
    const request = index.getAll(range);

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}
