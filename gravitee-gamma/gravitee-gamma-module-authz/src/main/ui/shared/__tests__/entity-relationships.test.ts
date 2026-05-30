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
import type { PolicyResponse } from '../api/authz-api.types';
import type { EntityInstance } from '../entity.types';
import { childrenByType, policiesFor, referencedBy } from '../entity-relationships';

const mcp = (id: string, parents: { type: string; id: string }[] = []): EntityInstance => ({
    uid: { type: 'MCPServer', id },
    attrs: {},
    parents,
    source: 'gravitee-catalog',
});
const tool = (id: string, parentId: string): EntityInstance => ({
    uid: { type: 'MCPTool', id },
    attrs: {},
    parents: [{ type: 'MCPServer', id: parentId }],
    source: 'gravitee-catalog',
});
const resource = (id: string, parentId: string): EntityInstance => ({
    uid: { type: 'MCPResource', id },
    attrs: {},
    parents: [{ type: 'MCPServer', id: parentId }],
    source: 'gravitee-catalog',
});

const server = mcp('flight-status-mcp');
const all = [
    server,
    tool('get-flight', 'flight-status-mcp'),
    tool('search', 'flight-status-mcp'),
    resource('arrivals', 'flight-status-mcp'),
    mcp('payments-mcp'),
];

function policy(over: Partial<PolicyResponse>): PolicyResponse {
    return { id: 'x', name: 'p', kind: 'RESOURCE', entityId: 'mcp.flight-status-mcp', policyText: '', status: 'DRAFT', ...over };
}

describe('referencedBy', () => {
    it('finds entities that list this uid as a parent', () => {
        expect(referencedBy(server, all).map(e => e.uid.id)).toEqual(['get-flight', 'search', 'arrivals']);
    });

    it('returns empty when nothing references the entity', () => {
        expect(referencedBy(mcp('payments-mcp'), all)).toEqual([]);
    });

    it('matches on the canonical dotted uid, not the bare id', () => {
        // A child whose parent id collides bare-wise but differs by kind must NOT match.
        const userParent: EntityInstance = {
            uid: { type: 'User', id: 'child' },
            attrs: {},
            parents: [{ type: 'User', id: 'flight-status-mcp' }],
            source: 'local',
        };
        expect(referencedBy(server, [server, userParent])).toEqual([]);
    });
});

describe('childrenByType', () => {
    it('groups reverse children by type with counts, sorted by type', () => {
        expect(childrenByType(server, all)).toEqual([
            { type: 'MCPResource', count: 1 },
            { type: 'MCPTool', count: 2 },
        ]);
    });

    it('returns empty for a leaf entity', () => {
        expect(childrenByType(mcp('payments-mcp'), all)).toEqual([]);
    });
});

describe('policiesFor', () => {
    it('matches policies whose entityId equals the canonical uid', () => {
        const policies = [
            policy({ id: '1', name: 'p1' }),
            policy({ id: '2', name: 'global', kind: 'GLOBAL', entityId: null }),
            policy({ id: '3', name: 'other', entityId: 'mcp.payments-mcp' }),
        ];
        expect(policiesFor(server, policies).map(p => p.name)).toEqual(['p1']);
    });

    it('excludes GLOBAL (entityId null) policies', () => {
        expect(policiesFor(server, [policy({ kind: 'GLOBAL', entityId: null })])).toEqual([]);
    });

    it('returns empty when no policy targets the entity', () => {
        expect(policiesFor(server, [])).toEqual([]);
    });
});
