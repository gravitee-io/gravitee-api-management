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
import { describe, expect, it } from 'vitest';
import type { EntityResponse } from '../../../shared/api/authz-api.types';
import { buildActionOptions } from '../action-options';

function entity(uid: string, attributes: Record<string, unknown> = {}): EntityResponse {
    return { uid, attributes } as EntityResponse;
}

describe('buildActionOptions', () => {
    it('merges schema actions and action.* entities into the Action group', () => {
        const schema = 'action "read" appliesTo {}; action "write" appliesTo {};';
        const result = buildActionOptions(schema, [entity('action.delete', { name: 'delete' })], []);

        expect(result.map(o => o.label)).toEqual(['read', 'write', 'delete']);
        expect(result.every(o => o.group === 'Action')).toBe(true);
        expect(result.find(o => o.label === 'read')?.id).toBe('Action::"read"');
    });

    it('adds mcptool.* entities as a separate "MCP Tools" group, sorted by label', () => {
        const tools = [
            entity('mcptool.get_forecast', { _displayName: 'get_forecast', description: 'Forecast' }),
            entity('mcptool.get_current_weather', { _displayName: 'get_current_weather' }),
        ];
        const result = buildActionOptions(undefined, [], tools);

        expect(result.map(o => ({ label: o.label, group: o.group, id: o.id }))).toEqual([
            { label: 'get_current_weather', group: 'MCP Tools', id: 'Action::"get_current_weather"' },
            { label: 'get_forecast', group: 'MCP Tools', id: 'Action::"get_forecast"' },
        ]);
        expect(result[1].description).toBe('Forecast');
    });

    it('keeps actions first and tools after, as distinct groups', () => {
        const result = buildActionOptions(
            'action "read" appliesTo {};',
            [],
            [entity('mcptool.get_forecast', { _displayName: 'get_forecast' })],
        );

        expect(result.map(o => o.group)).toEqual(['Action', 'MCP Tools']);
    });

    it('falls back to the uid slug when a tool has no display name', () => {
        const result = buildActionOptions(undefined, [], [entity('mcptool.search_locations')]);

        expect(result[0].label).toBe('search_locations');
        expect(result[0].id).toBe('Action::"search_locations"');
    });

    it('deduplicates by action id across schema, action.* and tools (first wins)', () => {
        const result = buildActionOptions(
            'action "read" appliesTo {};',
            [entity('action.read', { name: 'read' })],
            [entity('mcptool.read', { _displayName: 'read' })],
        );

        expect(result).toHaveLength(1);
        expect(result[0].group).toBe('Action');
    });
});
