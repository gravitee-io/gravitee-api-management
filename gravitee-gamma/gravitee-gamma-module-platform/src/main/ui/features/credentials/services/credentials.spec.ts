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
import {
    createCredential,
    deleteCredential,
    getCredential,
    listCredentialConsumers,
    listCredentials,
    rotateCredential,
    updateCredential,
} from './credentials';
import { gammaFetchJson } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    gammaFetchJson: jest.fn(),
    resolveOrganizationId: jest.fn().mockResolvedValue('org-1'),
}));

const mockGammaFetchJson = jest.mocked(gammaFetchJson);
const BASE = '/organizations/org-1/environments/env-1/credentials';

describe('credentials service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockGammaFetchJson.mockResolvedValue(undefined);
    });

    it('lists credentials with pagination and a trimmed search', async () => {
        await listCredentials('env-1', { query: ' open ai ', page: 2, perPage: 25 });

        expect(mockGammaFetchJson).toHaveBeenCalledWith(`${BASE}?page=2&perPage=25&q=open+ai`);
    });

    it('leaves a blank search out of the list request', async () => {
        await listCredentials('env-1', { query: '  ', page: 1, perPage: 10 });

        expect(mockGammaFetchJson).toHaveBeenCalledWith(`${BASE}?page=1&perPage=10`);
    });

    it('URL-encodes the id when loading a credential', async () => {
        await getCredential('env-1', 'cred/1');

        expect(mockGammaFetchJson).toHaveBeenCalledWith(`${BASE}/cred%2F1`);
    });

    it('creates a credential with the given payload', async () => {
        const payload = { name: 'OpenAI', strategy: 'BEARER' as const, values: { token: 'sk-1' } };

        await createCredential('env-1', payload);

        expect(mockGammaFetchJson).toHaveBeenCalledWith(BASE, { method: 'POST', body: JSON.stringify(payload) });
    });

    it('replaces the details of a credential', async () => {
        const payload = { name: 'OpenAI', configuration: {} };

        await updateCredential('env-1', 'cred/1', payload);

        expect(mockGammaFetchJson).toHaveBeenCalledWith(`${BASE}/cred%2F1`, {
            method: 'PUT',
            body: JSON.stringify(payload),
        });
    });

    it('rotates the values of a credential', async () => {
        await rotateCredential('env-1', 'cred/1', { values: { token: 'sk-2' } });

        expect(mockGammaFetchJson).toHaveBeenCalledWith(`${BASE}/cred%2F1/_rotate`, {
            method: 'POST',
            body: JSON.stringify({ values: { token: 'sk-2' } }),
        });
    });

    it('deletes a credential', async () => {
        await deleteCredential('env-1', 'cred/1');

        expect(mockGammaFetchJson).toHaveBeenCalledWith(`${BASE}/cred%2F1`, { method: 'DELETE' });
    });

    it('returns the consumers of a credential', async () => {
        const consumer = { module: 'aim', type: 'LLM_PROXY', id: 'proxy-1', name: 'Proxy' };
        mockGammaFetchJson.mockResolvedValue({ data: [consumer] });

        await expect(listCredentialConsumers('env-1', 'cred/1')).resolves.toEqual([consumer]);
        expect(mockGammaFetchJson).toHaveBeenCalledWith(`${BASE}/cred%2F1/consumers`);
    });
});
