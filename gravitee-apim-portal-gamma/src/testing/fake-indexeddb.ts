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
type StoreRecord = Record<string, unknown>;

interface FakeDatabase {
    version: number;
    stores: Map<string, Map<string, StoreRecord>>;
}

const databases = new Map<string, FakeDatabase>();

class FakeRequest<T> {
    result: T | undefined;
    error: DOMException | null = null;
    onsuccess: ((this: FakeRequest<T>, event: Event) => void) | null = null;
    onerror: ((this: FakeRequest<T>, event: Event) => void) | null = null;
    onupgradeneeded: ((this: FakeRequest<T>, event: Event) => void) | null = null;

    succeed(result: T) {
        this.result = result;
        this.onsuccess?.call(this, new Event('success'));
    }

    fail(error: DOMException) {
        this.error = error;
        this.onerror?.call(this, new Event('error'));
    }
}

class FakeObjectStore {
    constructor(private readonly records: Map<string, StoreRecord>) {}

    get(key: string) {
        const request = new FakeRequest<StoreRecord | undefined>();
        queueMicrotask(() => request.succeed(this.records.get(key)));
        return request;
    }

    getAll() {
        const request = new FakeRequest<StoreRecord[]>();
        queueMicrotask(() => request.succeed([...this.records.values()]));
        return request;
    }

    put(value: StoreRecord) {
        const request = new FakeRequest<void>();
        queueMicrotask(() => {
            const key = String(value.id);
            this.records.set(key, value);
            request.succeed(undefined);
        });
        return request;
    }

    delete(key: string) {
        const request = new FakeRequest<void>();
        queueMicrotask(() => {
            this.records.delete(key);
            request.succeed(undefined);
        });
        return request;
    }
}

class FakeTransaction {
    oncomplete: ((this: FakeTransaction, event: Event) => void) | null = null;
    onerror: ((this: FakeTransaction, event: Event) => void) | null = null;
    error: DOMException | null = null;

    constructor(private readonly store: FakeObjectStore) {
        queueMicrotask(() => this.oncomplete?.call(this, new Event('complete')));
    }

    objectStore() {
        return this.store;
    }
}

class FakeDatabaseConnection {
    objectStoreNames = {
        contains: (name: string) => this.db.stores.has(name),
    };

    constructor(
        readonly name: string,
        private readonly db: FakeDatabase,
    ) {}

    createObjectStore(name: string, options?: { keyPath?: string }) {
        if (!options?.keyPath) {
            throw new Error('keyPath is required');
        }
        const store = new Map<string, StoreRecord>();
        this.db.stores.set(name, store);
        return new FakeObjectStore(store);
    }

    transaction(storeName: string, _mode: IDBTransactionMode) {
        const store = this.db.stores.get(storeName);
        if (!store) {
            throw new Error(`Store ${storeName} not found`);
        }
        return new FakeTransaction(new FakeObjectStore(store));
    }
}

export function installFakeIndexedDB() {
    const indexedDB = {
        open(name: string, version = 1) {
            const request = new FakeRequest<FakeDatabaseConnection>();
            const db = databases.get(name) ?? { version: 0, stores: new Map<string, Map<string, StoreRecord>>() };
            databases.set(name, db);

            queueMicrotask(() => {
                const connection = new FakeDatabaseConnection(name, db);

                if (db.version < version) {
                    request.result = connection;
                    request.onupgradeneeded?.call(request, new Event('upgradeneeded'));
                    db.version = version;
                }

                request.succeed(connection);
            });

            return request;
        },
        deleteDatabase(name: string) {
            const request = new FakeRequest<void>();
            queueMicrotask(() => {
                databases.delete(name);
                request.succeed(undefined);
            });
            return request;
        },
    };

    Object.defineProperty(globalThis, 'indexedDB', {
        configurable: true,
        value: indexedDB,
    });
}

export function resetFakeIndexedDB() {
    databases.clear();
}
