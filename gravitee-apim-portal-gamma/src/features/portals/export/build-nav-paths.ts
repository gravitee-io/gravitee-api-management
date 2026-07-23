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
import type { PortalNavigationItem } from '../types';

export function buildNavPaths(navItems: readonly PortalNavigationItem[]): ReadonlyMap<string, string> {
    const itemsByParent = new Map<string | null, PortalNavigationItem[]>();

    for (const item of navItems) {
        const siblings = itemsByParent.get(item.parentId) ?? [];
        siblings.push(item);
        itemsByParent.set(item.parentId, siblings);
    }

    for (const siblings of itemsByParent.values()) {
        siblings.sort((a, b) => a.order - b.order);
    }

    const paths = new Map<string, string>();

    function walk(parentId: string | null, parentPath: string): void {
        const children = itemsByParent.get(parentId) ?? [];
        for (const item of children) {
            const path = parentPath ? `${parentPath}/${item.slug}` : `/${item.slug}`;
            paths.set(item.id, path);
            if (
                item.type === 'FOLDER'
                || item.type === 'API'
                || item.type === 'API_PRODUCT'
                || item.type === 'AI_WORKSPACE'
            ) {
                walk(item.id, path);
            }
        }
    }

    walk(null, '');
    return paths;
}

export function getParentPath(
    item: PortalNavigationItem,
    paths: ReadonlyMap<string, string>,
): string {
    if (item.parentId === null) {
        return '/';
    }
    return paths.get(item.parentId) ?? '/';
}
