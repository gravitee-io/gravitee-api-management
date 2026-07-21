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
import { installFakeIndexedDB, resetFakeIndexedDB } from '../../../testing/fake-indexeddb';
import {
    APPLICATIONS_STORE_NAME,
    DB_NAME,
    openDB,
    PAGE_TEMPLATES_STORE_NAME,
    PORTAL_CATEGORIES_STORE_NAME,
    PORTAL_DOMAINS_STORE_NAME,
    PORTAL_IDENTITY_PROVIDERS_STORE_NAME,
    PORTAL_SUBSCRIPTION_FORMS_STORE_NAME,
    resetDatabaseSchemaState,
    SUBSCRIPTIONS_STORE_NAME,
    TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME,
} from './db';

function openDatabaseAtVersion(version: number, onUpgrade?: (db: IDBDatabase) => void): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, version);
        request.onupgradeneeded = () => {
            onUpgrade?.(request.result);
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

describe('IndexedDB migrations', () => {
    beforeEach(() => {
        resetFakeIndexedDB();
        resetDatabaseSchemaState();
        installFakeIndexedDB();
    });

    afterEach(async () => {
        resetDatabaseSchemaState();
        await new Promise<void>((resolve, reject) => {
            const request = indexedDB.deleteDatabase(DB_NAME);
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
            request.onblocked = () => resolve();
        });
        resetFakeIndexedDB();
    });

    it('should create catalog stores on fresh database', async () => {
        const db = await openDB();

        expect(db.objectStoreNames.contains(APPLICATIONS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(SUBSCRIPTIONS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PORTAL_CATEGORIES_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PORTAL_SUBSCRIPTION_FORMS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PORTAL_IDENTITY_PROVIDERS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PORTAL_DOMAINS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PAGE_TEMPLATES_STORE_NAME)).toBe(true);
    });

    it('should add missing catalog stores when upgrading from v4', async () => {
        await openDatabaseAtVersion(4, db => {
            db.createObjectStore('portals', { keyPath: 'id' });
        });

        const db = await openDB();
        expect(db.objectStoreNames.contains(APPLICATIONS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(SUBSCRIPTIONS_STORE_NAME)).toBe(true);
    });

    it('should repair a v5 database that is missing catalog stores', async () => {
        await openDatabaseAtVersion(5, db => {
            db.createObjectStore('portals', { keyPath: 'id' });
        });

        const db = await openDB();
        expect(db.objectStoreNames.contains(APPLICATIONS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(SUBSCRIPTIONS_STORE_NAME)).toBe(true);
    });

    it('should add portal settings stores when upgrading from v9', async () => {
        await openDatabaseAtVersion(9, db => {
            db.createObjectStore('portals', { keyPath: 'id' });
            db.createObjectStore('portal-consumers', { keyPath: 'id' });
            db.createObjectStore('portal-invitations', { keyPath: 'id' });
        });

        const db = await openDB();
        expect(db.objectStoreNames.contains(PORTAL_CATEGORIES_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PORTAL_SUBSCRIPTION_FORMS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PORTAL_IDENTITY_PROVIDERS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PORTAL_DOMAINS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PAGE_TEMPLATES_STORE_NAME)).toBe(true);
    });

    it('should add module config stores when upgrading from v10', async () => {
        await openDatabaseAtVersion(10, db => {
            db.createObjectStore('portals', { keyPath: 'id' });
            db.createObjectStore('portal-categories', { keyPath: 'id' });
            db.createObjectStore('portal-subscription-forms', { keyPath: 'id' });
            db.createObjectStore('portal-identity-providers', { keyPath: 'id' });
        });

        const db = await openDB();
        expect(db.objectStoreNames.contains(TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PORTAL_DOMAINS_STORE_NAME)).toBe(true);
        expect(db.objectStoreNames.contains(PAGE_TEMPLATES_STORE_NAME)).toBe(true);
    });
});
