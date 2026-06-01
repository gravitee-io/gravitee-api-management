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
import { buildResourceOptions } from '../resource-options';

function entity(uid: string, attributes: Record<string, unknown> = {}): EntityResponse {
    return { uid, attributes } as EntityResponse;
}

describe('buildResourceOptions', () => {
    it('maps mcp.* catalog rows to the MCP group with a canonical resource id', () => {
        const result = buildResourceOptions({ type: 'MCP', hasTarget: true }, [entity('mcp.weather', { displayName: 'Weather MCP' })], []);

        expect(result).toEqual([{ id: 'MCP::"weather"', label: 'Weather MCP', group: 'MCP', description: undefined }]);
    });

    it('adds mcptool.* rows as a separate "MCP Tools" group with MCPTool resource ids', () => {
        const tools = [
            entity('mcptool.get_forecast', { _displayName: 'get_forecast', description: 'Forecast' }),
            entity('mcptool.get_current_weather', { _displayName: 'get_current_weather' }),
        ];
        const result = buildResourceOptions({ type: 'MCP', hasTarget: true }, [], tools);

        expect(result.map(o => ({ id: o.id, label: o.label, group: o.group }))).toEqual([
            { id: 'MCPTool::"get_current_weather"', label: 'get_current_weather', group: 'MCP Tools' },
            { id: 'MCPTool::"get_forecast"', label: 'get_forecast', group: 'MCP Tools' },
        ]);
        expect(result.find(o => o.label === 'get_forecast')?.description).toBe('Forecast');
    });

    it('orders the MCP server group before the MCP Tools group', () => {
        const result = buildResourceOptions(
            { type: 'MCP', hasTarget: true },
            [entity('mcp.weather', { displayName: 'Weather MCP' })],
            [entity('mcptool.get_forecast', { _displayName: 'get_forecast' })],
        );

        expect(result.map(o => o.group)).toEqual(['MCP', 'MCP Tools']);
    });

    it('falls back to the uid slug when a tool has no display name', () => {
        const result = buildResourceOptions({ type: 'MCP', hasTarget: true }, [], [entity('mcptool.search_locations')]);

        expect(result[0]).toEqual({
            id: 'MCPTool::"search_locations"',
            label: 'search_locations',
            group: 'MCP Tools',
            description: undefined,
        });
    });

    it('filters catalog rows whose prefix does not match the target service type', () => {
        const result = buildResourceOptions(
            { type: 'MCP', hasTarget: true },
            [entity('mcp.weather', { displayName: 'Weather MCP' }), entity('model.gpt', { displayName: 'GPT' })],
            [],
        );

        expect(result.map(o => o.id)).toEqual(['MCP::"weather"']);
    });
});
