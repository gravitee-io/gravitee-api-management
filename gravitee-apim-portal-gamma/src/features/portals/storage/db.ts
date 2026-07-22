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
// Re-exported by gravitee-gamma-module-apim — both apps share this IndexedDB schema.
export const DB_VERSION = 11;

export const PORTALS_STORE_NAME = 'portals';
export const NAVIGATION_ITEMS_STORE_NAME = 'navigation-items';
export const PAGE_CONTENTS_STORE_NAME = 'page-contents';
export const APPLICATIONS_STORE_NAME = 'applications';
export const SUBSCRIPTIONS_STORE_NAME = 'subscriptions';
export const THEMES_STORE_NAME = 'portal-themes';
export const PORTAL_TENANTS_STORE_NAME = 'portal-tenants';
export const PORTAL_TENANT_MEMBERS_STORE_NAME = 'portal-tenant-members';
export const PORTAL_CONSUMERS_STORE_NAME = 'portal-consumers';
export const PORTAL_INVITATIONS_STORE_NAME = 'portal-invitations';
export const PORTAL_CATEGORIES_STORE_NAME = 'portal-categories';
export const PORTAL_SUBSCRIPTION_FORMS_STORE_NAME = 'portal-subscription-forms';
export const PORTAL_IDENTITY_PROVIDERS_STORE_NAME = 'portal-identity-providers';
export const TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME = 'transversal-identity-providers';
export const PORTAL_DOMAINS_STORE_NAME = 'portal-domains';
export const PAGE_TEMPLATES_STORE_NAME = 'page-templates';

const REQUIRED_OBJECT_STORES = [
    PORTALS_STORE_NAME,
    NAVIGATION_ITEMS_STORE_NAME,
    PAGE_CONTENTS_STORE_NAME,
    APPLICATIONS_STORE_NAME,
    SUBSCRIPTIONS_STORE_NAME,
    THEMES_STORE_NAME,
    PORTAL_TENANTS_STORE_NAME,
    PORTAL_TENANT_MEMBERS_STORE_NAME,
    PORTAL_CONSUMERS_STORE_NAME,
    PORTAL_INVITATIONS_STORE_NAME,
    PORTAL_CATEGORIES_STORE_NAME,
    PORTAL_SUBSCRIPTION_FORMS_STORE_NAME,
    PORTAL_IDENTITY_PROVIDERS_STORE_NAME,
    TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME,
    PORTAL_DOMAINS_STORE_NAME,
    PAGE_TEMPLATES_STORE_NAME,
] as const;

function closeDatabase(db: IDBDatabase): void {
    db.close?.();
}

function ensureCatalogObjectStores(db: IDBDatabase): void {
    if (!db.objectStoreNames.contains(APPLICATIONS_STORE_NAME)) {
        db.createObjectStore(APPLICATIONS_STORE_NAME, { keyPath: 'id' });
    }

    if (!db.objectStoreNames.contains(SUBSCRIPTIONS_STORE_NAME)) {
        const subStore = db.createObjectStore(SUBSCRIPTIONS_STORE_NAME, { keyPath: 'id' });
        subStore.createIndex('api', 'api', { unique: false });
        subStore.createIndex('application', 'application', { unique: false });
        subStore.createIndex('status', 'status', { unique: false });
    }
}

function ensureNavigationAndPageStores(db: IDBDatabase): void {
    if (!db.objectStoreNames.contains(NAVIGATION_ITEMS_STORE_NAME)) {
        const navStore = db.createObjectStore(NAVIGATION_ITEMS_STORE_NAME, { keyPath: 'id' });
        navStore.createIndex('portalId', 'portalId', { unique: false });
        navStore.createIndex('portalId_slug', ['portalId', 'slug'], { unique: true });
    }

    if (!db.objectStoreNames.contains(PAGE_CONTENTS_STORE_NAME)) {
        const pageStore = db.createObjectStore(PAGE_CONTENTS_STORE_NAME, { keyPath: 'id' });
        pageStore.createIndex('portalId', 'portalId', { unique: false });
        pageStore.createIndex('navigationItemId', 'navigationItemId', { unique: true });
        pageStore.createIndex('portalId_navigationItemId', ['portalId', 'navigationItemId'], { unique: true });
    }
}

