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
import { setupCatalogDatabaseTests } from '../../editor/services/catalog.test-utils';
import { listSubscriptions } from '../../editor/services/subscriptions.service';

describe('subscriptions IndexedDB integration', () => {
    setupCatalogDatabaseTests();

    it('should load seeded subscriptions from IndexedDB', async () => {
        const response = await listSubscriptions({ page: 1, size: 10 });
        expect(response.data).toHaveLength(5);
        expect(response.data[0].applicationName).toBe('Internal Tools');
    });
});
