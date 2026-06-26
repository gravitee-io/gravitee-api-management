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
    stores: Map<string, FakeObjectStore>;
}

const databases = new Map<string, FakeDatabase>();

class FakeRequest<T> {
    result: T | undefined;
    error: DOMException | null = null;
    transaction: { objectStore: (name: string) => FakeObjectStore } | null = null;
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

class FakeIndex {
    constructor(
        private readonly records: Map<string, StoreRecord>,
        private readonly keyPath: string | string[],
    ) {}

    get(query: IDBValidKey) {
        const request = new FakeRequest<StoreRecord | undefined>();
        queueMicrotask(() => {
            const found = [...this.records.values()].find(record => this.matches(record, query));
            request.succeed(found);
        });
        return request;
    }

    getAll(query?: IDBValidKey) {
        const request = new FakeRequest<StoreRecord[]>();
        queueMicrotask(() => {
            const results =
                query === undefined
                    ? [...this.records.values()]
                    : [...this.records.values()].filter(record => this.matches(record, query));
            request.succeed(results);
        });
        return request;
    }

    private matches(record: StoreRecord, query: IDBValidKey): boolean {
        if (Array.isArray(this.keyPath)) {
            const queryValues = query as IDBValidKey[];
            return this.keyPath.every((path, index) => record[path] === queryValues[index]);
        }
        return record[this.keyPath] === query;
    }
}

class FakeObjectStore {
    private readonly indexes = new Map<string, FakeIndex>();

    constructor(private readonly records: Map<string, StoreRecord>) {}

    createIndex(name: string, keyPath: string | string[]) {
        const index = new FakeIndex(this.records, keyPath);
        this.indexes.set(name, index);
        return index;
    }

    index(name: string) {
        const index = this.indexes.get(name);
        if (!index) {
            throw new Error(`Index ${name} not found`);
        }
        return index;
    }

    get indexNames() {
        return {
            contains: (name: string) => this.indexes.has(name),
        };
    }

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
        const store = new FakeObjectStore(new Map<string, StoreRecord>());
        this.db.stores.set(name, store);
        return store;
    }

    transaction(storeName: string, _mode: IDBTransactionMode) {
        const store = this.db.stores.get(storeName);
        if (!store) {
            throw new Error(`Store ${storeName} not found`);
        }
        return new FakeTransaction(store);
    }
}

export function installFakeIndexedDB() {
    const indexedDB = {
        open(name: string, version = 1) {
            const request = new FakeRequest<FakeDatabaseConnection>();
            const db = databases.get(name) ?? { version: 0, stores: new Map<string, FakeObjectStore>() };
            databases.set(name, db);

            queueMicrotask(() => {
                const connection = new FakeDatabaseConnection(name, db);

                if (db.version < version) {
                    const oldVersion = db.version;
                    request.result = connection;
                    request.transaction = {
                        objectStore: (storeName: string) => {
                            const store = db.stores.get(storeName);
                            if (!store) {
                                throw new Error(`Store ${storeName} not found`);
                            }
                            return store;
                        },
                    };
                    request.onupgradeneeded?.call(request, {
                        oldVersion,
                        newVersion: version,
                        target: request,
                    } as unknown as IDBVersionChangeEvent);
                    db.version = version;
                } else {
                    request.result = connection;
                }

                request.succeed(request.result!);
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
