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
import type { ApiDocumentationItem, ApiPortalPublication, ApiPortalPublishLocation } from '../types/apiDocumentation';

export interface DocumentationRow {
    item: ApiDocumentationItem;
    depth: number;
    hasChildren: boolean;
    expanded: boolean;
    syncState: SyncState;
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
    const toVisit = [...(childrenByParent.get(undefined) ?? [])].reverse().map(item => ({ item, depth: 0, belowSource: false }));
    let next = toVisit.pop();
    while (next) {
        const { item, depth, belowSource } = next;
        const children = childrenByParent.get(item.id) ?? [];
        const syncState = belowSource ? 'BELOW_SYNCED_FOLDER' : hasSource(item) ? 'OWN_SOURCE' : 'NOT_SYNCED';
        const expanded = expandedIds.has(item.id);
        rows.push({
            item,
            depth,
            hasChildren: children.length > 0,
            expanded,
            syncState,
            descendantCount: descendantCounts.get(item.id) ?? 0,
        });
        if (expanded) {
            for (const child of [...children].reverse()) {
                toVisit.push({ item: child, depth: depth + 1, belowSource: syncState !== 'NOT_SYNCED' });
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

/** The folders above an item, from the top level down. Stops at a parent missing from the list, or one already seen. */
export function getAncestors(items: ApiDocumentationItem[], item: ApiDocumentationItem): ApiDocumentationItem[] {
    const byId = new Map(items.map(candidate => [candidate.id, candidate]));
    const ancestors: ApiDocumentationItem[] = [];
    const seen = new Set<string>([item.id]);
    let parent = item.parentId === undefined ? undefined : byId.get(item.parentId);
    while (parent && !seen.has(parent.id)) {
        seen.add(parent.id);
        ancestors.unshift(parent);
        parent = parent.parentId === undefined ? undefined : byId.get(parent.parentId);
    }
    return ancestors;
}

export function hasSource(item: ApiDocumentationItem): boolean {
    return item.type !== 'LINK' && item.source !== undefined;
}

/** How an item is kept in step with an external source. A synced folder owns everything below it, sources included. */
export type SyncState = 'NOT_SYNCED' | 'OWN_SOURCE' | 'BELOW_SYNCED_FOLDER';

export function syncStateOf(item: ApiDocumentationItem, ancestors: ApiDocumentationItem[]): SyncState {
    if (ancestors.some(hasSource)) return 'BELOW_SYNCED_FOLDER';
    return hasSource(item) ? 'OWN_SOURCE' : 'NOT_SYNCED';
}

/**
 * The items a manual fetch accepts, as the server decides: a page with a source, a folder imported from one (which
 * imports again), and any other folder holding a page with a source, whose fetch refreshes every such page below it —
 * a folder with a source it was not imported from included. What a synced folder holds is fetched through that folder.
 */
export function fetchableItemIds(items: ApiDocumentationItem[]): Set<string> {
    const fetchable = new Set<string>();
    for (const item of items) {
        if (!hasSource(item)) continue;
        const ancestors = getAncestors(items, item);
        const firstSynced = ancestors.findIndex(hasSource);
        if (item.type === 'PAGE') {
            const reachable = firstSynced === -1 ? [...ancestors, item] : ancestors.slice(0, firstSynced + 1);
            reachable.forEach(reached => fetchable.add(reached.id));
        } else if (item.type === 'FOLDER' && firstSynced === -1 && item.source?.subtreeImport) {
            fetchable.add(item.id);
        }
    }
    return fetchable;
}

/**
 * The pages whose content a fetch of the given item can replace: the item itself, and the synced pages below it. A page
 * written by hand is never replaced, and may hold a draft. Without the documentation, only the item itself.
 */
export function replaceablePageIds(items: ApiDocumentationItem[], navId: string): string[] {
    if (!items.some(item => item.id === navId)) return [navId];
    return items
        .filter(item => item.type === 'PAGE')
        .filter(item => {
            const ancestors = getAncestors(items, item);
            const inFetchedItem = item.id === navId || ancestors.some(ancestor => ancestor.id === navId);
            return inFetchedItem && syncStateOf(item, ancestors) !== 'NOT_SYNCED';
        })
        .map(item => item.id);
}

export interface AllowedChanges {
    content: boolean;
    title: boolean;
    access: boolean;
    publication: boolean;
    delete: boolean;
    addInside: boolean;
}

/**
 * What the server lets change on an item in this state, before permissions — its SourcedItemReadOnlyRule: an item
 * with its own source only changes its access and whether it is published, or is deleted with what it holds, and
 * nothing below a synced folder changes at all.
 */
export function allowedChanges(state: SyncState): AllowedChanges {
    const notSynced = state === 'NOT_SYNCED';
    const ownedByItself = state !== 'BELOW_SYNCED_FOLDER';
    return {
        content: notSynced,
        title: notSynced,
        access: ownedByItself,
        publication: ownedByItself,
        delete: ownedByItself,
        addInside: notSynced,
    };
}

/** The section the API is visible under, if any: a hidden listing does not make the API published. */
export function getPublishedSection(publications: ApiPortalPublication[]): string | undefined {
    return publications.find(publication => publication.portalNavigationItem.published)?.sectionName;
}

/** A listing kept hidden, typically added in the portal editor: publishing the API reuses it rather than adding another. */
export function getHiddenListing(publications: ApiPortalPublication[]): ApiPortalPublication | undefined {
    return publications.find(publication => !publication.portalNavigationItem.published);
}

/** The section to offer first: where a hidden listing already sits, else the one named "APIs", else the first. */
export function defaultPublishSectionId(locations: ApiPortalPublishLocation[], publications: ApiPortalPublication[]): string | undefined {
    const hiddenListingSectionId = getHiddenListing(publications)?.portalNavigationItem.parentId;
    const preferred =
        locations.find(location => location.id === hiddenListingSectionId) ?? locations.find(location => location.name === 'APIs');
    return (preferred ?? locations[0])?.id;
}
