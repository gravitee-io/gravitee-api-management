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
import { buildPolicyEntityRefs, deriveTargetEntityId, extractEntityRefsFromPolicyText, type PolicyRef } from '../policy-entity-refs';

function makePolicy(overrides: Partial<PolicyResponse> = {}): PolicyResponse {
    return {
        id: overrides.id ?? 'pol-1',
        environmentId: 'DEFAULT',
        name: overrides.name ?? 'Policy 1',
        description: null,
        policyText: overrides.policyText ?? '',
        type: overrides.type ?? 'API',
        target: overrides.target ?? null,
        status: overrides.status ?? 'DRAFT',
        createdAt: '2025-01-01T00:00:00.000Z',
        updatedAt: '2025-01-01T00:00:00.000Z',
        ...overrides,
    };
}

function makeEntity(type: string, id: string): EntityInstance {
    return {
        uid: { type, id },
        attrs: {},
        parents: [],
        source: 'local',
    };
}

describe('extractEntityRefsFromPolicyText', () => {
    it('returns empty array for empty input', () => {
        expect(extractEntityRefsFromPolicyText('')).toEqual([]);
    });

    it('extracts a single Type::"id" reference', () => {
        const refs = extractEntityRefsFromPolicyText('principal == User::"alice"');
        expect(refs).toEqual([{ type: 'User', id: 'alice', clause: 'principal' }]);
    });

    it('classifies clause as principal / action / resource', () => {
        const text = `permit (
              principal == User::"alice",
              action == action::"read",
              resource in [Endpoint::"flights", Endpoint::"bookings"]
            );`;
        const refs = extractEntityRefsFromPolicyText(text);
        const principalRefs = refs.filter(r => r.clause === 'principal');
        const actionRefs = refs.filter(r => r.clause === 'action');
        const resourceRefs = refs.filter(r => r.clause === 'resource');
        expect(principalRefs).toEqual([{ type: 'User', id: 'alice', clause: 'principal' }]);
        expect(actionRefs).toEqual([{ type: 'action', id: 'read', clause: 'action' }]);
        expect(resourceRefs).toEqual([
            { type: 'Endpoint', id: 'flights', clause: 'resource' },
            { type: 'Endpoint', id: 'bookings', clause: 'resource' },
        ]);
    });

    it('returns empty list for malformed / non-policy text without throwing', () => {
        const refs = extractEntityRefsFromPolicyText('this is not a valid policy {{{');
        expect(refs).toEqual([]);
    });
});

describe('buildPolicyEntityRefs', () => {
    it('returns an empty map when no entities or policies are given', () => {
        expect(buildPolicyEntityRefs([], [])).toEqual(new Map());
        expect(buildPolicyEntityRefs([makeEntity('User', 'alice')], [])).toEqual(new Map([['User::alice', []]]));
        expect(buildPolicyEntityRefs([], [makePolicy()])).toEqual(new Map());
    });

    it('returns no matches when policy text mentions other entities', () => {
        const entities = [makeEntity('User', 'alice')];
        const policies = [makePolicy({ policyText: 'permit ( principal == User::"bob" );' })];
        const map = buildPolicyEntityRefs(entities, policies);
        expect(map.get('User::alice')).toEqual([]);
    });

    it('finds a single matching policy and includes clause info', () => {
        const entities = [makeEntity('User', 'alice')];
        const policies = [
            makePolicy({
                id: 'p-read',
                name: 'Read access',
                policyText: 'permit ( principal == User::"alice", action == action::"read" );',
            }),
        ];
        const refs = buildPolicyEntityRefs(entities, policies).get('User::alice') as PolicyRef[];
        expect(refs).toHaveLength(1);
        expect(refs[0].policy.id).toBe('p-read');
        expect(refs[0].clauses).toEqual(['principal']);
    });

    it('finds multiple distinct policies that reference the same entity', () => {
        const entities = [makeEntity('User', 'alice')];
        const policies = [
            makePolicy({ id: 'p1', policyText: 'permit ( principal == User::"alice" );' }),
            makePolicy({ id: 'p2', policyText: 'forbid ( principal == User::"alice" );' }),
            makePolicy({ id: 'p3', policyText: 'permit ( principal == User::"bob" );' }),
        ];
        const refs = buildPolicyEntityRefs(entities, policies).get('User::alice') as PolicyRef[];
        expect(refs.map(r => r.policy.id).sort()).toEqual(['p1', 'p2']);
    });

    it('distinguishes principal vs action vs resource matches', () => {
        const entities = [makeEntity('User', 'alice'), makeEntity('Endpoint', 'flights')];
        const policies = [
            makePolicy({
                id: 'p1',
                policyText: `permit (
                    principal == User::"alice",
                    action == action::"read",
                    resource == Endpoint::"flights"
                );`,
            }),
        ];
        const map = buildPolicyEntityRefs(entities, policies);
        const aliceRefs = map.get('User::alice') as PolicyRef[];
        const flightsRefs = map.get('Endpoint::flights') as PolicyRef[];
        expect(aliceRefs).toHaveLength(1);
        expect(aliceRefs[0].clauses).toEqual(['principal']);
        expect(flightsRefs).toHaveLength(1);
        expect(flightsRefs[0].clauses).toEqual(['resource']);
    });

    it('records both clause kinds when an entity appears as principal AND resource in one policy', () => {
        const entities = [makeEntity('User', 'alice')];
        const policies = [
            makePolicy({
                id: 'p1',
                policyText: `permit (
                    principal == User::"alice",
                    action == action::"impersonate",
                    resource == User::"alice"
                );`,
            }),
        ];
        const refs = buildPolicyEntityRefs(entities, policies).get('User::alice') as PolicyRef[];
        expect(refs).toHaveLength(1);
        expect(refs[0].clauses.sort()).toEqual(['principal', 'resource']);
    });

    it('handles malformed policy text gracefully (no throw, no false positive)', () => {
        const entities = [makeEntity('User', 'alice')];
        const policies = [
            makePolicy({ id: 'p-bad', policyText: 'this is not a policy at all }}}' }),
            makePolicy({ id: 'p-good', policyText: 'permit ( principal == User::"alice" );' }),
        ];
        const map = buildPolicyEntityRefs(entities, policies);
        const refs = map.get('User::alice') as PolicyRef[];
        expect(refs).toHaveLength(1);
        expect(refs[0].policy.id).toBe('p-good');
    });

    it('treats list refs (resource in [A::"x", B::"y"]) the same as scalar refs', () => {
        const entities = [makeEntity('Endpoint', 'a'), makeEntity('Endpoint', 'b')];
        const policies = [
            makePolicy({
                id: 'p1',
                policyText: 'permit ( resource in [Endpoint::"a", Endpoint::"b"] );',
            }),
        ];
        const map = buildPolicyEntityRefs(entities, policies);
        expect((map.get('Endpoint::a') as PolicyRef[])[0].policy.id).toBe('p1');
        expect((map.get('Endpoint::b') as PolicyRef[])[0].policy.id).toBe('p1');
    });
});

