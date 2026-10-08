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
    createApiDocumentationItem,
    deleteApiDocumentationItem,
    getApiDocumentationPageContent,
    importApiDocumentation,
    listApiDocumentation,
    listApiPublishLocations,
    publishApiToPortal,
    saveApiDocumentationPageContent,
    unpublishApiFromPortal,
    updateApiDocumentationItem,
} from './apiDocumentation';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { CreateApiDocumentationItem, ImportPortalNavigationRequest, UpdateApiDocumentationItem } from '../types/apiDocumentation';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

const BASE_PATH = '/apis/api-1/portal-navigation-items';

describe('apiDocumentation service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2.mockResolvedValue(undefined);
    });

    describe('listApiDocumentation', () => {
        it('GETs the documentation owned by the API', async () => {
            const response = { items: [], publications: [] };
            mockApimFetchJsonV2.mockResolvedValueOnce(response);

            await expect(listApiDocumentation('env-1', 'api-1')).resolves.toBe(response);
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', BASE_PATH);
        });

        it('URL-encodes the API id', async () => {
            await listApiDocumentation('env-1', 'api/with spaces');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api%2Fwith%20spaces/portal-navigation-items');
        });
    });

    describe('createApiDocumentationItem', () => {
        it('POSTs the item to the collection', async () => {
            const payload: CreateApiDocumentationItem = {
                type: 'PAGE',
                title: 'Getting started',
                area: 'TOP_NAVBAR',
                visibility: 'PUBLIC',
                contentType: 'GRAVITEE_MARKDOWN',
            };
            await createApiDocumentationItem('env-1', 'api-1', payload);
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', BASE_PATH, {
                method: 'POST',
                body: JSON.stringify(payload),
            });
        });
    });

    describe('updateApiDocumentationItem', () => {
        const payload: UpdateApiDocumentationItem = {
            type: 'FOLDER',
            title: 'Guides',
            order: 0,
            published: true,
            visibility: 'PUBLIC',
        };

        it('PUTs the item to its own path', async () => {
            await updateApiDocumentationItem('env-1', 'api-1', 'nav-1', payload);
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav-1`, {
                method: 'PUT',
                body: JSON.stringify(payload),
            });
        });

        it('asks to publish everything below the item when requested', async () => {
            await updateApiDocumentationItem('env-1', 'api-1', 'nav-1', payload, { propagatePublishToChildren: true });
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav-1?propagatePublishToChildren=true`, {
                method: 'PUT',
                body: JSON.stringify(payload),
            });
        });

        it('leaves out the propagation flag when it is false', async () => {
            await updateApiDocumentationItem('env-1', 'api-1', 'nav-1', payload, { propagatePublishToChildren: false });
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav-1`, expect.anything());
        });

        it('URL-encodes the item id', async () => {
            await updateApiDocumentationItem('env-1', 'api-1', 'nav/1', payload);
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav%2F1`, expect.anything());
        });
    });

    describe('deleteApiDocumentationItem', () => {
        it('DELETEs the item', async () => {
            await deleteApiDocumentationItem('env-1', 'api-1', 'nav-1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav-1`, { method: 'DELETE' });
        });

        it('URL-encodes the item id', async () => {
            await deleteApiDocumentationItem('env-1', 'api-1', 'nav 1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav%201`, { method: 'DELETE' });
        });
    });

    describe('importApiDocumentation', () => {
        it('POSTs the import request', async () => {
            const request: ImportPortalNavigationRequest = {
                title: 'Docs',
                source: { type: 'github-fetcher', configuration: { repository: 'docs' } },
            };
            await importApiDocumentation('env-1', 'api-1', request);
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/_import`, {
                method: 'POST',
                body: JSON.stringify(request),
            });
        });
    });

    describe('listApiPublishLocations', () => {
        it('GETs the sections the API can be published to', async () => {
            await listApiPublishLocations('env-1', 'api-1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/_publish-locations`);
        });
    });

    describe('publishApiToPortal', () => {
        it('POSTs the chosen section', async () => {
            await publishApiToPortal('env-1', 'api-1', { sectionId: 'section-1' });
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/_publish`, {
                method: 'POST',
                body: JSON.stringify({ sectionId: 'section-1' }),
            });
        });
    });

    describe('unpublishApiFromPortal', () => {
        it('POSTs with no body', async () => {
            await unpublishApiFromPortal('env-1', 'api-1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/_unpublish`, { method: 'POST' });
        });
    });

    describe('getApiDocumentationPageContent', () => {
        it("GETs the page's content", async () => {
            const content = { id: 'content-1', type: 'GRAVITEE_MARKDOWN', content: '# Hello' };
            mockApimFetchJsonV2.mockResolvedValueOnce(content);

            await expect(getApiDocumentationPageContent('env-1', 'api-1', 'nav-1')).resolves.toBe(content);
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav-1/content`);
        });

        it('URL-encodes the page id', async () => {
            await getApiDocumentationPageContent('env-1', 'api-1', 'nav/1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav%2F1/content`);
        });
    });

    describe('saveApiDocumentationPageContent', () => {
        it("PUTs the content to the page's content path", async () => {
            await saveApiDocumentationPageContent('env-1', 'api-1', 'nav-1', { content: '# Hello' });
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav-1/content`, {
                method: 'PUT',
                body: JSON.stringify({ content: '# Hello' }),
            });
        });

        it('URL-encodes the page id', async () => {
            await saveApiDocumentationPageContent('env-1', 'api-1', 'nav/1', { content: '' });
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', `${BASE_PATH}/nav%2F1/content`, expect.anything());
        });
    });
});
