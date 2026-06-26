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
import type { PortalNavigationItem } from '../../portals/types';

export function isDescendantOf(
    navItems: readonly PortalNavigationItem[],
    itemId: string,
    ancestorId: string,
): boolean {
    let current = navItems.find(item => item.id === itemId);
    while (current?.parentId) {
        if (current.parentId === ancestorId) {
            return true;
        }
        current = navItems.find(item => item.id === current?.parentId);
    }
    return false;
}

export function shouldExpandNode(
    navItems: readonly PortalNavigationItem[],
    itemId: string,
    selectedNavItemId: string | null,
): boolean {
    if (!selectedNavItemId) {
        return true;
    }
    if (itemId === selectedNavItemId) {
        return true;
    }
    return isDescendantOf(navItems, selectedNavItemId, itemId);
}
