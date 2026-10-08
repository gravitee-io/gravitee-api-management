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
import { buildDocumentationRows, getAncestors, getPublishedSection } from './documentationTree';
import type { ApiDocumentationItem, ApiPortalPublication, PortalNavigationItemSource } from '../types/apiDocumentation';

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

    it('marks as synced an item with a source and everything below it, but not its siblings', () => {
        const items = [
            folder('reference', 0, undefined, SOURCE),
            folder('v1', 0, 'reference'),
            page('endpoints', 0, 'v1'),
            page('changelog', 1),
        ];

        const rows = buildDocumentationRows(items, new Set(['reference', 'v1']));

        expect(rows.map(row => [row.item.id, row.synced])).toEqual([
            ['reference', true],
            ['v1', true],
            ['endpoints', true],
            ['changelog', false],
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