export function upgradeDatabase(db: IDBDatabase, oldVersion: number, transaction?: IDBTransaction): void {
    if (!db.objectStoreNames.contains(PORTALS_STORE_NAME)) {
        db.createObjectStore(PORTALS_STORE_NAME, { keyPath: 'id' });
    }

    if (oldVersion < 2) {
        ensureNavigationAndPageStores(db);
    }

    if (oldVersion < 3 && transaction && db.objectStoreNames.contains(NAVIGATION_ITEMS_STORE_NAME)) {
        const navStore = transaction.objectStore(NAVIGATION_ITEMS_STORE_NAME);
        if (!navStore.indexNames.contains('portalId_slug')) {
            navStore.createIndex('portalId_slug', ['portalId', 'slug'], { unique: true });
        }
    }

    if (oldVersion < 4) {
        ensureCatalogObjectStores(db);
    }

    if (oldVersion < 5) {
        ensureCatalogObjectStores(db);
    }

    if (oldVersion < 6) {
        ensureCatalogObjectStores(db);
        ensureNavigationAndPageStores(db);
    }

    if (oldVersion < 7) {
        if (!db.objectStoreNames.contains(THEMES_STORE_NAME)) {
            db.createObjectStore(THEMES_STORE_NAME, { keyPath: 'id' });
        }
    }

    if (oldVersion < 8) {
        if (!db.objectStoreNames.contains(PORTAL_TENANTS_STORE_NAME)) {
            const tenantStore = db.createObjectStore(PORTAL_TENANTS_STORE_NAME, { keyPath: 'id' });
            tenantStore.createIndex('portalId', 'portalId', { unique: false });
        }

        if (!db.objectStoreNames.contains(PORTAL_TENANT_MEMBERS_STORE_NAME)) {
            const memberStore = db.createObjectStore(PORTAL_TENANT_MEMBERS_STORE_NAME, { keyPath: 'id' });
            memberStore.createIndex('tenantId', 'tenantId', { unique: false });
            memberStore.createIndex('userId', 'userId', { unique: false });
        }
    }

    if (oldVersion < 9) {
        if (!db.objectStoreNames.contains(PORTAL_CONSUMERS_STORE_NAME)) {
            const consumerStore = db.createObjectStore(PORTAL_CONSUMERS_STORE_NAME, { keyPath: 'id' });
            consumerStore.createIndex('portalId', 'portalId', { unique: false });
            consumerStore.createIndex('email', 'email', { unique: false });
            consumerStore.createIndex('portalId_email', ['portalId', 'email'], { unique: true });
        }

        if (!db.objectStoreNames.contains(PORTAL_INVITATIONS_STORE_NAME)) {
            const invitationStore = db.createObjectStore(PORTAL_INVITATIONS_STORE_NAME, { keyPath: 'id' });
            invitationStore.createIndex('token', 'token', { unique: true });
            invitationStore.createIndex('tenantId', 'tenantId', { unique: false });
            invitationStore.createIndex('portalId', 'portalId', { unique: false });
        }
    }

    if (oldVersion < 10) {
        ensurePortalSettingsStores(db);
    }

    if (oldVersion < 11) {
        ensureModuleConfigStores(db);
    }
}

