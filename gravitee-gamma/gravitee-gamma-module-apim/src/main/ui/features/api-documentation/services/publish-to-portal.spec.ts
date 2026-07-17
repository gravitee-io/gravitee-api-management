/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { installFakeIndexedDB, resetFakeIndexedDB } from '@apim/portal-editor/testing/fake-indexeddb';
import { getNavItems, saveNavItem } from '@apim/portal-editor/portals/storage/navigation-items.storage';
import { getPageContentsForPortal, savePageContent } from '@apim/portal-editor/portals/storage/page-contents.storage';
import type { PortalNavigationApi, PortalNavigationPage } from '@apim/portal-editor/portals/types';
import { publishApiDocumentationToPortal } from './publish-to-portal';

installFakeIndexedDB();

const PORTAL_ID = 'portal-1';
const DRAFT_PORTAL_ID = 'api-doc-api-1';

describe('publishApiDocumentationToPortal', () => {
    beforeEach(async () => {
        resetFakeIndexedDB();

        const overviewPage: PortalNavigationPage = {
            id: 'draft-page-1',
            portalId: DRAFT_PORTAL_ID,
            title: 'Overview',
            type: 'PAGE',
            parentId: null,
            order: 0,
            slug: 'overview-abc123',
            published: true,
        };

        await saveNavItem(overviewPage);
        await savePageContent({
            id: 'draft-content-1',
            portalId: DRAFT_PORTAL_ID,
            navigationItemId: 'draft-page-1',
            document: [{ id: 'block-1', type: 'paragraph', content: [], children: [] }],
        });
    });

    it('should create an API node and publish draft pages under it', async () => {
        const result = await publishApiDocumentationToPortal({
            apiId: 'api-1',
            apiName: 'Payments API',
            draftPortalId: DRAFT_PORTAL_ID,
            portalId: PORTAL_ID,
            parentId: null,
            mode: 'replace',
        });

        const portalItems = await getNavItems(PORTAL_ID);
        const apiNode = portalItems.find(
            (item): item is PortalNavigationApi => item.type === 'API' && item.apiId === 'api-1',
        );

        expect(apiNode).toBeDefined();
        expect(result.apiNavItemId).toBe(apiNode?.id);
        expect(result.publishedPageCount).toBe(1);

        const publishedChildren = portalItems.filter(item => item.parentId === apiNode?.id);
        expect(publishedChildren).toHaveLength(1);
        expect(publishedChildren[0]?.title).toBe('Overview');

        const portalContents = await getPageContentsForPortal(PORTAL_ID);
        expect(portalContents).toHaveLength(1);
        expect(portalContents[0]?.navigationItemId).toBe(publishedChildren[0]?.id);
    });

    it('should replace existing API children when publishing again', async () => {
        await publishApiDocumentationToPortal({
            apiId: 'api-1',
            apiName: 'Payments API',
            draftPortalId: DRAFT_PORTAL_ID,
            portalId: PORTAL_ID,
            parentId: null,
            mode: 'replace',
        });

        const secondOverview: PortalNavigationPage = {
            id: 'draft-page-2',
            portalId: DRAFT_PORTAL_ID,
            title: 'Getting Started',
            type: 'PAGE',
            parentId: null,
            order: 1,
            slug: 'getting-started-def456',
            published: true,
        };
        await saveNavItem(secondOverview);
        await savePageContent({
            id: 'draft-content-2',
            portalId: DRAFT_PORTAL_ID,
            navigationItemId: 'draft-page-2',
            document: [{ id: 'block-2', type: 'paragraph', content: [], children: [] }],
        });

        await publishApiDocumentationToPortal({
            apiId: 'api-1',
            apiName: 'Payments API',
            draftPortalId: DRAFT_PORTAL_ID,
            portalId: PORTAL_ID,
            parentId: null,
            mode: 'replace',
        });

        const portalItems = await getNavItems(PORTAL_ID);
        const apiNode = portalItems.find((item): item is PortalNavigationApi => item.type === 'API' && item.apiId === 'api-1');
        const children = portalItems.filter(item => item.parentId === apiNode?.id);

        expect(children).toHaveLength(2);
        expect(children.map(item => item.title).sort()).toEqual(['Getting Started', 'Overview']);
    });
});
