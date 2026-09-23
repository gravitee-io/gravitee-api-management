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
import { http, HttpResponse } from 'msw';

import {
    createDocumentationPage,
    deleteDocumentationPage,
    fetchDocumentationPage,
    filterDocumentationPages,
    getAllApiPages,
    getApiPage,
    getApiPages,
    listFetchers,
    listPortalDocumentationFolders,
    placeApiInPortalFolder,
    publishDocumentationPage,
    startSpecGen,
    syncPortalDocumentationToApiProxy,
    syncPublishedPagesToPortalNavigation,
    publishApiWithDefaultOverview,
    unpublishApiFromPortalNavigation,
    unpublishDocumentationPage,
    unpublishPagesFromPortalNavigation,
    updateDocumentationPage,
} from './documentation';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { respondWith, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

const API_PAGES = `${TEST_V2_BASE}/apis/:apiId/pages`;
const API_PAGE = `${TEST_V2_BASE}/apis/:apiId/pages/:pageId`;
const PORTAL_ITEMS = `${TEST_V2_BASE}/portal-navigation-items`;
const PORTAL_ITEM = `${TEST_V2_BASE}/portal-navigation-items/:navId`;
const PORTAL_CONTENT = `${TEST_V2_BASE}/portal-page-contents/:contentId`;
const FETCHERS = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/environments/${TEST_CONFIG.environmentId}/fetchers`;

const PLACED_NAV_ITEMS = {
    items: [
        { id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR' },
        { id: 'api-nav', type: 'API', apiId: 'api-1', parentId: 'f1', title: 'Orders' },
    ],
};

describe('filterDocumentationPages', () => {
    it('keeps folders and editable page types and sorts breadcrumbs', () => {
        const result = filterDocumentationPages({
            pages: [
                { id: '1', type: 'MARKDOWN' },
                { id: '2', type: 'TRANSLATION' },
                { id: '3', type: 'FOLDER' },
                { id: '4', type: 'SWAGGER' },
            ],
            breadcrumb: [
                { id: 'b', name: 'B', position: 2 },
                { id: 'a', name: 'A', position: 1 },
            ],
        });
        expect(result.pages.map(page => page.id)).toEqual(['1', '3', '4']);
        expect(result.breadcrumb?.map(crumb => crumb.id)).toEqual(['a', 'b']);
    });
});

describe('documentation service', () => {
    beforeEach(() => {
        resetApimClientForTests();
        respondWith('get', API_PAGES, { pages: [] });
        respondWith('get', PORTAL_ITEMS, { items: [] });
    });

    it('getApiPages filters by parent and unsupported types', async () => {
        respondWith('get', API_PAGES, {
            pages: [
                { id: 'folder-1', name: 'Guides', type: 'FOLDER' },
                { id: 'md', type: 'MARKDOWN', parentId: 'folder-1' },
                { id: 'link', type: 'LINK', parentId: 'folder-1' },
                { id: 'root-md', type: 'MARKDOWN' },
            ],
        });

        const result = await getApiPages('DEFAULT', 'api-1', 'folder-1');
        expect(result.pages).toEqual([{ id: 'md', type: 'MARKDOWN', parentId: 'folder-1' }]);
        expect(result.breadcrumb).toEqual([{ id: 'folder-1', name: 'Guides', position: 1 }]);
    });

    it('createDocumentationPage posts the payload and mirrors an unpublished Navigation page', async () => {
        const created = { id: 'page-1', type: 'MARKDOWN', name: 'Intro' };
        const tracker = trackHandler('post', API_PAGES, created);
        respondWith('get', API_PAGES, { pages: [] });
        respondWith('get', PORTAL_ITEMS, PLACED_NAV_ITEMS);
        const navCreates: unknown[] = [];
        server.use(
            http.post(PORTAL_ITEMS, async ({ request }) => {
                const body = (await request.json()) as Record<string, unknown>;
                navCreates.push(body);
                return HttpResponse.json({
                    id: 'nav-page-1',
                    ...body,
                    portalPageContentId: 'content-1',
                });
            }),
        );

        await expect(
            createDocumentationPage('DEFAULT', 'api-1', { name: 'Intro', type: 'MARKDOWN', parentId: 'ROOT' }),
        ).resolves.toEqual(
            expect.objectContaining({
                id: 'page-1',
                name: 'Intro',
                type: 'MARKDOWN',
                published: false,
                portalNavId: 'nav-page-1',
                portalPageContentId: 'content-1',
            }),
        );
        expect(tracker.lastCall?.body).toEqual({ name: 'Intro', type: 'MARKDOWN', parentId: 'ROOT' });
        expect(navCreates).toEqual([
            expect.objectContaining({
                type: 'PAGE',
                title: 'Intro',
                parentId: 'api-nav',
                published: false,
                contentType: 'GRAVITEE_MARKDOWN',
            }),
        ]);
    });

    it('createDocumentationPage places the API under the default folder when missing, then mirrors the page unpublished', async () => {
        respondWith('get', API_PAGES, { pages: [] });
        respondWith('get', PORTAL_ITEMS, {
            items: [{ id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR' }],
        });
        respondWith('get', `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/environments/${TEST_CONFIG.environmentId}/settings`, {
            portalNext: { documentation: { defaultFolderId: 'f1' } },
        });
        respondWith('get', `${TEST_V2_BASE}/apis/api-1`, { id: 'api-1', name: 'Orders' });
        trackHandler('post', API_PAGES, { id: 'page-1', type: 'MARKDOWN', name: 'Intro' });

        const navCreates: unknown[] = [];
        let navListCalls = 0;
        server.use(
            http.get(PORTAL_ITEMS, () => {
                navListCalls += 1;
                // After the API is placed, subsequent lists include the API node.
                if (navCreates.some(item => (item as { type?: string }).type === 'API')) {
                    return HttpResponse.json({
                        items: [
                            { id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR' },
                            { id: 'api-nav', type: 'API', apiId: 'api-1', parentId: 'f1', title: 'Orders', published: false },
                        ],
                    });
                }
                return HttpResponse.json({
                    items: [{ id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR' }],
                });
            }),
            http.post(PORTAL_ITEMS, async ({ request }) => {
                const body = (await request.json()) as Record<string, unknown>;
                navCreates.push(body);
                if (body.type === 'API') {
                    return HttpResponse.json({ id: 'api-nav', ...body });
                }
                return HttpResponse.json({ id: 'nav-page-1', ...body, portalPageContentId: 'content-1' });
            }),
        );

        await createDocumentationPage('DEFAULT', 'api-1', { name: 'Intro', type: 'MARKDOWN', parentId: 'ROOT' });

        expect(navCreates).toEqual([
            expect.objectContaining({ type: 'API', apiId: 'api-1', parentId: 'f1', published: false }),
            expect.objectContaining({ type: 'PAGE', title: 'Intro', parentId: 'api-nav', published: false }),
        ]);
        expect(navListCalls).toBeGreaterThan(0);
    });

    it('createDocumentationPage fails when no default Navigation folder is configured', async () => {
        respondWith('get', API_PAGES, { pages: [] });
        respondWith('get', PORTAL_ITEMS, {
            items: [{ id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR' }],
        });
        respondWith('get', `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/environments/${TEST_CONFIG.environmentId}/settings`, {
            portalNext: { documentation: {} },
        });
        trackHandler('post', API_PAGES, { id: 'page-1', type: 'MARKDOWN', name: 'Intro' });

        await expect(
            createDocumentationPage('DEFAULT', 'api-1', { name: 'Intro', type: 'MARKDOWN', parentId: 'ROOT' }),
        ).rejects.toThrow(/default Navigation folder/);
    });

    it('update, publish, unpublish, fetch and delete use the page id', async () => {
        const page = { id: 'page-1', name: 'Intro', type: 'MARKDOWN', order: 2 };
        trackHandler('put', API_PAGE, page);
        trackHandler('post', `${API_PAGE}/_publish`, { ...page, published: true });
        trackHandler('post', `${API_PAGE}/_unpublish`, { ...page, published: false });
        trackHandler('post', `${API_PAGE}/_fetch`, { ...page, content: 'fetched' });
        trackHandler('delete', API_PAGE, undefined);

        await expect(updateDocumentationPage('DEFAULT', 'api-1', 'page-1', page)).resolves.toEqual(page);
        await expect(publishDocumentationPage('DEFAULT', 'api-1', 'page-1')).resolves.toEqual({ ...page, published: true });
        await expect(unpublishDocumentationPage('DEFAULT', 'api-1', 'page-1')).resolves.toEqual({ ...page, published: false });
        await expect(fetchDocumentationPage('DEFAULT', 'api-1', 'page-1')).resolves.toEqual({ ...page, content: 'fetched' });
        await expect(deleteDocumentationPage('DEFAULT', 'api-1', 'page-1')).resolves.toBeUndefined();
    });

    it('publish and unpublish navigation-only pages update portal items instead of failing classic 404', async () => {
        trackHandler('post', `${API_PAGE}/_publish`, { message: 'Page [nav-only] cannot be found.' }, 404);
        trackHandler('post', `${API_PAGE}/_unpublish`, { message: 'Page [nav-only] cannot be found.' }, 404);
        respondWith('get', PORTAL_ITEMS, {
            items: [
                ...PLACED_NAV_ITEMS.items,
                {
                    id: 'nav-only',
                    type: 'PAGE',
                    title: 'From navigation',
                    parentId: 'api-nav',
                    portalPageContentId: 'c1',
                    published: false,
                    visibility: 'PUBLIC',
                    order: 0,
                },
            ],
        });
        const publishNav = trackHandler('put', PORTAL_ITEM, {
            id: 'nav-only',
            type: 'PAGE',
            title: 'From navigation',
            published: true,
        });

        await expect(publishDocumentationPage('DEFAULT', 'api-1', 'nav-only')).resolves.toEqual(
            expect.objectContaining({ id: 'nav-only', published: true, portalNavId: 'nav-only' }),
        );
        expect(publishNav.lastCall?.body).toEqual(
            expect.objectContaining({ type: 'PAGE', title: 'From navigation', published: true, parentId: 'api-nav' }),
        );

        respondWith('get', PORTAL_ITEMS, {
            items: [
                ...PLACED_NAV_ITEMS.items,
                {
                    id: 'nav-only',
                    type: 'PAGE',
                    title: 'From navigation',
                    parentId: 'api-nav',
                    portalPageContentId: 'c1',
                    published: true,
                    visibility: 'PUBLIC',
                    order: 0,
                },
            ],
        });
        const unpublishNav = trackHandler('put', PORTAL_ITEM, {
            id: 'nav-only',
            type: 'PAGE',
            title: 'From navigation',
            published: false,
        });

        await expect(unpublishDocumentationPage('DEFAULT', 'api-1', 'nav-only')).resolves.toEqual(
            expect.objectContaining({ id: 'nav-only', published: false, portalNavId: 'nav-only' }),
        );
        expect(unpublishNav.lastCall?.body).toEqual(
            expect.objectContaining({ type: 'PAGE', title: 'From navigation', published: false, parentId: 'api-nav' }),
        );
    });

    it('getApiPage loads a single page', async () => {
        respondWith('get', API_PAGE, { id: 'page-1', name: 'Intro' });
        await expect(getApiPage('DEFAULT', 'api-1', 'page-1')).resolves.toEqual({ id: 'page-1', name: 'Intro' });
    });

    it('listFetchers uses the v1 environment fetchers catalog', async () => {
        respondWith('get', FETCHERS, [{ id: 'http-fetcher', name: 'HTTP', schema: '{}' }]);
        await expect(listFetchers('DEFAULT')).resolves.toEqual([{ id: 'http-fetcher', name: 'HTTP', schema: '{}' }]);
    });

    it('startSpecGen posts the spec-gen job', async () => {
        const tracker = trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/spec-gen/_start`, { state: 'STARTED' });
        await expect(startSpecGen('DEFAULT', 'api-1')).resolves.toEqual({ state: 'STARTED' });
        expect(tracker.callCount).toBe(1);
    });

    it('getAllApiPages omits parentId and filters unsupported types', async () => {
        const tracker = trackHandler('get', API_PAGES, {
            pages: [
                { id: 'md', type: 'MARKDOWN' },
                { id: 'link', type: 'LINK' },
                { id: 'folder', type: 'FOLDER' },
            ],
        });
        const result = await getAllApiPages('DEFAULT', 'api-1');
        expect(result.pages.map(page => page.id)).toEqual(['md', 'folder']);
        expect(tracker.lastCall?.url).not.toContain('parentId');
    });

    it('lists portal folders and finds the API placement', async () => {
        const tracker = trackHandler('get', `${TEST_V2_BASE}/portal-navigation-items`, {
            items: [
                {
                    id: 'f1',
                    type: 'FOLDER',
                    title: 'Docs',
                    area: 'TOP_NAVBAR',
                },
                { id: 'api-nav', type: 'API', apiId: 'api-1', parentId: 'f1' },
                { id: 'guides', type: 'FOLDER', title: 'Guides', parentId: 'api-nav' },
            ],
        });
        const result = await listPortalDocumentationFolders('DEFAULT', 'api-1');
        expect(result.folders).toEqual([{ id: 'f1', path: 'Docs', area: 'TOP_NAVBAR' }]);
        expect(result.placement).toEqual({
            folderId: 'f1',
            folderPath: 'Docs',
            itemId: 'api-nav',
            area: 'TOP_NAVBAR',
            published: false,
        });
        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.url).toContain('area=TOP_NAVBAR');
    });

    it('placeApiInPortalFolder posts an API navigation item into TOP_NAVBAR', async () => {
        const tracker = trackHandler('post', `${TEST_V2_BASE}/portal-navigation-items`, { id: 'api-nav' });
        await placeApiInPortalFolder('DEFAULT', 'api-1', 'Orders', { id: 'f1', path: 'Docs', area: 'TOP_NAVBAR' });
        expect(tracker.lastCall?.body).toEqual({
            type: 'API',
            apiId: 'api-1',
            title: 'Orders',
            parentId: 'f1',
            area: 'TOP_NAVBAR',
            visibility: 'PUBLIC',
            published: false,
        });
    });

    it('syncPublishedPagesToPortalNavigation creates folder and page items under the API', async () => {
        const created: unknown[] = [];
        const defaultPages = trackHandler('post', `${TEST_V2_BASE}/portal-navigation-items/_default-pages`, undefined, 204);
        server.use(
            http.get(`${TEST_V2_BASE}/portal-navigation-items`, () => HttpResponse.json({ items: [] })),
            http.post(`${TEST_V2_BASE}/portal-navigation-items`, async ({ request }) => {
                const body = (await request.json()) as { type: string; title?: string };
                created.push(body);
                const id = `nav-${created.length}`;
                return HttpResponse.json({
                    id,
                    ...body,
                    portalPageContentId: body.type === 'PAGE' ? `content-${created.length}` : undefined,
                });
            }),
            http.put(`${TEST_V2_BASE}/portal-page-contents/:contentId`, async ({ request }) => {
                created.push(await request.json());
                return HttpResponse.json({});
            }),
            http.put(`${TEST_V2_BASE}/portal-navigation-items/:navId`, async ({ request }) => {
                created.push(await request.json());
                return HttpResponse.json({});
            }),
            http.get(`${TEST_V2_BASE}/apis/:apiId/pages/:pageId`, () =>
                HttpResponse.json({ id: 'page-1', name: 'Getting started', type: 'MARKDOWN', content: '# Hello' }),
            ),
        );

        await syncPublishedPagesToPortalNavigation(
            'DEFAULT',
            'api-1',
            'Orders',
            { id: 'f1', path: 'Docs', area: 'TOP_NAVBAR' },
            null,
            [
                { id: 'folder-1', name: 'Guides', type: 'FOLDER', order: 0 },
                { id: 'page-1', name: 'Getting started', type: 'MARKDOWN', parentId: 'folder-1', order: 0 },
            ],
            ['folder-1', 'page-1'],
        );

        expect(defaultPages.callCount).toBe(0);
        const apiPublishIndex = created.findIndex(
            item => typeof item === 'object' && item !== null && (item as { type?: string; published?: boolean }).type === 'API' && (item as { published?: boolean }).published === true,
        );
        const folderPublishIndex = created.findIndex(
            item =>
                typeof item === 'object' &&
                item !== null &&
                (item as { type?: string; published?: boolean; title?: string }).type === 'FOLDER' &&
                (item as { published?: boolean }).published === true &&
                (item as { title?: string }).title === 'Guides',
        );
        expect(apiPublishIndex).toBeGreaterThanOrEqual(0);
        expect(folderPublishIndex).toBeGreaterThan(apiPublishIndex);
        expect(created).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ type: 'API', apiId: 'api-1', parentId: 'f1', area: 'TOP_NAVBAR' }),
                expect.objectContaining({ type: 'API', published: true, apiId: 'api-1' }),
                expect.objectContaining({ type: 'FOLDER', title: 'Guides', area: 'TOP_NAVBAR' }),
                expect.objectContaining({ type: 'FOLDER', title: 'Guides', published: true }),
                expect.objectContaining({ type: 'PAGE', title: 'Getting started', contentType: 'GRAVITEE_MARKDOWN' }),
                expect.objectContaining({ content: '# Hello', type: 'GRAVITEE_MARKDOWN' }),
                expect.objectContaining({ type: 'PAGE', title: 'Getting started', published: true }),
            ]),
        );
    });

    it('syncPublishedPagesToPortalNavigation moves an existing API node to the selected folder', async () => {
        const updates: unknown[] = [];
        respondWith('get', PORTAL_ITEMS, {
            items: [
                { id: 'f1', type: 'FOLDER', title: 'Default Docs', area: 'TOP_NAVBAR', published: true, visibility: 'PUBLIC' },
                { id: 'f2', type: 'FOLDER', title: 'Target', area: 'TOP_NAVBAR', published: true, visibility: 'PUBLIC' },
                {
                    id: 'api-nav',
                    type: 'API',
                    apiId: 'api-1',
                    parentId: 'f1',
                    title: 'Orders',
                    published: false,
                    visibility: 'PUBLIC',
                    order: 0,
                },
                {
                    id: 'page-nav',
                    type: 'PAGE',
                    title: 'Getting started',
                    parentId: 'api-nav',
                    published: false,
                    visibility: 'PUBLIC',
                    order: 0,
                    portalPageContentId: 'c1',
                },
            ],
        });
        respondWith('put', `${TEST_V2_BASE}/portal-page-contents/c1`, {});
        respondWith('get', `${TEST_V2_BASE}/apis/api-1/pages/page-1`, {
            id: 'page-1',
            name: 'Getting started',
            type: 'MARKDOWN',
            content: '# Hello',
        });
        server.use(
            http.put(`${TEST_V2_BASE}/portal-navigation-items/:navId`, async ({ request }) => {
                updates.push(await request.json());
                return HttpResponse.json({});
            }),
        );

        await syncPublishedPagesToPortalNavigation(
            'DEFAULT',
            'api-1',
            'Orders',
            { id: 'f2', path: 'Target', area: 'TOP_NAVBAR' },
            { folderId: 'f1', folderPath: 'Default Docs', itemId: 'api-nav', area: 'TOP_NAVBAR', published: false },
            [{ id: 'page-1', name: 'Getting started', type: 'MARKDOWN', order: 0, portalNavId: 'page-nav' }],
            ['page-1'],
        );

        expect(updates).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ type: 'API', apiId: 'api-1', parentId: 'f2', published: true }),
                expect.objectContaining({ type: 'PAGE', title: 'Getting started', published: true, parentId: 'api-nav' }),
            ]),
        );
    });

    it('getAllApiPages merges navigation-only pages even when the API nav node is unpublished', async () => {
        respondWith('get', API_PAGES, { pages: [{ id: 'md', name: 'Intro', type: 'MARKDOWN' }] });
        respondWith('get', PORTAL_ITEMS, {
            items: [
                { id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR' },
                { id: 'api-nav', type: 'API', apiId: 'api-1', parentId: 'f1', title: 'Orders', published: false },
                {
                    id: 'nav-only',
                    type: 'PAGE',
                    title: 'From navigation',
                    parentId: 'api-nav',
                    portalPageContentId: 'c1',
                    published: false,
                },
            ],
        });
        respondWith('get', PORTAL_CONTENT, { id: 'c1', type: 'GRAVITEE_MARKDOWN', content: '# Nav' });

        const result = await getAllApiPages('DEFAULT', 'api-1');
        expect(result.pages).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ id: 'md', name: 'Intro', type: 'MARKDOWN' }),
                expect.objectContaining({
                    id: 'nav-only',
                    name: 'From navigation',
                    type: 'MARKDOWN',
                    published: false,
                    portalNavId: 'nav-only',
                    portalPageContentId: 'c1',
                }),
            ]),
        );
    });

    it('getApiPage overlays matching navigation content onto the classic page', async () => {
        respondWith('get', API_PAGE, { id: 'page-1', name: 'Intro', type: 'MARKDOWN', content: '# Classic' });
        respondWith('get', API_PAGES, { pages: [{ id: 'page-1', name: 'Intro', type: 'MARKDOWN', content: '# Classic' }] });
        respondWith('get', PORTAL_ITEMS, {
            items: [
                ...PLACED_NAV_ITEMS.items,
                {
                    id: 'intro-nav',
                    type: 'PAGE',
                    title: 'Intro',
                    parentId: 'api-nav',
                    portalPageContentId: 'c1',
                    published: true,
                },
            ],
        });
        respondWith('get', PORTAL_CONTENT, { id: 'c1', type: 'GRAVITEE_MARKDOWN', content: '# From navigation' });

        await expect(getApiPage('DEFAULT', 'api-1', 'page-1')).resolves.toEqual(
            expect.objectContaining({
                id: 'page-1',
                name: 'Intro',
                type: 'MARKDOWN',
                content: '# From navigation',
                portalNavId: 'intro-nav',
                portalPageContentId: 'c1',
            }),
        );
    });

    it('updateDocumentationPage writes the matching navigation item when the API is placed', async () => {
        const page = { id: 'page-1', name: 'Intro', type: 'MARKDOWN' as const, content: '# Updated', order: 2 };
        trackHandler('put', API_PAGE, page);
        respondWith('get', API_PAGES, { pages: [page] });
        respondWith('get', PORTAL_ITEMS, {
            items: [
                ...PLACED_NAV_ITEMS.items,
                {
                    id: 'intro-nav',
                    type: 'PAGE',
                    title: 'Intro',
                    parentId: 'api-nav',
                    portalPageContentId: 'c1',
                },
            ],
        });
        const navTracker = trackHandler('put', PORTAL_ITEM, {});
        const contentTracker = trackHandler('put', PORTAL_CONTENT, {});

        await expect(updateDocumentationPage('DEFAULT', 'api-1', 'page-1', page)).resolves.toEqual(
            expect.objectContaining({ id: 'page-1', portalNavId: 'intro-nav' }),
        );
        expect(navTracker.lastCall?.body).toEqual(
            expect.objectContaining({ type: 'PAGE', title: 'Intro', parentId: 'api-nav' }),
        );
        expect(contentTracker.lastCall?.body).toEqual({ content: '# Updated', type: 'GRAVITEE_MARKDOWN' });
    });

    it('deleteDocumentationPage removes the classic page and its Navigation mirror', async () => {
        respondWith('get', PORTAL_ITEMS, {
            items: [
                ...PLACED_NAV_ITEMS.items,
                { id: 'intro-nav', type: 'PAGE', title: 'Intro', parentId: 'api-nav', published: false },
                { id: 'other-nav', type: 'PAGE', title: 'Other', parentId: 'api-nav', published: false },
            ],
        });
        respondWith('get', API_PAGE, {
            id: 'page-1',
            name: 'Intro',
            type: 'MARKDOWN',
            portalNavId: 'intro-nav',
        });
        respondWith('get', API_PAGES, {
            pages: [
                { id: 'page-1', name: 'Intro', type: 'MARKDOWN' },
                { id: 'page-2', name: 'Other', type: 'MARKDOWN' },
            ],
        });
        const navDeletes: string[] = [];
        server.use(
            http.delete(PORTAL_ITEM, ({ params }) => {
                navDeletes.push(String(params.navId));
                return new HttpResponse(null, { status: 204 });
            }),
        );
        const pageDelete = trackHandler('delete', API_PAGE, undefined, 204);

        await expect(deleteDocumentationPage('DEFAULT', 'api-1', 'page-1')).resolves.toBeUndefined();
        expect(pageDelete.callCount).toBe(1);
        expect(navDeletes).toEqual(['intro-nav']);
    });

    it('deleteDocumentationPage removes the API Navigation node when it was the last document', async () => {
        respondWith('get', PORTAL_ITEMS, {
            items: [
                ...PLACED_NAV_ITEMS.items,
                { id: 'intro-nav', type: 'PAGE', title: 'Intro', parentId: 'api-nav', published: false },
            ],
        });
        respondWith('get', API_PAGE, {
            id: 'page-1',
            name: 'Intro',
            type: 'MARKDOWN',
            portalNavId: 'intro-nav',
        });
        respondWith('get', API_PAGES, {
            pages: [{ id: 'page-1', name: 'Intro', type: 'MARKDOWN' }],
        });
        const navDeletes: string[] = [];
        server.use(
            http.delete(PORTAL_ITEM, ({ params }) => {
                navDeletes.push(String(params.navId));
                return new HttpResponse(null, { status: 204 });
            }),
        );
        trackHandler('delete', API_PAGE, undefined, 204);

        await deleteDocumentationPage('DEFAULT', 'api-1', 'page-1');
        expect(navDeletes).toEqual(['intro-nav', 'api-nav']);
    });

    it('deleteDocumentationPage deletes a navigation-only page without calling classic DELETE', async () => {
        respondWith('get', PORTAL_ITEMS, {
            items: [
                ...PLACED_NAV_ITEMS.items,
                {
                    id: 'nav-only',
                    type: 'PAGE',
                    title: 'From navigation',
                    parentId: 'api-nav',
                    portalPageContentId: 'c1',
                    published: false,
                },
            ],
        });
        respondWith('get', API_PAGES, { pages: [] });
        respondWith('get', PORTAL_CONTENT, { id: 'c1', type: 'GRAVITEE_MARKDOWN', content: '# Nav' });
        trackHandler('get', API_PAGE, { message: 'not found' }, 404);
        const pageDelete = trackHandler('delete', API_PAGE, undefined, 204);
        const navDeletes: string[] = [];
        server.use(
            http.delete(PORTAL_ITEM, ({ params }) => {
                navDeletes.push(String(params.navId));
                return new HttpResponse(null, { status: 204 });
            }),
        );

        await deleteDocumentationPage('DEFAULT', 'api-1', 'nav-only');
        expect(pageDelete.callCount).toBe(0);
        expect(navDeletes).toEqual(['nav-only', 'api-nav']);
    });

    it('syncPortalDocumentationToApiProxy overwrites classic pages from navigation', async () => {
        respondWith('get', PORTAL_ITEMS, {
            items: [
                ...PLACED_NAV_ITEMS.items,
                {
                    id: 'guides',
                    type: 'FOLDER',
                    title: 'Guides',
                    parentId: 'api-nav',
                    published: true,
                    order: 0,
                },
                {
                    id: 'intro-nav',
                    type: 'PAGE',
                    title: 'Intro',
                    parentId: 'guides',
                    portalPageContentId: 'c1',
                    published: true,
                    order: 0,
                },
            ],
        });
        respondWith('get', PORTAL_CONTENT, { id: 'c1', type: 'GRAVITEE_MARKDOWN', content: '# From portal' });
        respondWith('get', API_PAGES, {
            pages: [{ id: 'old-page', name: 'Stale', type: 'MARKDOWN' }],
        });
        const deleteOld = trackHandler('delete', API_PAGE, undefined, 204);
        const created: unknown[] = [];
        server.use(
            http.post(API_PAGES, async ({ request }) => {
                const body = await request.json();
                created.push(body);
                const id = `created-${created.length}`;
                return HttpResponse.json({ id, ...(body as object) });
            }),
            http.post(`${API_PAGE}/_publish`, async ({ request }) => {
                created.push({ publish: true, url: request.url });
                return HttpResponse.json({});
            }),
        );

        await syncPortalDocumentationToApiProxy('DEFAULT', 'api-1');

        expect(deleteOld.callCount).toBe(1);
        expect(created).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ name: 'Guides', type: 'FOLDER', parentId: 'ROOT' }),
                expect.objectContaining({ name: 'Intro', type: 'MARKDOWN', content: '# From portal', parentId: 'created-1' }),
                expect.objectContaining({ publish: true }),
            ]),
        );
    });

    it('unpublishes the API navigation node when the last published document is unpublished', async () => {
        respondWith('get', PORTAL_ITEMS, {
            items: [
                { id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR', published: true },
                { id: 'api-nav', type: 'API', apiId: 'api-1', parentId: 'f1', title: 'Orders', published: true },
                {
                    id: 'page-nav',
                    type: 'PAGE',
                    title: 'Getting started',
                    parentId: 'api-nav',
                    portalPageContentId: 'c1',
                    published: true,
                },
            ],
        });
        const updates: Array<{ navId: string; body: Record<string, unknown> }> = [];
        server.use(
            http.put(PORTAL_ITEM, async ({ params, request }) => {
                updates.push({ navId: String(params.navId), body: (await request.json()) as Record<string, unknown> });
                return HttpResponse.json({});
            }),
        );

        await unpublishPagesFromPortalNavigation(
            'DEFAULT',
            'api-1',
            { folderId: 'f1', folderPath: 'Docs', itemId: 'api-nav', area: 'TOP_NAVBAR', published: true },
            [{ id: 'page-1', name: 'Getting started', type: 'MARKDOWN', published: true, portalNavId: 'page-nav' }],
            ['page-1'],
        );

        expect(updates).toEqual([
            expect.objectContaining({
                navId: 'page-nav',
                body: expect.objectContaining({ type: 'PAGE', published: false }),
            }),
            expect.objectContaining({
                navId: 'api-nav',
                body: expect.objectContaining({ type: 'API', apiId: 'api-1', published: false }),
            }),
        ]);
    });

    it('keeps the API navigation node published when other published documents remain', async () => {
        respondWith('get', PORTAL_ITEMS, {
            items: [
                { id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR', published: true },
                { id: 'api-nav', type: 'API', apiId: 'api-1', parentId: 'f1', title: 'Orders', published: true },
                {
                    id: 'page-nav',
                    type: 'PAGE',
                    title: 'Getting started',
                    parentId: 'api-nav',
                    portalPageContentId: 'c1',
                    published: true,
                },
                {
                    id: 'other-nav',
                    type: 'PAGE',
                    title: 'Reference',
                    parentId: 'api-nav',
                    portalPageContentId: 'c2',
                    published: true,
                },
            ],
        });
        const updates: Array<{ navId: string; body: Record<string, unknown> }> = [];
        server.use(
            http.put(PORTAL_ITEM, async ({ params, request }) => {
                updates.push({ navId: String(params.navId), body: (await request.json()) as Record<string, unknown> });
                return HttpResponse.json({});
            }),
        );

        await unpublishPagesFromPortalNavigation(
            'DEFAULT',
            'api-1',
            { folderId: 'f1', folderPath: 'Docs', itemId: 'api-nav', area: 'TOP_NAVBAR', published: true },
            [
                { id: 'page-1', name: 'Getting started', type: 'MARKDOWN', published: true, portalNavId: 'page-nav' },
                { id: 'page-2', name: 'Reference', type: 'MARKDOWN', published: true, portalNavId: 'other-nav' },
            ],
            ['page-1'],
        );

        expect(updates).toEqual([
            expect.objectContaining({
                navId: 'page-nav',
                body: expect.objectContaining({ type: 'PAGE', published: false }),
            }),
        ]);
        expect(updates.some(update => update.navId === 'api-nav')).toBe(false);
    });

    it('syncPublishedPagesToPortalNavigation reuses an existing API node when placement is null', async () => {
        const created: Array<{ type: string; apiId?: string }> = [];
        respondWith('get', PORTAL_ITEMS, {
            items: [
                { id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR', published: true },
                { id: 'api-nav', type: 'API', apiId: 'api-1', parentId: 'default-folder', title: 'Orders', published: false },
            ],
        });
        server.use(
            http.post(PORTAL_ITEMS, async ({ request }) => {
                const body = (await request.json()) as { type: string; apiId?: string };
                created.push(body);
                return HttpResponse.json({ id: `new-${created.length}`, ...body });
            }),
            http.put(PORTAL_ITEM, () => HttpResponse.json({})),
            http.put(PORTAL_CONTENT, () => HttpResponse.json({})),
            http.get(API_PAGE, () =>
                HttpResponse.json({ id: 'page-1', name: 'Overview', type: 'MARKDOWN', content: '# Overview' }),
            ),
        );

        await syncPublishedPagesToPortalNavigation(
            'DEFAULT',
            'api-1',
            'Orders',
            { id: 'f1', path: 'Docs', area: 'TOP_NAVBAR' },
            null,
            [{ id: 'page-1', name: 'Overview', type: 'MARKDOWN', published: true }],
            ['page-1'],
        );

        expect(created.filter(item => item.type === 'API')).toEqual([]);
        expect(created.some(item => item.type === 'PAGE')).toBe(true);
    });

    it('publishApiWithDefaultOverview publishes the API parent before the Overview child', async () => {
        const classicCreates = trackHandler('post', API_PAGES, {
            id: 'overview-1',
            name: 'Overview',
            type: 'MARKDOWN',
            content: '# Orders',
        });
        const classicPublish = trackHandler('post', `${API_PAGE}/_publish`, {
            id: 'overview-1',
            name: 'Overview',
            type: 'MARKDOWN',
            published: true,
            content: '# Orders',
        });

        const portalPosts: Array<{ type: string; apiId?: string; parentId?: string | null; title?: string }> = [];
        const portalPuts: Array<{ navId: string; published?: boolean; type?: string; parentId?: string | null }> = [];
        let items = [
            { id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR', published: true, visibility: 'PUBLIC' },
        ];

        server.use(
            http.get(PORTAL_ITEMS, () => HttpResponse.json({ items })),
            http.post(PORTAL_ITEMS, async ({ request }) => {
                const body = (await request.json()) as {
                    type: string;
                    apiId?: string;
                    parentId?: string | null;
                    title?: string;
                };
                portalPosts.push(body);
                const id = body.type === 'API' ? 'api-nav' : 'page-nav';
                const created = {
                    id,
                    ...body,
                    portalPageContentId: body.type === 'PAGE' ? 'c1' : undefined,
                    published: false,
                    visibility: 'PUBLIC',
                };
                items = [...items, created];
                return HttpResponse.json(created);
            }),
            http.put(PORTAL_ITEM, async ({ request, params }) => {
                const body = (await request.json()) as {
                    published?: boolean;
                    type?: string;
                    parentId?: string | null;
                };
                portalPuts.push({ navId: String(params.navId), ...body });
                items = items.map(item => (item.id === params.navId ? { ...item, ...body } : item));
                return HttpResponse.json({});
            }),
            http.put(PORTAL_CONTENT, () => HttpResponse.json({})),
            http.get(API_PAGE, () =>
                HttpResponse.json({ id: 'overview-1', name: 'Overview', type: 'MARKDOWN', content: '# Orders' }),
            ),
        );

        await publishApiWithDefaultOverview(
            'DEFAULT',
            'api-1',
            'Orders',
            { id: 'f1', path: 'Docs', area: 'TOP_NAVBAR' },
            null,
        );

        expect(classicCreates.callCount).toBe(1);
        expect(classicPublish.callCount).toBe(1);
        expect(portalPosts[0]).toEqual(
            expect.objectContaining({ type: 'API', apiId: 'api-1', parentId: 'f1' }),
        );
        expect(portalPosts.some(post => post.type === 'PAGE' && post.parentId === 'api-nav')).toBe(true);

        const apiPublishIndex = portalPuts.findIndex(
            put => put.navId === 'api-nav' && put.published === true && put.type === 'API',
        );
        const pagePublishIndex = portalPuts.findIndex(
            put => put.navId === 'page-nav' && put.published === true && put.type === 'PAGE',
        );
        expect(apiPublishIndex).toBeGreaterThanOrEqual(0);
        expect(pagePublishIndex).toBeGreaterThan(apiPublishIndex);
    });

    it('unpublishApiFromPortalNavigation unpublishes classic pages and propagates to API children', async () => {
        respondWith('get', PORTAL_ITEMS, {
            items: [
                { id: 'f1', type: 'FOLDER', title: 'Docs', area: 'TOP_NAVBAR', published: true },
                { id: 'api-nav', type: 'API', apiId: 'api-1', parentId: 'f1', title: 'Orders', published: true },
                {
                    id: 'page-nav',
                    type: 'PAGE',
                    title: 'Getting started',
                    parentId: 'api-nav',
                    portalPageContentId: 'c1',
                    published: true,
                },
            ],
        });
        const classicUnpublish = trackHandler('post', `${API_PAGE}/_unpublish`, { id: 'page-1', published: false });
        const updates: Array<{ url: string; body: Record<string, unknown> }> = [];
        server.use(
            http.put(PORTAL_ITEM, async ({ request, params }) => {
                updates.push({
                    url: String(params.navId) + (new URL(request.url).search || ''),
                    body: (await request.json()) as Record<string, unknown>,
                });
                return HttpResponse.json({});
            }),
        );

        await unpublishApiFromPortalNavigation(
            'DEFAULT',
            'api-1',
            { folderId: 'f1', folderPath: 'Docs', itemId: 'api-nav', area: 'TOP_NAVBAR', published: true },
            [{ id: 'page-1', name: 'Getting started', type: 'MARKDOWN', published: true, portalNavId: 'page-nav' }],
        );

        expect(classicUnpublish.callCount).toBe(1);
        expect(updates).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    url: 'api-nav?propagatePublishToChildren=true',
                    body: expect.objectContaining({ type: 'API', apiId: 'api-1', published: false }),
                }),
            ]),
        );
    });
});
