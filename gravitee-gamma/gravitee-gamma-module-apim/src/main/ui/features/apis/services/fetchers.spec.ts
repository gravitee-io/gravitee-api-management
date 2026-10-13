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
import { listFetchers } from './fetchers';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

const HTTP_FETCHER_SCHEMA = JSON.stringify({
    type: 'object',
    properties: { url: { type: 'string' }, autoFetch: { type: 'boolean' }, fetchCron: { type: 'string' } },
    required: ['url'],
    if: { properties: { autoFetch: { const: true } } },
    then: { required: ['fetchCron'] },
});

describe('listFetchers', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('GETs the installed fetchers with their schema', async () => {
        mockApimFetchJsonV1Env.mockResolvedValueOnce([]);

        await listFetchers('env-1');

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/fetchers?expand=schema');
    });

    it('asks only for the fetchers that can list a folder when they are for an import', async () => {
        mockApimFetchJsonV1Env.mockResolvedValueOnce([]);

        await listFetchers('env-1', { forImport: true });

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/fetchers?expand=schema&import=true');
    });

    it('parses each schema and leaves out the legacy auto-fetch fields', async () => {
        mockApimFetchJsonV1Env.mockResolvedValueOnce([{ id: 'http-fetcher', name: 'HTTP Fetcher', schema: HTTP_FETCHER_SCHEMA }]);

        await expect(listFetchers('env-1')).resolves.toEqual([
            {
                id: 'http-fetcher',
                name: 'HTTP Fetcher',
                schema: { type: 'object', properties: { url: { type: 'string' } }, required: ['url'] },
            },
        ]);
    });

    it('names a fetcher without a name after its id', async () => {
        mockApimFetchJsonV1Env.mockResolvedValueOnce([{ id: 'github-fetcher', schema: '{"type":"object","properties":{}}' }]);

        const [fetcher] = await listFetchers('env-1');

        expect(fetcher.name).toBe('GitHub');
    });

    it('gives a fetcher without a readable schema empty settings, keeping the other fetchers', async () => {
        mockApimFetchJsonV1Env.mockResolvedValueOnce([
            { id: 'broken-fetcher', name: 'Broken', schema: '{not json' },
            { id: 'bare-fetcher', name: 'Bare', schema: null },
            { id: 'github-fetcher', name: 'GitHub', schema: '{"type":"object","properties":{"repository":{"type":"string"}}}' },
        ]);

        const fetchers = await listFetchers('env-1');

        expect(fetchers.map(fetcher => [fetcher.id, fetcher.schema])).toEqual([
            ['broken-fetcher', { type: 'object', properties: {} }],
            ['bare-fetcher', { type: 'object', properties: {} }],
            ['github-fetcher', { type: 'object', properties: { repository: { type: 'string' } } }],
        ]);
    });

    it('gives empty settings to a schema that is JSON but not an object', async () => {
        mockApimFetchJsonV1Env.mockResolvedValueOnce([
            { id: 'null-fetcher', name: 'Null', schema: 'null' },
            { id: 'array-fetcher', name: 'Array', schema: '[]' },
            { id: 'number-fetcher', name: 'Number', schema: '42' },
        ]);

        const fetchers = await listFetchers('env-1');

        expect(fetchers.map(fetcher => fetcher.schema)).toEqual([
            { type: 'object', properties: {} },
            { type: 'object', properties: {} },
            { type: 'object', properties: {} },
        ]);
    });
});
