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

import type { OrgGroup, OrgTag, ShardingTagRow } from '../types/entrypoint';

export function toShardingTagRows(tags: OrgTag[], groups: OrgGroup[]): ShardingTagRow[] {
    const groupNameById = new Map(groups.map(group => [group.id, group.name || group.id]));

    return tags.map(tag => {
        const restrictedGroupIds = tag.restricted_groups ?? [];
        return {
            id: tag.id,
            key: tag.key,
            name: tag.name ?? '',
            description: tag.description ?? '',
            restrictedGroupIds,
            restrictedGroupNames: restrictedGroupIds.map(id => groupNameById.get(id) ?? id),
        };
    });
}

export function filterShardingTags(rows: ShardingTagRow[], query: string): ShardingTagRow[] {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return rows;
    return rows.filter(
        row =>
            row.key.toLowerCase().includes(normalized) ||
            row.name.toLowerCase().includes(normalized) ||
            row.description.toLowerCase().includes(normalized),
    );
}
