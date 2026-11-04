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
import { importApiPathMappings, listApiSwaggerPages } from './apis';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
    apimFetchJsonV2WithMeta: jest.fn(),
    apimFetchBlobV2: jest.fn(),
    apimFetchJsonOrg: jest.fn(),
    apimFetchJsonV1Env: jest.fn(),
}));

const mockFetch = jest.mocked(apimFetchJsonV2);

describe('listApiSwaggerPages', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('returns only SWAGGER pages with id and name', async () => {
        mockFetch.mockResolvedValueOnce({
            pages: [
                { id: 'sw-1', name: 'Petstore', type: 'SWAGGER' },
                { id: 'md-1', name: 'Readme', type: 'MARKDOWN' },
                { id: 'sw-2', name: '', type: 'SWAGGER' },
                { type: 'SWAGGER' },
            ],
        });

        await expect(listApiSwaggerPages('env-1', 'api-1')).resolves.toEqual([
            { id: 'sw-1', name: 'Petstore' },
            { id: 'sw-2', name: 'sw-2' },
        ]);
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/pages');
    });
});

describe('importApiPathMappings', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('POSTs pageId and overwrite to path-mappings/_import', async () => {
        mockFetch.mockResolvedValueOnce({ pathMappings: ['/pets/:petId'] });

        await expect(importApiPathMappings('env-1', 'api-1', { pageId: 'sw-1', overwrite: true })).resolves.toEqual({
            pathMappings: ['/pets/:petId'],
        });

        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/path-mappings/_import', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pageId: 'sw-1', overwrite: true }),
        });
    });
});
