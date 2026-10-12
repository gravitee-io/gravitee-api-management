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
import { verifyCustomApiKey } from './subscriptions';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
    apimFetchJsonV1Env: jest.fn(),
    apimFetchBlobV2: jest.fn(),
}));

const mockFetch = apimFetchJsonV2 as jest.Mock;

describe('verifyCustomApiKey', () => {
    beforeEach(() => {
        mockFetch.mockReset();
        mockFetch.mockResolvedValue({ ok: true });
    });

    it('posts the application and key to the API subscription verify endpoint', async () => {
        await expect(
            verifyCustomApiKey('env-1', { type: 'api', entityId: 'api/1' }, { applicationId: 'app-1', apiKey: 'validkey1' }),
        ).resolves.toEqual({
            ok: true,
        });
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api%2F1/subscriptions/_verify', {
            method: 'POST',
            body: JSON.stringify({ applicationId: 'app-1', apiKey: 'validkey1' }),
        });
    });

    it('posts to the API product verify endpoint when the subscription belongs to an API product', async () => {
        await verifyCustomApiKey('env-1', { type: 'api-product', entityId: 'product-1' }, { applicationId: 'app-1', apiKey: 'validkey1' });
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/api-products/product-1/subscriptions/_verify', {
            method: 'POST',
            body: JSON.stringify({ applicationId: 'app-1', apiKey: 'validkey1' }),
        });
    });
});
