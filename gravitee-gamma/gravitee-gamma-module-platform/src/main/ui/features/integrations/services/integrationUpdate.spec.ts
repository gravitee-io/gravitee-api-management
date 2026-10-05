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
import { updateIntegration } from './integrationUpdate';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

function sentRequest() {
    const [environmentId, path, init] = mockApimFetchJsonV2.mock.calls[0];
    return { environmentId, path, method: init?.method, body: JSON.parse(String(init?.body)) };
}

describe('integration update service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('PUTs the whole update request to the encoded integration in the environment and returns the updated integration', async () => {
        const updated = { id: 'int/1', name: 'New name', description: 'New description', provider: 'A2A' };
        mockApimFetchJsonV2.mockResolvedValue(updated);
        const request = {
            name: 'New name',
            description: 'New description',
            groups: ['Platform Team'],
            wellKnownUrls: [{ url: 'https://agent.example.com/.well-known/agent.json' }],
        };

        const result = await updateIntegration('env-1', 'int/1', request);

        expect(sentRequest()).toEqual({ environmentId: 'env-1', path: '/integrations/int%2F1', method: 'PUT', body: request });
        expect(result).toBe(updated);
    });
});
