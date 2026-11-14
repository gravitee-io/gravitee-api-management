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
import { updateApiProductResources } from './apiProduct';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../testing/factories';
import { respondWith, trackHandler } from '../../../testing/helpers';
import type { ApiResource } from '../../apis/types/resource';

const PRODUCT_PATH = `${TEST_V2_BASE}/api-products/:productId`;

const CURRENT_PRODUCT = {
    id: 'product-1',
    name: 'Payments',
    version: '1.0',
    apiIds: ['api-1'],
    resources: [{ name: 'old-cache', type: 'cache', enabled: true, configuration: { timeToLiveSeconds: 30 } }],
};

describe('updateApiProductResources', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    const NEW_RESOURCES: ApiResource[] = [{ name: 'session-cache', type: 'cache', enabled: true, configuration: { ttl: 120 } }];

    it('PUTs the full product with only the resources list replaced', async () => {
        respondWith('get', PRODUCT_PATH, CURRENT_PRODUCT);
        const put = trackHandler('put', PRODUCT_PATH, { ...CURRENT_PRODUCT, resources: NEW_RESOURCES });

        await updateApiProductResources('DEFAULT', 'product-1', () => NEW_RESOURCES);

        expect(put.callCount).toBe(1);
        const body = put.lastCall!.body as Record<string, unknown>;
        expect(body.name).toBe('Payments');
        expect(body.apiIds).toEqual(['api-1']);
        expect(body.resources).toEqual(NEW_RESOURCES);
        expect(body).not.toHaveProperty('groups');
        expect(body).not.toHaveProperty('tags');
        expect(body).toEqual(
            expect.objectContaining({
                name: 'Payments',
                version: '1.0',
                apiIds: ['api-1'],
                resources: NEW_RESOURCES,
            }),
        );
    });

    it('issues exactly one GET then one PUT', async () => {
        const get = trackHandler('get', PRODUCT_PATH, CURRENT_PRODUCT);
        const put = trackHandler('put', PRODUCT_PATH, CURRENT_PRODUCT);

        await updateApiProductResources('DEFAULT', 'product-1', () => NEW_RESOURCES);

        expect(get.callCount).toBe(1);
        expect(put.callCount).toBe(1);
    });

    it('sends the new resources list even when GET omits the resources field', async () => {
        const productWithoutResources = {
            id: 'product-1',
            name: 'Payments',
            version: '1.0',
            apiIds: ['api-1'],
        };
        respondWith('get', PRODUCT_PATH, productWithoutResources);
        const put = trackHandler('put', PRODUCT_PATH, { ...productWithoutResources, resources: NEW_RESOURCES });

        await updateApiProductResources('DEFAULT', 'product-1', () => NEW_RESOURCES);

        expect((put.lastCall!.body as { resources: ApiResource[] }).resources).toEqual(NEW_RESOURCES);
    });

    it('forwards analytics from GET so reporter settings are not cleared on PUT', async () => {
        const analytics = { enabled: true, logging: { content: { headers: true } } };
        respondWith('get', PRODUCT_PATH, { ...CURRENT_PRODUCT, analytics });
        const put = trackHandler('put', PRODUCT_PATH, { ...CURRENT_PRODUCT, analytics, resources: NEW_RESOURCES });

        await updateApiProductResources('DEFAULT', 'product-1', () => NEW_RESOURCES);

        expect((put.lastCall!.body as { analytics: typeof analytics }).analytics).toEqual(analytics);
    });

    it('merges edits with resources returned by GET so concurrent additions are kept', async () => {
        const serverResources: ApiResource[] = [
            { name: 'cache-a', type: 'cache', enabled: true, configuration: {} },
            { name: 'cache-b', type: 'cache', enabled: true, configuration: {} },
        ];
        respondWith('get', PRODUCT_PATH, { ...CURRENT_PRODUCT, resources: serverResources });
        const put = trackHandler('put', PRODUCT_PATH, { ...CURRENT_PRODUCT, resources: serverResources });

        await updateApiProductResources('DEFAULT', 'product-1', current =>
            current.map(r => (r.name === 'cache-a' ? { ...r, enabled: false } : r)),
        );

        const body = put.lastCall!.body as { resources: ApiResource[] };
        expect(body.resources).toHaveLength(2);
        expect(body.resources.find(r => r.name === 'cache-b')).toMatchObject({ enabled: true });
        expect(body.resources.find(r => r.name === 'cache-a')?.enabled).toBe(false);
    });
});
