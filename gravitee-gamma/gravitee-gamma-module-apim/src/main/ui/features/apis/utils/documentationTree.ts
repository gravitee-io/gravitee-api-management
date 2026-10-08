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
import type { ApiDocumentationItem, ApiPortalPublication } from '../types/apiDocumentation';

export interface DocumentationRow {
    item: ApiDocumentationItem;
    depth: number;
    hasChildren: boolean;
    expanded: boolean;
    /** The item has an external source, or sits below one: the server refuses to delete it. */
    synced: boolean;
    descendantCount: number;
}

/**
 * Orders the API's documentation into the rows the table displays.
 *
 * Input: the flat list the server returns, in no display order. Each item only knows its `parentId` and its
 * `order` among its siblings, so the list cannot be drawn as is.
 *
 *     items = [Changelog (top, order 1), OAuth setup (in Auth), Guides (top, order 0), Auth (in Guides), Getting started (in Guides)]
 *     expandedIds = {Guides}
 *
 * Output: one row per visible item, each folder followed by its contents, siblings by `order`, with a depth
 * for indentation. The contents of a folder missing from `expandedIds` are left out.
 *
 *     Guides             depth 0, expanded
 *       Getting started  depth 1
 *       Auth             depth 1, collapsed: OAuth setup left out
 *     Changelog          depth 0
 */
export function buildDocumentationRows(items: ApiDocumentationItem[], expandedIds: ReadonlySet<string>): DocumentationRow[] {
    const childrenByParent = new Map<string | undefined, ApiDocumentationItem[]>();
    for (const item of items) {
        const siblings = childrenByParent.get(item.parentId) ?? [];
        siblings.push(item);
        childrenByParent.set(item.parentId, siblings);
    }
    for (const siblings of childrenByParent.values()) {
        siblings.sort((a, b) => a.order - b.order);
    }

    const descendantCounts = countDescendants(childrenByParent);

    // Walked with an explicit stack: recursion would exhaust the call stack on a deep enough hierarchy.
    const rows: DocumentationRow[] = [];
    const toVisit = [...(childrenByParent.get(undefined) ?? [])].reverse().map(item => ({ item, depth: 0, underSource: false }));
    let next = toVisit.pop();
    while (next) {
        const { item, depth, underSource } = next;
        const children = childrenByParent.get(item.id) ?? [];
        const synced = underSource || hasSource(item);
        const expanded = expandedIds.has(item.id);
        rows.push({ item, depth, hasChildren: children.length > 0, expanded, synced, descendantCount: descendantCounts.get(item.id) ?? 0 });
        if (expanded) {
            for (const child of [...children].reverse()) {
                toVisit.push({ item: child, depth: depth + 1, underSource: synced });
            }
        }
        next = toVisit.pop();
    }
    return rows;
}

/** Counts every item below each folder in one pass, by adding each item's count to its parent's, children first. */
function countDescendants(childrenByParent: Map<string | undefined, ApiDocumentationItem[]>): Map<string, number> {
    const parentsFirst: ApiDocumentationItem[] = [];
    const toVisit = [...(childrenByParent.get(undefined) ?? [])];
    let item = toVisit.pop();
    while (item) {
        parentsFirst.push(item);
        for (const child of childrenByParent.get(item.id) ?? []) {
            toVisit.push(child);
        }
        item = toVisit.pop();
    }

    const counts = new Map<string, number>();
    for (const child of parentsFirst.reverse()) {
        if (child.parentId !== undefined) {
            counts.set(child.parentId, (counts.get(child.parentId) ?? 0) + 1 + (counts.get(child.id) ?? 0));
        }
    }
    return counts;
}

export function hasSource(item: ApiDocumentationItem): boolean {
    return item.type !== 'LINK' && item.source !== undefined;
}

/** The section the API is visible under, if any: a hidden listing does not make the API published. */
export function getPublishedSection(publications: ApiPortalPublication[]): string | undefined {
    return publications.find(publication => publication.portalNavigationItem.published)?.sectionName;
}
