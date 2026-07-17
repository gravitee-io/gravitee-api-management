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
import { getPageContent, getPageContentsForPortal, savePageContent } from '@apim/portal-editor/portals/storage/page-contents.storage';
import type { PortalNavigationPage } from '@apim/portal-editor/portals/types';
import {
    ensureApiDocumentationDraft,
    getApiDocumentationDraftPortalId,
    getPublishPreferences,
    savePublishPreferences,
} from './api-documentation.storage';

jest.mock('@apim/portal-editor/blocks/ApiSpecBlock/api-ref-page-generator', () => ({
    buildTagPageDefinitions: jest.fn(async () => ({
        overviewDocument: [{ id: 'block-1', type: 'paragraph', content: [], children: [] }],
        tagPages: [
            {
                tag: 'Users',
                title: 'Users',
                document: [{ id: 'block-2', type: 'paragraph', content: [], children: [] }],
            },
        ],
    })),
}));

jest.mock('@apim/portal-editor/editor/gmd/gmd-content', () => ({
    serializeDocumentToGmd: jest.fn(() => ''),
}));

installFakeIndexedDB();

describe('api-documentation.storage', () => {
    beforeEach(() => {
        resetFakeIndexedDB();
        localStorage.clear();
    });

    it('should derive a stable draft portal id from the api id', () => {
        expect(getApiDocumentationDraftPortalId('api-123')).toBe('api-doc-api-123');
    });

    it('should seed overview and tag pages on first open', async () => {
        const portalId = await ensureApiDocumentationDraft('api-123', 'Payments API');

        expect(portalId).toBe('api-doc-api-123');

        const navItems = await getNavItems(portalId);
        expect(navItems).toHaveLength(2);
        expect(navItems.map(item => item.title)).toEqual(['Overview', 'Users']);
        expect(navItems.map(item => item.id)).toEqual([
            'api-doc-api-123-overview',
            'api-doc-api-123-tag-users',
        ]);

        const contents = await getPageContentsForPortal(portalId);
        expect(contents).toHaveLength(2);
    });

    it('should not reseed when draft already exists', async () => {
        await ensureApiDocumentationDraft('api-123', 'Payments API');
        await ensureApiDocumentationDraft('api-123', 'Payments API');

        const navItems = await getNavItems('api-doc-api-123');
        expect(navItems).toHaveLength(2);
    });

    it('should not create duplicate pages when called concurrently', async () => {
        const portalId = getApiDocumentationDraftPortalId('api-123');

        await Promise.all([
            ensureApiDocumentationDraft('api-123', 'Payments API'),
            ensureApiDocumentationDraft('api-123', 'Payments API'),
        ]);

        const navItems = await getNavItems(portalId);
        expect(navItems).toHaveLength(2);
        expect(new Set(navItems.map(item => item.title)).size).toBe(2);
    });

    it('should wipe and reseed when duplicate root page titles are detected', async () => {
        const portalId = await ensureApiDocumentationDraft('api-123', 'Payments API');

        const duplicateOverview: PortalNavigationPage = {
            id: 'api-doc-api-123-overview-duplicate',
            portalId,
            title: 'Overview',
            type: 'PAGE',
            parentId: null,
            order: 99,
            slug: 'overview-duplicate',
            published: true,
        };
        await saveNavItem(duplicateOverview);

        const navItemsBeforeHeal = await getNavItems(portalId);
        expect(navItemsBeforeHeal.filter(item => item.title === 'Overview')).toHaveLength(2);

        await ensureApiDocumentationDraft('api-123', 'Payments API');

        const navItems = await getNavItems(portalId);
        expect(navItems).toHaveLength(2);
        expect(navItems.map(item => item.title)).toEqual(['Overview', 'Users']);
        expect(navItems.map(item => item.id)).toEqual([
            'api-doc-api-123-overview',
            'api-doc-api-123-tag-users',
        ]);
    });

    it('should wipe and reseed when overview content still uses a legacy markdown paragraph', async () => {
        const portalId = await ensureApiDocumentationDraft('api-123', 'Payments API');
        const overviewPageId = 'api-doc-api-123-overview';

        await savePageContent({
            id: `page-content-${overviewPageId}`,
            portalId,
            navigationItemId: overviewPageId,
            document: [
                { id: 'heading', type: 'heading', props: { level: 1 }, content: [{ type: 'text', text: 'Overview', styles: {} }], children: [] },
                { id: 'name', type: 'graviteeApiMetadata', props: { field: 'name' }, children: [] },
                { id: 'version', type: 'graviteeApiMetadata', props: { field: 'version' }, children: [] },
                { id: 'description', type: 'graviteeApiMetadata', props: { field: 'description' }, children: [] },
                {
                    id: 'legacy-paragraph',
                    type: 'paragraph',
                    content: [{ type: 'text', text: '## Getting started\n\n1. Create an application.', styles: {} }],
                    children: [],
                },
            ],
            gmd: '',
        });

        await ensureApiDocumentationDraft('api-123', 'Payments API');

        const overviewContent = await getPageContent(overviewPageId);
        expect(overviewContent?.document).toEqual([
            { id: 'block-1', type: 'paragraph', content: [], children: [] },
        ]);
    });

    it('should persist publish preferences per api', () => {
        savePublishPreferences('api-123', { portalId: 'portal-1', parentId: 'folder-1' });

        expect(getPublishPreferences('api-123')).toEqual({
            portalId: 'portal-1',
            parentId: 'folder-1',
        });
        expect(getPublishPreferences('api-999')).toBeNull();
    });
});
