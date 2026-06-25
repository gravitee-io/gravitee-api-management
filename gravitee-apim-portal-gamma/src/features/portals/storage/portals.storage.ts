/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { DeveloperPortal } from '../types';
import { createDummyPortals } from './dummy-portals';

export const DB_NAME = 'gravitee-portal-gamma';
export const DB_VERSION = 1;
export const STORE_NAME = 'portals';

function openDB(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        request.onupgradeneeded = () => {
            const db = request.result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                db.createObjectStore(STORE_NAME, { keyPath: 'id' });
            }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

function runTransaction<T>(mode: IDBTransactionMode, run: (store: IDBObjectStore) => IDBRequest<T>): Promise<T> {
    return openDB().then(
        db =>
            new Promise<T>((resolve, reject) => {
                const tx = db.transaction(STORE_NAME, mode);
                const request = run(tx.objectStore(STORE_NAME));
                request.onsuccess = () => resolve(request.result);
                request.onerror = () => reject(request.error);
                tx.onerror = () => reject(tx.error);
            }),
    );
}

export async function getAllPortals(): Promise<DeveloperPortal[]> {
    return runTransaction('readonly', store => store.getAll());
}

export async function getPortal(id: string): Promise<DeveloperPortal | undefined> {
    return runTransaction('readonly', store => store.get(id));
}

export async function savePortal(portal: DeveloperPortal): Promise<void> {
    await runTransaction('readwrite', store => store.put(portal));
}

export async function deletePortal(id: string): Promise<void> {
    await runTransaction('readwrite', store => store.delete(id));
}

export async function seedPortalsIfEmpty(): Promise<DeveloperPortal[]> {
    const existing = await getAllPortals();
    if (existing.length > 0) {
        return existing;
    }

    const dummyPortals = createDummyPortals();
    await Promise.all(dummyPortals.map(portal => savePortal(portal)));
    return dummyPortals;
}
