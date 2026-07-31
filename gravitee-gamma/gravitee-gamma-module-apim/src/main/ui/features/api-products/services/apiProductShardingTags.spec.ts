/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { http, HttpResponse } from 'msw';

import { updateApiProductShardingTags } from './apiProduct';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

const PRODUCT_PATH = `${TEST_V2_BASE}/api-products/:productId`;

describe('updateApiProductShardingTags', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('reads the current product, then PUTs it back with the new tags while preserving other fields', async () => {
        // Real server-like state: GET returns the existing product, PUT echoes the body back.
        server.use(
            http.get(PRODUCT_PATH, () =>
                HttpResponse.json({
                    id: 'prod-1',
                    name: 'Payments',
                    version: '1.0.0',
                    description: 'Keep me',
                    apiIds: ['api-a', 'api-b'],
                    tags: ['old-tag'],
                }),
            ),
        );
        const putTracker = trackHandler('put', PRODUCT_PATH, { ok: true });

        await updateApiProductShardingTags('DEFAULT', 'prod-1', ['public', 'partner']);

        expect(putTracker.callCount).toBe(1);
        const url = new URL(putTracker.lastCall!.url);
        expect(url.pathname).toContain('/api-products/prod-1');
        // Tags replaced, all other fields carried over untouched — guards against the classic
        // GET-then-PUT pitfall of dropping fields the form did not load.
        expect(putTracker.lastCall?.body).toEqual({
            id: 'prod-1',
            name: 'Payments',
            version: '1.0.0',
            description: 'Keep me',
            apiIds: ['api-a', 'api-b'],
            tags: ['public', 'partner'],
        });
    });

    it('clears all tags when given an empty list', async () => {
        server.use(http.get(PRODUCT_PATH, () => HttpResponse.json({ id: 'prod-1', name: 'P', version: '1', tags: ['a', 'b'] })));
        const putTracker = trackHandler('put', PRODUCT_PATH, { ok: true });

        await updateApiProductShardingTags('DEFAULT', 'prod-1', []);

        expect((putTracker.lastCall?.body as { tags: string[] }).tags).toEqual([]);
    });
});
