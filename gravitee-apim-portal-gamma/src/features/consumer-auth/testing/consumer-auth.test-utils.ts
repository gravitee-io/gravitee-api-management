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
import { DB_NAME, resetDatabaseSchemaState } from '../../portals/storage/db';
import { createDummyPortalTenants } from '../../tenants/storage/dummy-portal-tenants';
import { savePortalTenant } from '../../tenants/storage/portal-tenants.storage';
import { resetDemoConsumerSeedState } from '../storage/seed-demo-consumer';

export const TEST_PORTAL_ID = 'portal-payments';

export function clearPortalsDatabase(): Promise<void> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.deleteDatabase(DB_NAME);
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
        request.onblocked = () => resolve();
    });
}

export async function seedTenantForAuthTests(): Promise<void> {
    const tenants = createDummyPortalTenants(TEST_PORTAL_ID);
    await Promise.all(tenants.map(tenant => savePortalTenant(tenant)));
}

export function setupConsumerAuthDatabaseTests(): void {
    beforeEach(async () => {
        resetFakeIndexedDB();
        resetDatabaseSchemaState();
        resetDemoConsumerSeedState();
        installFakeIndexedDB();
        sessionStorage.clear();
        await clearPortalsDatabase();
        await seedTenantForAuthTests();
    });

    afterEach(async () => {
        sessionStorage.clear();
        resetDemoConsumerSeedState();
        await clearPortalsDatabase();
        resetDatabaseSchemaState();
        resetFakeIndexedDB();
    });
}
