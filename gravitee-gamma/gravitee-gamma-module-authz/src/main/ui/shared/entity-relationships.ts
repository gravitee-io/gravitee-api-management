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
import type { PolicyResponse } from './api/authz-api.types';
import { formatEntityUid } from './entity-adapter';
import type { EntityInstance } from './entity.types';

/** Entities that list this entity's uid among their parents. */
export function referencedBy(entity: EntityInstance, all: readonly EntityInstance[]): EntityInstance[] {
    const uid = formatEntityUid(entity.uid);
    return all.filter(e => e.parents.some(p => formatEntityUid(p) === uid));
}

/** Reverse children grouped by entity type, sorted by type for stable rendering. */
export function childrenByType(entity: EntityInstance, all: readonly EntityInstance[]): { type: string; count: number }[] {
    const counts = new Map<string, number>();
    for (const child of referencedBy(entity, all)) {
        counts.set(child.uid.type, (counts.get(child.uid.type) ?? 0) + 1);
    }
    return Array.from(counts, ([type, count]) => ({ type, count })).sort((a, b) => a.type.localeCompare(b.type));
}

/** Policies whose target entityId equals this entity's canonical uid. */
export function policiesFor(entity: EntityInstance, policies: readonly PolicyResponse[]): PolicyResponse[] {
    const uid = formatEntityUid(entity.uid);
    return policies.filter(p => p.target?.id === uid);
}
