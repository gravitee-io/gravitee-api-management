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
import { saveApplications } from '../../portals/storage/applications.storage';
import { createDummyApplications } from '../../portals/storage/dummy-applications';
import { createDummySubscriptions } from '../../portals/storage/dummy-subscriptions';
import { saveSubscriptions } from '../../portals/storage/subscriptions.storage';
import { DB_NAME, resetDatabaseSchemaState } from '../../portals/storage/db';

export function clearPortalsDatabase(): Promise<void> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.deleteDatabase(DB_NAME);
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
        request.onblocked = () => resolve();
    });
}

export async function seedCatalogForTests(): Promise<void> {
    await saveApplications(createDummyApplications());
    await saveSubscriptions(createDummySubscriptions());
}

export function setupCatalogDatabaseTests(): void {
    beforeEach(async () => {
        resetFakeIndexedDB();
        resetDatabaseSchemaState();
        installFakeIndexedDB();
        await clearPortalsDatabase();
        await seedCatalogForTests();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
        resetDatabaseSchemaState();
        resetFakeIndexedDB();
    });
}
