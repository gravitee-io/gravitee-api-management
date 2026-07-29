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
import type { AccessTreeRow } from './resolve-nav-item-access';

export interface PortalAccessTreeGroup {
    readonly rootId: string;
    readonly portalId: string;
    readonly rows: readonly AccessTreeRow[];
}

/**
 * Groups flattened grant-scope rows by the asset embedding that produced them
 * (first-seen root order). Rows from the same portal but different embeddings stay
 * in separate groups so duplicates do not collapse into one flat list.
 */
export function groupAccessTreeRowsByPortal(rows: readonly AccessTreeRow[]): PortalAccessTreeGroup[] {
    const groups: PortalAccessTreeGroup[] = [];
    const indexByRootId = new Map<string, number>();

    for (const row of rows) {
        const existingIndex = indexByRootId.get(row.rootId);

        if (existingIndex === undefined) {
            indexByRootId.set(row.rootId, groups.length);
            groups.push({ rootId: row.rootId, portalId: row.item.portalId, rows: [row] });
            continue;
        }

        const group = groups[existingIndex];
        groups[existingIndex] = {
            rootId: group.rootId,
            portalId: group.portalId,
            rows: [...group.rows, row],
        };
    }

    return groups;
}

/** True when the subtree should render section headers (more than one embedding). */
export function shouldShowPortalGroupHeaders(groups: readonly PortalAccessTreeGroup[]): boolean {
    return groups.length > 1;
}
