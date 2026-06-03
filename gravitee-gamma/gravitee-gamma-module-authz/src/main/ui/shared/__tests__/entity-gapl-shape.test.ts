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
import { buildGaplShape, inferAttrType, toGaplJson } from '../entity-gapl-shape';
import type { EntityInstance } from '../entity.types';

function entity(over: Partial<EntityInstance> = {}): EntityInstance {
    return {
        uid: { type: 'MCPServer', id: 'flight-status-mcp' },
        displayName: 'Flight Status MCP',
        attrs: { name: 'Flight Status MCP', url: 'https://x', transport: 'http' },
        parents: [],
        source: 'gravitee-catalog',
        ...over,
    };
}

describe('inferAttrType', () => {
    it('maps JS runtime types to display labels', () => {
        expect(inferAttrType('x')).toBe('String');
        expect(inferAttrType('')).toBe('String');
        expect(inferAttrType(3)).toBe('Integer');
        expect(inferAttrType(0)).toBe('Integer');
        expect(inferAttrType(-7)).toBe('Integer');
        expect(inferAttrType(3.5)).toBe('Decimal');
        expect(inferAttrType(true)).toBe('Boolean');
        expect(inferAttrType(false)).toBe('Boolean');
        expect(inferAttrType(['a', 'b'])).toBe('Set');
        expect(inferAttrType([])).toBe('Set');
    });
});

describe('buildGaplShape', () => {
    it('returns uid(type, canonical id) + attrs + canonical parent strings', () => {
        const shape = buildGaplShape(entity({ parents: [{ type: 'Group', id: 'developers' }] }));
        expect(shape).toEqual({
            uid: { type: 'MCPServer', id: 'mcp.flight-status-mcp' },
            attrs: { name: 'Flight Status MCP', url: 'https://x', transport: 'http' },
            parents: ['group.developers'],
        });
    });

    it('copies attrs (does not alias the entity attribute object)', () => {
        const e = entity();
        const shape = buildGaplShape(e);
        expect(shape.attrs).not.toBe(e.attrs);
        expect(shape.attrs).toEqual(e.attrs);
    });

    it('handles an entity with no attributes and no parents', () => {
        const shape = buildGaplShape(entity({ attrs: {}, parents: [] }));
        expect(shape.attrs).toEqual({});
        expect(shape.parents).toEqual([]);
    });

    it('serializes multiple parents in order', () => {
        const shape = buildGaplShape(
            entity({
                parents: [
                    { type: 'Group', id: 'a' },
                    { type: 'Group', id: 'b' },
                ],
            }),
        );
        expect(shape.parents).toEqual(['group.a', 'group.b']);
    });
});

describe('toGaplJson', () => {
    it('pretty-prints the canonical shape', () => {
        const json = toGaplJson(entity());
        expect(json).toContain('"type": "MCPServer"');
        expect(json).toContain('"id": "mcp.flight-status-mcp"');
        expect(json.startsWith('{')).toBe(true);
        expect(json).toContain('\n'); // pretty (2-space indent)
    });

    it('round-trips back to the same shape via JSON.parse', () => {
        const e = entity({ attrs: { count: 5, active: true }, parents: [{ type: 'Group', id: 'x' }] });
        expect(JSON.parse(toGaplJson(e))).toEqual(buildGaplShape(e));
    });
});
