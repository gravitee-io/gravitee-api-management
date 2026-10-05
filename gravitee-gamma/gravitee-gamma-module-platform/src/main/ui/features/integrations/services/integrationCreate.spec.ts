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

import { createIntegration } from './integrationCreate';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

function sentRequest() {
    const [environmentId, path, init] = mockApimFetchJsonV2.mock.calls[0];
    return { environmentId, path, method: init?.method, body: JSON.parse(String(init?.body)) };
}

describe('integration create service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2.mockResolvedValue({ id: 'int-1', name: 'My integration', provider: 'solace' });
    });

    it('POSTs only the name and the unchanged provider token to the environment integrations', async () => {
        await createIntegration('env-1', { name: 'My integration', provider: 'solace' });

        expect(sentRequest()).toEqual({
            environmentId: 'env-1',
            path: '/integrations',
            method: 'POST',
            body: { name: 'My integration', provider: 'solace' },
        });
    });

    it.each([
        {
            description: 'Ingests the EU gateways',
            expectedBody: { name: 'My integration', provider: 'apigee', description: 'Ingests the EU gateways' },
        },
        { description: '', expectedBody: { name: 'My integration', provider: 'apigee' } },
    ])('sends the description only when it is not empty (description "$description")', async ({ description, expectedBody }) => {
        await createIntegration('env-1', { name: 'My integration', provider: 'apigee', description });

        expect(sentRequest().body).toEqual(expectedBody);
    });

    it('sends each well-known URL as a url object, unchanged and in the entered order', async () => {
        await createIntegration('env-1', {
            name: 'Billing Agents',
            description: 'Invoice agents',
            provider: 'A2A',
            wellKnownUrls: [
                'https://search.example.com/.well-known/agent-card.json',
                'https://billing.example.com/agents/.well-known/agent.json',
            ],
        });

        expect(sentRequest().body).toEqual({
            name: 'Billing Agents',
            description: 'Invoice agents',
            provider: 'A2A',
            wellKnownUrls: [
                { url: 'https://search.example.com/.well-known/agent-card.json' },
                { url: 'https://billing.example.com/agents/.well-known/agent.json' },
            ],
        });
    });
});
