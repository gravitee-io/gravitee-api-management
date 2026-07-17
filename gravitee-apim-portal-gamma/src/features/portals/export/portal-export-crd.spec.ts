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
jest.mock('../../editor/gmd/gmd-content', () => ({
    serializeDocumentToGmd: jest.fn(() => 'gmd'),
}));

jest.mock('../../editor/utils/markdown-to-blocks', () => ({
    looksLikeMarkdown: () => false,
    markdownToBlocks: () => [],
}));

import { createDefaultTheme } from '../../theming/storage/default-theme';
import { createDummyPortals } from '../storage/dummy-portals';
import { buildNavItem } from '../storage/navigation-items.storage.test-utils';
import { buildPageContent } from '../storage/page-contents.storage.test-utils';
import { buildNavPaths } from './build-nav-paths';
import { buildPortalCrdDocuments, exportPortalToCrdsYaml } from './portal-export-crd';
import type { PortalExportBundle } from './portal-export.types';

function createBundle(overrides: Partial<PortalExportBundle> = {}): PortalExportBundle {
    const portal = createDummyPortals()[0];
    const { screenshotDataUrl: _screenshot, ...portalWithoutScreenshot } = portal;

    const folder = buildNavItem({
        id: 'nav-folder',
        portalId: portal.id,
        title: 'APIs',
        type: 'FOLDER',
        slug: 'apis',
        order: 0,
    });
    const apiItem = buildNavItem({
        id: 'nav-api',
        portalId: portal.id,
        title: 'Payments API',
        type: 'API',
        parentId: 'nav-folder',
        slug: 'payments-api',
        order: 0,
        apiId: 'payments-api-hrid',
    } as Partial<import('../types').PortalNavigationApi>);
    const blockPage = buildNavItem({
        id: 'nav-home',
        portalId: portal.id,
        title: 'Home',
        type: 'PAGE',
        parentId: 'nav-folder',
        slug: 'home',
        order: 1,
    });
    const openApiPage = buildNavItem({
        id: 'nav-spec',
        portalId: portal.id,
        title: 'OpenAPI',
        type: 'PAGE',
        contentType: 'OPENAPI',
        renderer: 'swagger',
        specSource: { type: 'INLINE', content: '{"openapi":"3.0.0"}' },
        parentId: 'nav-folder',
        slug: 'openapi',
        order: 2,
    } as Partial<import('../types').PortalNavigationOpenApiPage>);

    return {
        formatVersion: '1',
        exportedAt: '2026-06-29T12:00:00.000Z',
        portal: portalWithoutScreenshot,
        navigation: [folder, apiItem, blockPage, openApiPage],
        pageContents: [
            buildPageContent({
                portalId: portal.id,
                navigationItemId: 'nav-home',
                blockStyles: { blockA: { fontSize: '16px' } },
            }),
            {
                id: 'content-spec',
                portalId: portal.id,
                navigationItemId: 'nav-spec',
                contentType: 'OPENAPI',
                specContent: '{"openapi":"3.0.0","info":{"title":"Test"}}',
                renderer: 'swagger',
            },
        ],
        theme: createDefaultTheme(portal.id),
        ...overrides,
    };
}

describe('buildNavPaths', () => {
    it('should build nested slash paths from slugs', () => {
        const bundle = createBundle();
        const paths = buildNavPaths(bundle.navigation);

        expect(paths.get('nav-folder')).toBe('/apis');
        expect(paths.get('nav-api')).toBe('/apis/payments-api');
        expect(paths.get('nav-home')).toBe('/apis/home');
        expect(paths.get('nav-spec')).toBe('/apis/openapi');
    });
});

describe('portal-export-crd', () => {
    it('should emit Portal, PortalListing, PortalBlockPage, PortalPage, PortalDocumentation, and PortalTheme documents', () => {
        const bundle = createBundle();
        const documents = buildPortalCrdDocuments(bundle);
        const kinds = documents.map(document => document.kind);

        expect(kinds).toContain('Portal');
        expect(kinds).toContain('PortalListing');
        expect(kinds).toContain('PortalBlockPage');
        expect(kinds).toContain('PortalPage');
        expect(kinds).toContain('PortalDocumentation');
        expect(kinds).toContain('PortalTheme');
    });

    it('should map API nav items to PortalListing entries', () => {
        const bundle = createBundle();
        const listing = buildPortalCrdDocuments(bundle).find(document => document.kind === 'PortalListing');

        expect(listing?.spec.apis).toEqual([
            {
                apiHrid: 'payments-api-hrid',
                location: '/apis',
                order: 0,
            },
        ]);
    });

    it('should map API Product nav items to PortalListing apiProducts entries', () => {
        const bundle = createBundle();
        const productItem = buildNavItem({
            id: 'nav-product',
            portalId: bundle.portal.id,
            title: 'Commerce Platform',
            type: 'API_PRODUCT',
            parentId: 'nav-folder',
            slug: 'commerce-platform',
            order: 2,
            apiProductId: 'product-commerce',
        } as Partial<import('../types').PortalNavigationApiProduct>);
        const documents = buildPortalCrdDocuments({
            ...bundle,
            navigation: [...bundle.navigation, productItem],
        });
        const portal = documents.find(document => document.kind === 'Portal');
        const listing = documents.find(document => document.kind === 'PortalListing');

        expect(
            (portal?.spec.navigation as Array<Record<string, unknown>>).find(entry => entry.type === 'API_PRODUCT'),
        ).toMatchObject({
            apiProductId: 'product-commerce',
            displayName: 'Commerce Platform',
        });
        expect(listing?.spec.apiProducts).toEqual([
            {
                apiProductHrid: 'product-commerce',
                location: '/apis',
                order: 2,
            },
        ]);
    });

    it('should include block document in PortalBlockPage without blockStyles', () => {
        const bundle = createBundle();
        const blockPage = buildPortalCrdDocuments(bundle).find(document => document.kind === 'PortalBlockPage');

        expect(blockPage?.spec.location).toBe('/apis/home');
        expect(blockPage?.spec.blockStyles).toBeUndefined();
        expect(blockPage?.spec.document).toBeDefined();
    });

    it('should export PortalPage markup for block pages', () => {
        const bundle = createBundle();
        const portalPage = buildPortalCrdDocuments(bundle).find(document => document.kind === 'PortalPage');

        expect(portalPage?.spec.location).toBe('/apis/home');
        expect(portalPage?.spec.content).toBeDefined();
    });

    it('should export theme with foundation and schemaVersion', () => {
        const bundle = createBundle();
        const themeDoc = buildPortalCrdDocuments(bundle).find(document => document.kind === 'PortalTheme');

        expect(themeDoc?.spec.schemaVersion).toBe(1);
        expect(themeDoc?.spec.foundation).toBeDefined();
        expect(themeDoc?.spec.elements).toBeDefined();
        expect(themeDoc?.spec.tokens).toBeUndefined();
    });

    it('should serialize multi-document YAML separated by document markers', () => {
        const yaml = exportPortalToCrdsYaml(createBundle());

        expect(yaml).toContain('apiVersion: gravitee.io/v1alpha1');
        expect(yaml).toContain('kind: Portal');
        expect(yaml).toContain('kind: PortalTheme');
        expect(yaml.split('---').length).toBeGreaterThanOrEqual(4);
    });
});
