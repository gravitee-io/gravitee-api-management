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
    allowedChanges,
    buildDocumentationRows,
    defaultPublishSectionId,
    fetchableItemIds,
    replaceablePageIds,
    getAncestors,
    getHiddenListing,
    getPublishedSection,
    syncStateOf,
} from './documentationTree';
import type {
    ApiDocumentationItem,
    ApiPortalPublication,
    ApiPortalPublishLocation,
    PortalNavigationItemSource,
} from '../types/apiDocumentation';

const SOURCE: PortalNavigationItemSource = { type: 'github-fetcher', configuration: {} };

function folder(id: string, order: number, parentId?: string, source?: PortalNavigationItemSource): ApiDocumentationItem {
    return { ...base(id, order, parentId), type: 'FOLDER', source };
}

function page(id: string, order: number, parentId?: string, source?: PortalNavigationItemSource): ApiDocumentationItem {
    return { ...base(id, order, parentId), type: 'PAGE', portalPageContentId: `content-${id}`, source };
}

function base(id: string, order: number, parentId?: string) {
    return {
        id,
        organizationId: 'DEFAULT',
        environmentId: 'DEFAULT',
        title: id,
        area: 'TOP_NAVBAR' as const,
        parentId,
        rootId: parentId ?? id,
        order,
        published: false,
        visibility: 'PUBLIC' as const,
    };
}

function publication(sectionName: string, published: boolean): ApiPortalPublication {
    return {
        portalId: 'DEFAULT',
        sectionName,
        portalNavigationItem: { ...base(`listing-${sectionName}`, 0, `section-${sectionName}`), published, type: 'API', apiId: 'api-1' },
    };
}

const ids = (rows: ReturnType<typeof buildDocumentationRows>) => rows.map(row => row.item.id);

describe('buildDocumentationRows', () => {
    it('lists children right under their parent, siblings in their order, at any depth', () => {
        const items = [
            page('changelog', 1),
            page('oauth', 0, 'auth'),
            folder('guides', 0),
            folder('auth', 1, 'guides'),
            page('getting-started', 0, 'guides'),
        ];

        const rows = buildDocumentationRows(items, new Set(['guides', 'auth']));

        expect(ids(rows)).toEqual(['guides', 'getting-started', 'auth', 'oauth', 'changelog']);
        expect(rows.map(row => row.depth)).toEqual([0, 1, 1, 2, 0]);
    });

    it('collapses every folder that has not been expanded', () => {
        const items = [folder('guides', 0), page('getting-started', 0, 'guides'), page('changelog', 1)];

        const rows = buildDocumentationRows(items, new Set());

        expect(ids(rows)).toEqual(['guides', 'changelog']);
        expect(rows[0]).toMatchObject({ hasChildren: true, expanded: false });
    });

    it('shows the contents of an expanded folder, keeping its collapsed subfolders closed', () => {
        const items = [folder('guides', 0), folder('auth', 0, 'guides'), page('oauth', 0, 'auth'), page('changelog', 1)];

        const rows = buildDocumentationRows(items, new Set(['guides']));

        expect(ids(rows)).toEqual(['guides', 'auth', 'changelog']);
        expect(rows[0]).toMatchObject({ expanded: true });
        expect(rows[1]).toMatchObject({ expanded: false });
    });

    it('records how each item is synced: its own source, a synced folder above it, or neither', () => {
        const items = [
            folder('reference', 0, undefined, SOURCE),
            folder('v1', 0, 'reference'),
            page('endpoints', 0, 'v1', SOURCE),
            page('changelog', 1),
        ];

        const rows = buildDocumentationRows(items, new Set(['reference', 'v1']));

        expect(rows.map(row => [row.item.id, row.syncState])).toEqual([
            ['reference', 'OWN_SOURCE'],
            ['v1', 'BELOW_SYNCED_FOLDER'],
            ['endpoints', 'BELOW_SYNCED_FOLDER'],
            ['changelog', 'NOT_SYNCED'],
        ]);
    });

    it('counts every descendant of a folder, even when it is collapsed', () => {
        const items = [folder('guides', 0), folder('auth', 0, 'guides'), page('oauth', 0, 'auth'), page('keys', 1, 'auth')];

        const rows = buildDocumentationRows(items, new Set());

        expect(rows[0]).toMatchObject({ item: expect.objectContaining({ id: 'guides' }), descendantCount: 3 });
    });

    it('returns no rows when the API has no documentation', () => {
        expect(buildDocumentationRows([], new Set())).toEqual([]);
    });

    it('handles a folder hierarchy deeper than the call stack', () => {
        const depth = 50_000;
        const items = Array.from({ length: depth }, (_, level) =>
            folder(`level-${level}`, 0, level === 0 ? undefined : `level-${level - 1}`),
        );

        const rows = buildDocumentationRows(items, new Set(items.map(item => item.id)));

        expect(rows).toHaveLength(depth);
        expect(rows[0]).toMatchObject({ depth: 0, descendantCount: depth - 1 });
        expect(rows[depth - 1]).toMatchObject({ depth: depth - 1, descendantCount: 0 });
    });
});

