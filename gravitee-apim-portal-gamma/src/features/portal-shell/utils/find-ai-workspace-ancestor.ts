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
import type { PortalNavigationAiWorkspace, PortalNavigationItem } from '../../portals/types';

export function findAiWorkspaceAncestor(
    navItems: readonly PortalNavigationItem[],
    itemId: string | null,
): PortalNavigationAiWorkspace | null {
    if (!itemId) {
        return null;
    }

    const itemMap = new Map(navItems.map(item => [item.id, item]));
    let current = itemMap.get(itemId);

    while (current) {
        if (current.type === 'AI_WORKSPACE') {
            return current;
        }
        current = current.parentId ? itemMap.get(current.parentId) : undefined;
    }

    return null;
}
