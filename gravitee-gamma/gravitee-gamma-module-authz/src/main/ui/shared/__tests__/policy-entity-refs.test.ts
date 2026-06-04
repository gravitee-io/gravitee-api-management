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
import { deriveTargetEntityId, extractEntityRefsFromPolicyText } from '../policy-entity-refs';

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
