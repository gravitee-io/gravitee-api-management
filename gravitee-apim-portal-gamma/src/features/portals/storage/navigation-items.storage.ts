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
import { NAVIGATION_ITEMS_STORE_NAME, runTransaction } from './db';

export const STORE_NAME = NAVIGATION_ITEMS_STORE_NAME;

export async function getNavItems(portalId: string): Promise<PortalNavigationItem[]> {
    const items = await runTransaction<PortalNavigationItem[]>(NAVIGATION_ITEMS_STORE_NAME, 'readonly', store =>
        store.index('portalId').getAll(portalId),
    );
    return items.sort((a, b) => a.order - b.order);
}

export async function getNavItem(id: string): Promise<PortalNavigationItem | undefined> {
    return runTransaction(NAVIGATION_ITEMS_STORE_NAME, 'readonly', store => store.get(id));
}

export async function getNavItemBySlug(portalId: string, slug: string): Promise<PortalNavigationItem | undefined> {
    return runTransaction(NAVIGATION_ITEMS_STORE_NAME, 'readonly', store =>
        store.index('portalId_slug').get([portalId, slug]),
    );
}

export async function saveNavItem(item: PortalNavigationItem): Promise<void> {
    await runTransaction(NAVIGATION_ITEMS_STORE_NAME, 'readwrite', store => store.put(item));
}

export async function deleteNavItem(id: string): Promise<void> {
    await runTransaction(NAVIGATION_ITEMS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deleteNavItemsForPortal(portalId: string): Promise<void> {
    const items = await getNavItems(portalId);
    await Promise.all(items.map(item => deleteNavItem(item.id)));
}

export async function reorderNavItems(portalId: string, orderedIds: readonly string[]): Promise<void> {
    const items = await getNavItems(portalId);
    const itemById = new Map(items.map(item => [item.id, item]));

    await Promise.all(
        orderedIds.map((id, order) => {
            const item = itemById.get(id);
            if (!item) {
                return Promise.resolve();
            }
            return saveNavItem({ ...item, order });
        }),
    );
}