describe('deriveTargetEntityId', () => {
    it('binds to an MCP server when the resource is the server itself', () => {
        expect(deriveTargetEntityId('permit ( principal, action, resource == MCP::"bookings" );')).toBe('mcp.bookings');
    });

    it('reduces an MCP tool resource to its owning server', () => {
        expect(deriveTargetEntityId('permit ( principal, action, resource == MCP::"bookings.cancel-booking" );')).toBe('mcp.bookings');
    });

    it('binds LLM and API resources to their service entity', () => {
        // `LLM` is an alias of the `model` kind — the derived id is canonicalised.
        expect(deriveTargetEntityId('permit ( principal, action, resource == LLM::"gpt-4o" );')).toBe('model.gpt-4o');
        expect(deriveTargetEntityId('forbid ( principal, action, resource == API::"orders.items" );')).toBe('api.orders');
    });

    it('canonicalises UI-type and alias tokens to the canonical kind', () => {
        // The visual builder emits the UI type (`MCPServer`); code authors may use
        // the kind or an alias. All must reduce to the same canonical entityId so
        // the target matches the resource entity (`mcp.bookings`), not `mcpserver.*`.
        expect(deriveTargetEntityId('permit ( principal, action, resource == MCPServer::"bookings" );')).toBe('mcp.bookings');
        expect(deriveTargetEntityId('permit ( principal, action, resource == mcpserver::"bookings" );')).toBe('mcp.bookings');
        expect(deriveTargetEntityId('permit ( principal, action, resource == Model::"gpt-4o" );')).toBe('model.gpt-4o');
        expect(deriveTargetEntityId('permit ( principal, action, resource == AgentIdentity::"svc" );')).toBe('agent-identity.svc');
    });

    it('returns null for generic/custom resources (stays GLOBAL → Custom)', () => {
        expect(deriveTargetEntityId('permit ( principal, action, resource == Resource::"thing" );')).toBeNull();
        expect(deriveTargetEntityId('permit ( principal, action, resource );')).toBeNull();
    });

    it('ignores principal/action clauses and only binds on a resource token', () => {
        // An MCP token in the principal clause must not be mistaken for the target.
        expect(deriveTargetEntityId('permit ( principal == MCP::"bookings", action == Action::"read", resource );')).toBeNull();
    });

    it('takes the first service-typed resource when several are present', () => {
        const text = 'permit ( principal, action, resource in [MCP::"bookings", MCP::"customers"] );';
        expect(deriveTargetEntityId(text)).toBe('mcp.bookings');
    });

    it('returns null for empty, nullish, or malformed text', () => {
        expect(deriveTargetEntityId('')).toBeNull();
        expect(deriveTargetEntityId(null)).toBeNull();
        expect(deriveTargetEntityId(undefined)).toBeNull();
        expect(deriveTargetEntityId('garbage {{{ no tokens')).toBeNull();
    });
});
