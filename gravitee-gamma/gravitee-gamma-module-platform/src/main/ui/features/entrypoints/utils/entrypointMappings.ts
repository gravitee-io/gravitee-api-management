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

import type { Entrypoint, EntrypointMappingRow, EntrypointTarget, OrgEnvironment, OrgTag } from '../types/entrypoint';

const TARGET_LABELS: Record<EntrypointTarget, string> = {
    HTTP: 'HTTP',
    TCP: 'TCP',
    KAFKA: 'Kafka',
};

export function entrypointTargetLabel(target: EntrypointTarget | undefined): string {
    if (!target) return '—';
    return TARGET_LABELS[target] ?? target;
}

export function toEntrypointMappingRows(entrypoints: Entrypoint[], environments: OrgEnvironment[], tags: OrgTag[]): EntrypointMappingRow[] {
    const envNameById = new Map(environments.map(env => [env.id, env.name || env.id]));
    const tagNameByKey = new Map(tags.map(tag => [tag.key, tag.name || tag.key]));

    return entrypoints.map(entrypoint => {
        const tagKeys = (entrypoint.tags ?? []).map(tag => tag.trim()).filter(Boolean);
        const environmentIds = entrypoint.environmentIds ?? [];
        const target = entrypoint.target ?? 'HTTP';
        return {
            id: entrypoint.id,
            value: entrypoint.value ?? '',
            target,
            targetLabel: entrypointTargetLabel(target),
            tags: tagKeys,
            tagsName: tagKeys.map(key => {
                const name = (tagNameByKey.get(key) ?? key).trim();
                return name || key;
            }),
            environmentIds,
            environmentNames: environmentIds.map(id => envNameById.get(id) ?? id),
        };
    });
}
