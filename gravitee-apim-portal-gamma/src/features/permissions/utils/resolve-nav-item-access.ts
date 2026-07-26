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
import type { PortalNavigationItem } from '../../portals/types/navigation-item.types';
import type { PortalAccessGrant, PortalAccessLevel, PortalGrantScopeType } from '../types/permissions.types';

export type ResolvedAccess = PortalAccessLevel | 'NONE';

export interface ResolvedNavItemAccess {
    access: ResolvedAccess;
    /** True when the access comes straight from the enclosing scope rather than an override. */
    inherited: boolean;
    /** The grant the access was derived from, or null when nothing grants this item. */
    grant: PortalAccessGrant | null;
}

const ASSET_TYPES: readonly PortalGrantScopeType[] = ['API', 'API_PRODUCT', 'AI_WORKSPACE'];

/** The asset a navigation item points at, when it is an asset node rather than a page or folder. */
export function getNavItemAssetId(item: PortalNavigationItem): string | undefined {
    switch (item.type) {
        case 'API':
            return item.apiId;
        case 'API_PRODUCT':
            return item.apiProductId;
        case 'AI_WORKSPACE':
            return item.aiWorkspaceId;
        default:
            return undefined;
    }
}

export function isAssetNavItem(item: PortalNavigationItem): boolean {
    return ASSET_TYPES.includes(item.type as PortalGrantScopeType);
}

function findAssetAncestor(
    item: PortalNavigationItem,
    itemsById: ReadonlyMap<string, PortalNavigationItem>,
): PortalNavigationItem | undefined {
    let current: PortalNavigationItem | undefined = item;
    const visited = new Set<string>();

    while (current && !visited.has(current.id)) {
        visited.add(current.id);

        if (isAssetNavItem(current)) {
            return current;
        }

        current = current.parentId ? itemsById.get(current.parentId) : undefined;
    }

    return undefined;
}

function findGrantForScope(
    grants: readonly PortalAccessGrant[],
    scopeType: PortalGrantScopeType,
    scopeId: string,
): PortalAccessGrant | undefined {
    return grants.find(grant => grant.scopeType === scopeType && grant.scopeId === scopeId);
}

/**
 * A navigation item inherits the access of its nearest enclosing asset, or of its portal when it
 * sits outside any asset. Stored overrides then narrow (or widen) individual nodes.
 */
export function resolveNavItemAccess(
    item: PortalNavigationItem,
    itemsById: ReadonlyMap<string, PortalNavigationItem>,
    grants: readonly PortalAccessGrant[],
): ResolvedNavItemAccess {
    const assetAncestor = findAssetAncestor(item, itemsById);

    const grant = assetAncestor
        ? findGrantForScope(
              grants,
              assetAncestor.type as PortalGrantScopeType,
              getNavItemAssetId(assetAncestor) ?? '',
          )
        : findGrantForScope(grants, 'PORTAL', item.portalId);

    if (!grant) {
        return { access: 'NONE', inherited: true, grant: null };
    }

    const override = grant.overrides.find(entry => entry.navigationItemId === item.id);
    if (override) {
        return { access: override.access, inherited: false, grant };
    }

    return { access: grant.access, inherited: true, grant };
}

export interface AccessTreeRow {
    item: PortalNavigationItem;
    depth: number;
    resolved: ResolvedNavItemAccess;
}

/**
 * The navigation items governed by a grant, depth-first and in navigation order.
 * Nested asset nodes are excluded because each asset owns its own grant row.
 */
export function flattenGrantScopeTree(
    grant: PortalAccessGrant,
    items: readonly PortalNavigationItem[],
    grants: readonly PortalAccessGrant[],
): AccessTreeRow[] {
    const itemsById = new Map(items.map(item => [item.id, item]));
    const childrenByParentId = new Map<string | null, PortalNavigationItem[]>();

    for (const item of items) {
        const siblings = childrenByParentId.get(item.parentId) ?? [];
        siblings.push(item);
        childrenByParentId.set(item.parentId, siblings);
    }

    for (const siblings of childrenByParentId.values()) {
        siblings.sort((a, b) => a.order - b.order);
    }

    const roots =
        grant.scopeType === 'PORTAL'
            ? (childrenByParentId.get(null) ?? []).filter(item => item.portalId === grant.scopeId)
            : items.filter(item => isAssetNavItem(item) && getNavItemAssetId(item) === grant.scopeId);

    const rows: AccessTreeRow[] = [];

    const walk = (item: PortalNavigationItem, depth: number, isRoot: boolean) => {
        // Nested assets are governed by their own grant, so stop descending there.
        if (!isRoot && isAssetNavItem(item)) {
            return;
        }

        rows.push({ item, depth, resolved: resolveNavItemAccess(item, itemsById, grants) });

        for (const child of childrenByParentId.get(item.id) ?? []) {
            walk(child, depth + 1, false);
        }
    };

    for (const root of roots) {
        if (grant.scopeType === 'PORTAL') {
            walk(root, 0, false);
        } else {
            for (const child of childrenByParentId.get(root.id) ?? []) {
                walk(child, 0, false);
            }
        }
    }

    return rows;
}