describe('syncStateOf', () => {
    it('is not synced without a source on the item or above it', () => {
        expect(syncStateOf(page('oauth', 0, 'guides'), [folder('guides', 0)])).toBe('NOT_SYNCED');
    });

    it('is synced by its own source', () => {
        expect(syncStateOf(page('petstore', 0, 'guides', SOURCE), [folder('guides', 0)])).toBe('OWN_SOURCE');
    });

    it('is below a synced folder when any folder above it has a source, even with a source of its own', () => {
        const ancestors = [folder('reference', 0, undefined, SOURCE), folder('v1', 0, 'reference')];

        expect(syncStateOf(page('endpoints', 0, 'v1'), ancestors)).toBe('BELOW_SYNCED_FOLDER');
        expect(syncStateOf(page('endpoints', 0, 'v1', SOURCE), ancestors)).toBe('BELOW_SYNCED_FOLDER');
    });
});

// Mirrors what the server's manual fetch accepts.
describe('replaceablePageIds', () => {
    it('is the fetched page itself', () => {
        expect(replaceablePageIds([page('petstore', 0, undefined, SOURCE)], 'petstore')).toEqual(['petstore']);
    });

    it('is every synced page below a fetched folder, leaving out the pages written by hand', () => {
        const items = [
            folder('guides', 0),
            page('petstore', 0, 'guides', SOURCE),
            page('oauth', 1, 'guides'),
            folder('reference', 2, 'guides', { ...SOURCE, subtreeImport: true }),
            page('endpoints', 0, 'reference'),
            page('elsewhere', 1, undefined, SOURCE),
        ];

        expect(replaceablePageIds(items, 'guides')).toEqual(['petstore', 'endpoints']);
    });

    it('is the fetched item alone when the documentation is not known', () => {
        expect(replaceablePageIds([], 'petstore')).toEqual(['petstore']);
    });
});

describe('fetchableItemIds', () => {
    const IMPORTED_SOURCE: PortalNavigationItemSource = { ...SOURCE, subtreeImport: true };

    it('includes a page or an imported folder with a source of its own', () => {
        const items = [page('petstore', 0, undefined, SOURCE), folder('reference', 1, undefined, IMPORTED_SOURCE)];

        expect(fetchableItemIds(items)).toEqual(new Set(['petstore', 'reference']));
    });

    it('leaves out a folder with a source it was not imported from, holding no page with a source', () => {
        const items = [folder('reference', 0, undefined, SOURCE), page('endpoints', 0, 'reference')];

        expect(fetchableItemIds(items)).toEqual(new Set());
    });

    it('includes a plain folder holding a page with a source at any depth', () => {
        const items = [folder('guides', 0), folder('v1', 0, 'guides'), page('petstore', 0, 'v1', SOURCE)];

        expect(fetchableItemIds(items)).toEqual(new Set(['guides', 'v1', 'petstore']));
    });

    it('leaves out a plain folder holding no page with a source', () => {
        expect(fetchableItemIds([folder('guides', 0), page('oauth', 0, 'guides')])).toEqual(new Set());
    });

    it('leaves out what is below a synced folder, which is fetched through that folder', () => {
        const items = [
            folder('reference', 0, undefined, IMPORTED_SOURCE),
            folder('v1', 0, 'reference'),
            page('endpoints', 0, 'v1', SOURCE),
        ];

        expect(fetchableItemIds(items)).toEqual(new Set(['reference']));
    });

    it('leaves out an imported folder below another synced folder, which is fetched through the folder above', () => {
        const items = [folder('reference', 0, undefined, SOURCE), folder('sdk', 0, 'reference', IMPORTED_SOURCE)];

        expect(fetchableItemIds(items)).toEqual(new Set());
    });

    it('includes a folder with a source it was not imported from when it holds a page with a source', () => {
        const items = [folder('reference', 0, undefined, SOURCE), folder('v1', 0, 'reference'), page('endpoints', 0, 'v1', SOURCE)];

        expect(fetchableItemIds(items)).toEqual(new Set(['reference']));
    });
});

