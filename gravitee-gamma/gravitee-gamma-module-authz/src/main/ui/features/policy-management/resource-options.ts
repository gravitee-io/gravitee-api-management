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
import type { EntityResponse, PolicyType } from '../../shared/api/authz-api.types';
import type { ChipOption } from '../../shared/chip-option';

export interface ResourceOptionsConfig {
    readonly type: PolicyType;
    readonly hasTarget?: boolean;
}

function firstString(...values: unknown[]): string | undefined {
    for (const v of values) {
        if (typeof v === 'string' && v) return v;
    }
    return undefined;
}

function uidSlug(uid: string): string {
    return uid.includes('.') ? uid.slice(uid.indexOf('.') + 1) : uid;
}

function groupForSegment(seg: string): string {
    switch (seg) {
        case 'mcp':
            return 'MCP';
        case 'model':
        case 'llm':
            return 'Model';
        case 'agent':
            return 'Agent';
        case 'api':
            return 'API';
        default:
            return 'Resource';
    }
}

export function buildResourceOptions(
    config: ResourceOptionsConfig,
    catalogRows: readonly EntityResponse[],
    mcpToolRows: readonly EntityResponse[],
): readonly ChipOption[] {
    const items: ChipOption[] = [];
    const seen = new Set<string>();
    const typePrefix = config.type.toLowerCase();

    for (const e of catalogRows) {
        const attrs = e.attributes ?? {};
        const firstSeg = e.uid.includes('.') ? e.uid.slice(0, e.uid.indexOf('.')).toLowerCase() : '';
        if (config.hasTarget && firstSeg !== typePrefix) continue;
        const slug = uidSlug(e.uid);
        const group = groupForSegment(firstSeg);
        const id = `${group}::"${slug}"`;
        if (seen.has(id)) continue;
        seen.add(id);
        items.push({
            id,
            label: firstString(attrs.displayName, attrs.name) ?? slug,
            group,
            description: firstString(attrs.description),
        });
    }

    for (const e of mcpToolRows) {
        const attrs = e.attributes ?? {};
        const slug = uidSlug(e.uid);
        const id = `MCPTool::"${slug}"`;
        if (seen.has(id)) continue;
        seen.add(id);
        items.push({
            id,
            label: firstString(attrs._displayName, attrs.displayName, attrs.name) ?? slug,
            group: 'MCP Tools',
            description: firstString(attrs.description),
        });
    }

    items.sort((a, b) => (a.group === b.group ? a.label.localeCompare(b.label) : a.group.localeCompare(b.group)));
    return items;
}
