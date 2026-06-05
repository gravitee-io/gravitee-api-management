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
import type { EntityResponse } from '../../shared/api/authz-api.types';
import type { ChipOption } from '../../shared/chip-option';
import type { ParsedAction } from '../../shared/engine-schema';

function firstString(...values: unknown[]): string | undefined {
    for (const v of values) {
        if (typeof v === 'string' && v) return v;
    }
    return undefined;
}

export function buildActionOptions(
    schemaActions: readonly ParsedAction[],
    actionRows: readonly EntityResponse[],
    mcpToolRows: readonly EntityResponse[],
): readonly ChipOption[] {
    const seen = new Set<string>();
    const actions: ChipOption[] = [];

    for (const a of schemaActions) {
        const id = `Action::"${a.name}"`;
        if (seen.has(id)) continue;
        seen.add(id);
        actions.push({ id, label: a.name, group: 'Action' });
    }

    for (const e of actionRows) {
        const name = firstString(e.attributes?.name) ?? e.uid.replace(/^action\./, '');
        if (!name) continue;
        const id = `Action::"${name}"`;
        if (seen.has(id)) continue;
        seen.add(id);
        const description = firstString(e.attributes?.description);
        actions.push({ id, label: name, group: 'Action', description });
    }

    const tools: ChipOption[] = [];
    for (const e of mcpToolRows) {
        const attrs = e.attributes ?? {};
        const name = firstString(attrs._displayName, attrs.displayName, attrs.name) ?? e.uid.replace(/^mcptool\./, '');
        if (!name) continue;
        const id = `Action::"${name}"`;
        if (seen.has(id)) continue;
        seen.add(id);
        const description = firstString(attrs.description);
        tools.push({ id, label: name, group: 'MCP Tools', description });
    }
    tools.sort((a, b) => a.label.localeCompare(b.label));

    return [...actions, ...tools];
}
