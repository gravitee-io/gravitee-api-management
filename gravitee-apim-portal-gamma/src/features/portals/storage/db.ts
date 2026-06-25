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
export const DB_NAME = 'gravitee-portal-gamma';
export const DB_VERSION = 2;

export const PORTALS_STORE_NAME = 'portals';
export const NAVIGATION_ITEMS_STORE_NAME = 'navigation-items';
export const PAGE_CONTENTS_STORE_NAME = 'page-contents';

export function upgradeDatabase(db: IDBDatabase, oldVersion: number): void {
    if (!db.objectStoreNames.contains(PORTALS_STORE_NAME)) {
        db.createObjectStore(PORTALS_STORE_NAME, { keyPath: 'id' });
    }

    if (oldVersion < 2) {
        if (!db.objectStoreNames.contains(NAVIGATION_ITEMS_STORE_NAME)) {
            const navStore = db.createObjectStore(NAVIGATION_ITEMS_STORE_NAME, { keyPath: 'id' });
            navStore.createIndex('portalId', 'portalId', { unique: false });
        }

        if (!db.objectStoreNames.contains(PAGE_CONTENTS_STORE_NAME)) {
            const pageStore = db.createObjectStore(PAGE_CONTENTS_STORE_NAME, { keyPath: 'id' });
            pageStore.createIndex('portalId', 'portalId', { unique: false });
            pageStore.createIndex('navigationItemId', 'navigationItemId', { unique: true });
            pageStore.createIndex('portalId_navigationItemId', ['portalId', 'navigationItemId'], { unique: true });
        }
    }
}

export function openDB(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        request.onupgradeneeded = event => {
            upgradeDatabase(request.result, event.oldVersion);
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

export function runTransaction<T>(
    storeName: string,
    mode: IDBTransactionMode,
    run: (store: IDBObjectStore) => IDBRequest<T>,
): Promise<T> {
    return openDB().then(
        db =>
            new Promise<T>((resolve, reject) => {
                const tx = db.transaction(storeName, mode);
                const request = run(tx.objectStore(storeName));
                request.onsuccess = () => resolve(request.result);
                request.onerror = () => reject(request.error);
                tx.onerror = () => reject(tx.error);
            }),
    );
}
