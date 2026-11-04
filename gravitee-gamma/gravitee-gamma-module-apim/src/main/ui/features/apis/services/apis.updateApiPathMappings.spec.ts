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
import { updateApiPathMappings } from './apis';
import { apimFetchJsonV2, apimFetchJsonV2WithMeta } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
    apimFetchJsonV2WithMeta: jest.fn(),
    apimFetchBlobV2: jest.fn(),
    apimFetchJsonOrg: jest.fn(),
    apimFetchJsonV1Env: jest.fn(),
}));

const mockFetch = jest.mocked(apimFetchJsonV2);
const mockFetchMeta = jest.mocked(apimFetchJsonV2WithMeta);

function putCall(): { body?: string; headers?: Record<string, string> } {
    return (mockFetch.mock.calls[0]?.[2] ?? {}) as { body?: string; headers?: Record<string, string> };
}

describe('updateApiPathMappings', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('updates pathMappings on the first HTTP listener only', async () => {
        mockFetchMeta.mockResolvedValueOnce({
            data: {
                id: 'api-1',
                updatedAt: '2026-03-25T10:00:00.000Z',
                listeners: [
                    { type: 'HTTP', pathMappings: ['/orders'] },
                    { type: 'SUBSCRIPTION', entrypoints: [] },
                ],
            },
            etag: '"etag-1"',
        });
        mockFetch.mockResolvedValueOnce({ id: 'api-1', listeners: [] });

        await updateApiPathMappings('env-1', 'api-1', current => [...current, '/pets/:petId']);

        const put = putCall();
        expect(JSON.parse(put.body ?? '{}').listeners).toEqual([
            { type: 'HTTP', pathMappings: ['/orders', '/pets/:petId'] },
            { type: 'SUBSCRIPTION', entrypoints: [] },
        ]);
        expect(put.headers?.['If-Match']).toBe('"etag-1"');
    });

    it('sends If-Match from updatedAt when the response has no ETag', async () => {
        mockFetchMeta.mockResolvedValueOnce({
            data: {
                id: 'api-1',
                updatedAt: 1_710_000_000_000,
                listeners: [{ type: 'HTTP', pathMappings: ['/orders'] }],
            },
            etag: null,
        });
        mockFetch.mockResolvedValueOnce({ id: 'api-1', listeners: [] });

        await updateApiPathMappings('env-1', 'api-1', current => [...current, '/pets']);

        expect(putCall().headers?.['If-Match']).toBe('"1710000000000"');
    });

    it('skips PUT when the updater returns the same list', async () => {
        const current = {
            id: 'api-1',
            listeners: [{ type: 'HTTP', pathMappings: ['/orders'] }],
        };
        mockFetchMeta.mockResolvedValueOnce({ data: current, etag: null });

        const result = await updateApiPathMappings('env-1', 'api-1', mappings => mappings);

        expect(result).toBe(current);
        expect(mockFetch).not.toHaveBeenCalled();
    });

    it('throws when there is no HTTP listener', async () => {
        mockFetchMeta.mockResolvedValueOnce({
            data: {
                id: 'api-1',
                listeners: [{ type: 'TCP', hosts: ['example.com'] }],
            },
            etag: null,
        });

        await expect(updateApiPathMappings('env-1', 'api-1', current => current)).rejects.toThrow(/no HTTP listener/);
        expect(mockFetch).not.toHaveBeenCalled();
    });
});
