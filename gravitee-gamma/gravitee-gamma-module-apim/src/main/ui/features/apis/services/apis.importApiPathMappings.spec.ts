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

    it('returns published next-gen OpenAPI pages and skips drafts and other content', async () => {
        mockFetch
            .mockResolvedValueOnce({
                items: [
                    { id: 'sw-1', type: 'PAGE', title: 'Petstore', published: true },
                    { id: 'draft-1', type: 'PAGE', title: 'Draft', published: false },
                    { id: 'blank-1', type: 'PAGE', title: '', published: true },
                    { id: 'md-1', type: 'PAGE', title: 'Guide', published: true },
                    { id: 'folder-1', type: 'FOLDER', title: 'Folder', published: true },
                ],
                publications: [],
            })
            .mockResolvedValueOnce({ id: 'content-1', type: 'OPENAPI', content: 'openapi: 3.0.0' })
            .mockResolvedValueOnce({ id: 'content-blank', type: 'OPENAPI', content: 'openapi: 3.0.0' })
            .mockResolvedValueOnce({ id: 'content-2', type: 'GRAVITEE_MARKDOWN', content: '# Guide' });

        await expect(listApiSwaggerPages('env-1', 'api-1', false)).resolves.toEqual([
            { id: 'sw-1', name: 'Petstore' },
            { id: 'blank-1', name: 'blank-1' },
        ]);
        expect(mockFetch).toHaveBeenCalledTimes(4);
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/portal-navigation-items');
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/portal-navigation-items/sw-1/content');
        expect(mockFetch).not.toHaveBeenCalledWith('env-1', '/apis/api-1/portal-navigation-items/draft-1/content');
        expect(mockFetch).not.toHaveBeenCalledWith('env-1', '/apis/api-1/portal-navigation-items/folder-1/content');
    });

    it('includes an unpublished OpenAPI page when the caller can see unpublished documentation', async () => {
        mockFetch
            .mockResolvedValueOnce({
                items: [
                    { id: 'draft-1', type: 'PAGE', title: 'Draft pets', published: false },
                    { id: 'md-1', type: 'PAGE', title: 'Guide', published: false },
                ],
                publications: [],
            })
            .mockResolvedValueOnce({ id: 'content-1', type: 'OPENAPI', content: 'openapi: 3.0.0' })
            .mockResolvedValueOnce({ id: 'content-2', type: 'GRAVITEE_MARKDOWN', content: '# Guide' });

        await expect(listApiSwaggerPages('env-1', 'api-1', true)).resolves.toEqual([{ id: 'draft-1', name: 'Draft pets' }]);
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/portal-navigation-items/draft-1/content');
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/portal-navigation-items/md-1/content');
    });

    it('keeps the pages whose content loads when another content request fails', async () => {
        mockFetch
            .mockResolvedValueOnce({
                items: [
                    { id: 'broken-1', type: 'PAGE', title: 'Broken', published: true },
                    { id: 'sw-1', type: 'PAGE', title: 'Petstore', published: true },
                ],
                publications: [],
            })
            .mockRejectedValueOnce(new Error('missing'))
            .mockResolvedValueOnce({ id: 'content-1', type: 'OPENAPI', content: 'openapi: 3.0.0' });

        await expect(listApiSwaggerPages('env-1', 'api-1', false)).resolves.toEqual([{ id: 'sw-1', name: 'Petstore' }]);
    });
});

describe('getApiSwaggerPagePaths', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('reads one Swagger page and converts its paths', async () => {
        mockFetch.mockResolvedValueOnce({
            type: 'OPENAPI',
            content: "swagger: '2.0'\npaths:\n  /store/order/{orderId}:\n    get: {}\n  /:\n    get: {}\n",
        });

        await expect(getApiSwaggerPagePaths('env-1', 'api-1', 'sw-1')).resolves.toEqual(['/store/order/:orderId']);
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/apis/api-1/portal-navigation-items/sw-1/content');
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
