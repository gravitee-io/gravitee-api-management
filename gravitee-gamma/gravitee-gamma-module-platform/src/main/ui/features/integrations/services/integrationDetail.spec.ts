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

import { deleteFederatedApis, deleteIntegration, getIntegration, hasFederatedApis } from './integrationDetail';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { Integration } from '../types/integration';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('integration detail service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2.mockResolvedValue({ id: 'int-1', name: 'Confluent integration', provider: 'confluent' });
    });

    it('gets the integration by id for the environment', async () => {
        const integration: Integration = { id: 'int-1', name: 'Confluent integration', provider: 'confluent' };
        mockApimFetchJsonV2.mockResolvedValueOnce(integration);

        const result = await getIntegration('env-1', 'int-1');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/int-1');
        expect(result).toEqual(integration);
    });

    it.each([
        {
            scenario: 'has one',
            response: {
                data: [{ id: 'federated-api-1' }],
                pagination: { page: 1, perPage: 1, pageCount: 3, pageItemsCount: 1, totalCount: 3 },
            },
            expected: true,
        },
        {
            scenario: 'has none',
            response: { data: [], pagination: { page: 1, perPage: 1, pageCount: 0, pageItemsCount: 0, totalCount: 0 } },
            expected: false,
        },
    ])('asks for a single federated API of the integration to tell it $scenario', async ({ response, expected }) => {
        mockApimFetchJsonV2.mockResolvedValueOnce(response);

        const result = await hasFederatedApis('env-1', 'int-1');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/int-1/apis?page=1&perPage=1');
        expect(result).toBe(expected);
    });

    it('deletes the integration by id for the environment', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce(undefined);

        await deleteIntegration('env-1', 'a/b c');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/a%2Fb%20c', { method: 'DELETE' });
    });

    it('deletes the federated APIs of the integration and returns the deleted, skipped and error counts', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce({ deleted: 2, skipped: 1, errors: 3 });

        const result = await deleteFederatedApis('env-1', 'a/b c');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/a%2Fb%20c/apis', { method: 'DELETE' });
        expect(result).toEqual({ deleted: 2, skipped: 1, errors: 3 });
    });

    it('percent-encodes reserved characters in the integration id', async () => {
        await getIntegration('env-1', 'a/b c');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations/a%2Fb%20c');
    });

    it('keeps the agent status and pending ingestion job of a gateway-style integration', async () => {
        const integration: Integration = {
            id: 'int-aws',
            name: 'AWS integration',
            provider: 'aws-api-gateway',
            agentStatus: 'DISCONNECTED',
            pendingJob: { id: 'job-1', status: 'PENDING' },
        };
        mockApimFetchJsonV2.mockResolvedValueOnce(integration);

        const result = await getIntegration('env-1', 'int-aws');

        expect(result).toEqual(integration);
    });
});