function ensurePortalSettingsStores(db: IDBDatabase): void {
    if (!db.objectStoreNames.contains(PORTAL_CATEGORIES_STORE_NAME)) {
        const categoriesStore = db.createObjectStore(PORTAL_CATEGORIES_STORE_NAME, { keyPath: 'id' });
        categoriesStore.createIndex('portalId', 'portalId', { unique: false });
    }

    if (!db.objectStoreNames.contains(PORTAL_SUBSCRIPTION_FORMS_STORE_NAME)) {
        const formsStore = db.createObjectStore(PORTAL_SUBSCRIPTION_FORMS_STORE_NAME, { keyPath: 'id' });
        formsStore.createIndex('portalId', 'portalId', { unique: false });
    }

    if (!db.objectStoreNames.contains(PORTAL_IDENTITY_PROVIDERS_STORE_NAME)) {
        const idpStore = db.createObjectStore(PORTAL_IDENTITY_PROVIDERS_STORE_NAME, { keyPath: 'id' });
        idpStore.createIndex('portalId', 'portalId', { unique: false });
    }
}

function ensureModuleConfigStores(db: IDBDatabase): void {
    if (!db.objectStoreNames.contains(TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME)) {
        db.createObjectStore(TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME, { keyPath: 'id' });
    }

    if (!db.objectStoreNames.contains(PORTAL_DOMAINS_STORE_NAME)) {
        const domainsStore = db.createObjectStore(PORTAL_DOMAINS_STORE_NAME, { keyPath: 'id' });
        domainsStore.createIndex('portalId', 'portalId', { unique: false });
        domainsStore.createIndex('hostname', 'hostname', { unique: true });
    }

    if (!db.objectStoreNames.contains(PAGE_TEMPLATES_STORE_NAME)) {
        db.createObjectStore(PAGE_TEMPLATES_STORE_NAME, { keyPath: 'id' });
    }
}

function hasAllObjectStores(db: IDBDatabase): boolean {
    return REQUIRED_OBJECT_STORES.every(name => db.objectStoreNames.contains(name));
}

function deleteDatabase(): Promise<void> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.deleteDatabase(DB_NAME);
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
        request.onblocked = () => resolve();
    });
}

function openDatabaseAtVersion(version: number): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, version);
        request.onupgradeneeded = event => {
            upgradeDatabase(request.result, event.oldVersion, request.transaction ?? undefined);
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

async function openDatabaseWithRepair(allowRepair: boolean): Promise<IDBDatabase> {
    const db = await openDatabaseAtVersion(DB_VERSION);

    if (hasAllObjectStores(db)) {
        return db;
    }

    closeDatabase(db);

    if (!allowRepair) {
        throw new Error(`IndexedDB "${DB_NAME}" is missing required object stores`);
    }

    await deleteDatabase();
    const repaired = await openDatabaseAtVersion(DB_VERSION);

    if (!hasAllObjectStores(repaired)) {
        closeDatabase(repaired);
        throw new Error(`IndexedDB "${DB_NAME}" could not be repaired`);
    }

    return repaired;
}

let schemaReady = false;
let schemaReadyPromise: Promise<void> | null = null;

async function ensureSchemaReady(): Promise<void> {
    if (schemaReady) {
        return;
    }

    if (!schemaReadyPromise) {
        schemaReadyPromise = (async () => {
            const db = await openDatabaseWithRepair(true);
            closeDatabase(db);
            schemaReady = true;
        })().catch(error => {
            schemaReadyPromise = null;
            throw error;
        });
    }

    await schemaReadyPromise;
}

export async function openDB(): Promise<IDBDatabase> {
    await ensureSchemaReady();
    return openDatabaseAtVersion(DB_VERSION);
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

                const finish = (result: T) => {
                    closeDatabase(db);
                    resolve(result);
                };

                const fail = (error: unknown) => {
                    closeDatabase(db);
                    reject(error);
                };

                request.onsuccess = () => finish(request.result);
                request.onerror = () => fail(request.error);
                tx.onerror = () => fail(tx.error);
            }),
    );
}

/** Test helper — reset cached schema state between specs. */
export function resetDatabaseSchemaState(): void {
    schemaReady = false;
    schemaReadyPromise = null;
}
