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
import { getApiSwaggerPagePaths, importApiPathMappings, listApiSwaggerPages } from './apis';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => {
    const actual = jest.requireActual('../../../shared/api/apimClient');
    return {
        ...actual,
        apimFetchJsonV2: jest.fn(),
        apimFetchJsonV2WithMeta: jest.fn(),
        apimFetchBlobV2: jest.fn(),
        apimFetchJsonOrg: jest.fn(),
        apimFetchJsonV1Env: jest.fn(),
    };
});

const mockFetch = jest.mocked(apimFetchJsonV2);

describe('listApiSwaggerPages', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('returns SWAGGER page names and does not read their content', async () => {
        mockFetch.mockResolvedValueOnce({
            pages: [
                {
                    id: 'sw-1',
                    name: 'Petstore',
                    type: 'SWAGGER',
                    content: JSON.stringify({ openapi: '3.0.0', paths: { '/pets/{petId}': {} } }),
                },
                { id: 'md-1', name: 'Readme', type: 'MARKDOWN' },
                { id: 'sw-2', name: '', type: 'SWAGGER' },
                { type: 'SWAGGER' },
            ],
        });

        await expect(listApiSwaggerPages('env-1', 'api-1')).resolves.toEqual([
            { id: 'sw-1', name: 'Petstore' },
            { id: 'sw-2', name: 'sw-2' },
        ]);
        expect(mockFetch).toHaveBeenCalledTimes(1);
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/pages');
    });
});

describe('getApiSwaggerPagePaths', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('reads one Swagger page and converts its paths', async () => {
        mockFetch.mockResolvedValueOnce({ content: "swagger: '2.0'\npaths:\n  /store/order/{orderId}:\n    get: {}\n  /:\n    get: {}\n" });

        await expect(getApiSwaggerPagePaths('env-1', 'api-1', 'sw-1')).resolves.toEqual(['/store/order/:orderId']);
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/pages/sw-1');
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