// Mirrors the server's SourcedItemReadOnlyRule.
describe('allowedChanges', () => {
    it('allows every change to an item that is not synced', () => {
        expect(allowedChanges('NOT_SYNCED')).toEqual({
            content: true,
            title: true,
            access: true,
            publication: true,
            delete: true,
            addInside: true,
        });
    });

    it('allows an item with its own source to change its access and publication, and to be deleted', () => {
        expect(allowedChanges('OWN_SOURCE')).toEqual({
            content: false,
            title: false,
            access: true,
            publication: true,
            delete: true,
            addInside: false,
        });
    });

    it('allows no change below a synced folder', () => {
        expect(allowedChanges('BELOW_SYNCED_FOLDER')).toEqual({
            content: false,
            title: false,
            access: false,
            publication: false,
            delete: false,
            addInside: false,
        });
    });
});

describe('getAncestors', () => {
    const items = [folder('guides', 0), folder('auth', 0, 'guides'), page('oauth', 0, 'auth'), page('changelog', 1)];

    it('lists the folders above an item, from the top level down', () => {
        expect(getAncestors(items, items[2]!).map(item => item.id)).toEqual(['guides', 'auth']);
    });

    it('is empty for a top-level item', () => {
        expect(getAncestors(items, items[3]!)).toEqual([]);
    });

    it('stops at a parent missing from the list', () => {
        expect(getAncestors([page('orphan', 0, 'gone')], page('orphan', 0, 'gone'))).toEqual([]);
    });

    it('stops when the parents loop back on themselves', () => {
        const loop = [folder('a', 0, 'b'), folder('b', 0, 'a')];
        expect(getAncestors(loop, page('leaf', 0, 'a')).map(item => item.id)).toEqual(['b', 'a']);
    });
});

describe('getPublishedSection', () => {
    it('names the section of a visible listing', () => {
        expect(getPublishedSection([publication('Hidden one', false), publication('APIs', true)])).toBe('APIs');
    });

    it('is undefined when the API is not listed', () => {
        expect(getPublishedSection([])).toBeUndefined();
    });

    it('is undefined when every listing is hidden', () => {
        expect(getPublishedSection([publication('APIs', false)])).toBeUndefined();
    });
});

describe('getHiddenListing', () => {
    it('finds the listing that is not visible', () => {
        const hidden = publication('Partner APIs', false);

        expect(getHiddenListing([publication('APIs', true), hidden])).toBe(hidden);
    });

    it('is undefined when the only listing is visible', () => {
        expect(getHiddenListing([publication('APIs', true)])).toBeUndefined();
    });

    it('is undefined when the API is not listed', () => {
        expect(getHiddenListing([])).toBeUndefined();
    });
});

describe('defaultPublishSectionId', () => {
    const location = (name: string): ApiPortalPublishLocation => ({ id: `section-${name}`, name });
    const GUIDES = location('Guides');
    const APIS = location('APIs');
    const PARTNERS = location('Partner APIs');

    it('keeps the section a hidden listing is already in', () => {
        expect(defaultPublishSectionId([GUIDES, APIS, PARTNERS], [publication('Partner APIs', false)])).toBe(PARTNERS.id);
    });

    it('falls back to the section named APIs when the hidden listing is somewhere it cannot be published to', () => {
        expect(defaultPublishSectionId([GUIDES, APIS], [publication('Nested folder', false)])).toBe(APIS.id);
    });

    it('prefers the section named APIs over the first one', () => {
        expect(defaultPublishSectionId([GUIDES, APIS], [])).toBe(APIS.id);
    });

    it('takes the first section when none is named APIs', () => {
        expect(defaultPublishSectionId([GUIDES, PARTNERS], [])).toBe(GUIDES.id);
    });

    it('is undefined when there is no section to publish to', () => {
        expect(defaultPublishSectionId([], [publication('Partner APIs', false)])).toBeUndefined();
    });
});
