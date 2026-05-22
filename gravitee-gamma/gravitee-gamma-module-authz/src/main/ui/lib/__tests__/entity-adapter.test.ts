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
import type { EntityResponse } from '../api/authz-api.types';
import { parseEntityUid, formatEntityUid, fromBackend, toBackend, META_KEYS } from '../entity-adapter';

// ---------- parseEntityUid ---------------------------------------------------

describe('parseEntityUid', () => {
    // Canonical dotted form — what the backend EntityIdValidator accepts.
    it('parses dotted user form', () => {
        expect(parseEntityUid('user.alice')).toEqual({ type: 'User', id: 'alice' });
    });

    it('parses dotted group form', () => {
        expect(parseEntityUid('group.engineering')).toEqual({ type: 'Group', id: 'engineering' });
    });

    it('parses SCIM 3-segment dotted form (kind.connector.slug)', () => {
        // For SCIM mirrors the id keeps the full tail so a from→to round-trip
        // reproduces the canonical entityId verbatim.
        expect(parseEntityUid('user.okta.alice')).toEqual({ type: 'User', id: 'okta.alice' });
    });

    it('maps the mcp short kind back to MCPServer', () => {
        expect(parseEntityUid('mcp.flight-mcp')).toEqual({ type: 'MCPServer', id: 'flight-mcp' });
    });

    // Legacy Type::"id" form — tolerated for backwards compat with test fixtures
    // and any pre-existing data; the backend would have rejected this on save.
    it('parses legacy unquoted form', () => {
        expect(parseEntityUid('User::alice')).toEqual({ type: 'User', id: 'alice' });
    });

    it('parses legacy quoted form', () => {
        expect(parseEntityUid('User::"alice"')).toEqual({ type: 'User', id: 'alice' });
    });

    it('parses legacy quoted id with escaped inner quote', () => {
        expect(parseEntityUid('User::"alice\\"bob"')).toEqual({ type: 'User', id: 'alice"bob' });
    });

    it('returns Unknown type when neither dotted nor legacy form matches', () => {
        const result = parseEntityUid('justsomething');
        expect(result.type).toBe('Unknown');
        expect(result.id).toBe('justsomething');
    });

    it('returns Unknown type when first dotted segment is not a recognised kind', () => {
        const result = parseEntityUid('badprefix.something');
        expect(result.type).toBe('Unknown');
        expect(result.id).toBe('badprefix.something');
    });
});

// ---------- formatEntityUid --------------------------------------------------

describe('formatEntityUid', () => {
    it('formats to canonical dotted form', () => {
        expect(formatEntityUid({ type: 'User', id: 'alice' })).toBe('user.alice');
    });

    it('collapses MCPServer to the mcp prefix the backend expects', () => {
        expect(formatEntityUid({ type: 'MCPServer', id: 'flight-mcp' })).toBe('mcp.flight-mcp');
    });

    it('keeps multi-segment ids intact (SCIM mirror layout)', () => {
        // Round-tripping a SCIM-sourced uid must reproduce the canonical 3-seg form.
        expect(formatEntityUid({ type: 'User', id: 'okta.alice' })).toBe('user.okta.alice');
    });

    it('round-trips a 2-segment local uid through parse', () => {
        const uid = { type: 'Group', id: 'engineering' };
        const formatted = formatEntityUid(uid);
        const parsed = parseEntityUid(formatted);
        expect(parsed).toEqual(uid);
    });

    it('round-trips a SCIM 3-segment uid through parse', () => {
        const uid = { type: 'User', id: 'okta.alice' };
        const formatted = formatEntityUid(uid);
        const parsed = parseEntityUid(formatted);
        expect(parsed).toEqual(uid);
    });
});

// ---------- Meta key separation in fromBackend --------------------------------

describe('fromBackend', () => {
    const base: EntityResponse = {
        id: 'backend-id-1',
        environmentId: 'DEFAULT',
        uid: 'user.alice',
        attributes: { _kind: 'user' },
        parents: [],
        createdAt: '2025-01-01T00:00:00.000Z',
        updatedAt: '2025-01-02T00:00:00.000Z',
    };

    it('parses dotted uid with _kind override and strips the kind prefix', () => {
        const inst = fromBackend(base);
        expect(inst.uid).toEqual({ type: 'User', id: 'alice' });
    });

    it('parses SCIM 3-segment uid, keeping connector.slug as the id', () => {
        // Regression for #8 — a SCIM-sourced principal must round-trip without
        // re-wrapping its dotted entityId. The id keeps the connector segment.
        const inst = fromBackend({ ...base, uid: 'user.okta.alice', attributes: { _kind: 'user', _connector: 'okta' } });
        expect(inst.uid).toEqual({ type: 'User', id: 'okta.alice' });
    });

    it('falls back to dotted-prefix inference when _kind is absent', () => {
        const inst = fromBackend({ ...base, uid: 'group.eng', attributes: {} });
        expect(inst.uid).toEqual({ type: 'Group', id: 'eng' });
    });

    it('defaults source to local when not in attributes', () => {
        const inst = fromBackend(base);
        expect(inst.source).toBe('local');
    });

    it('extracts _source meta key', () => {
        const inst = fromBackend({ ...base, attributes: { _kind: 'user', _source: 'scim' } });
        expect(inst.source).toBe('scim');
        expect(inst.attrs['_source']).toBeUndefined();
    });

    it('extracts _displayName meta key', () => {
        const inst = fromBackend({ ...base, attributes: { _kind: 'user', _displayName: 'Alice Admin', name: 'alice' } });
        expect(inst.displayName).toBe('Alice Admin');
        expect(inst.attrs['_displayName']).toBeUndefined();
        expect(inst.attrs['name']).toBe('alice');
    });

    it('does not leak any META_KEY into attrs', () => {
        const allMeta: Record<string, unknown> = {};
        for (const k of META_KEYS) allMeta[k] = 'test';
        const inst = fromBackend({ ...base, attributes: { ...allMeta, regular: 'value' } });
        for (const k of META_KEYS) {
            expect(inst.attrs[k]).toBeUndefined();
        }
        expect(inst.attrs['regular']).toBe('value');
    });

    it('parses dotted parents', () => {
        const inst = fromBackend({ ...base, parents: ['group.engineering', 'group.ops'] });
        expect(inst.parents).toEqual([
            { type: 'Group', id: 'engineering' },
            { type: 'Group', id: 'ops' },
        ]);
    });

    it('parses legacy quoted parents (backwards-compat)', () => {
        const inst = fromBackend({ ...base, parents: ['Group::"engineering"', 'Group::ops'] });
        expect(inst.parents).toEqual([
            { type: 'Group', id: 'engineering' },
            { type: 'Group', id: 'ops' },
        ]);
    });

    it('preserves backend id', () => {
        const inst = fromBackend(base);
        expect(inst._backendId).toBe('backend-id-1');
    });
});

// ---------- toBackend --------------------------------------------------------

describe('toBackend', () => {
    it('formats uid in canonical dotted form', () => {
        const req = toBackend({
            uid: { type: 'User', id: 'alice' },
            attrs: {},
            parents: [],
            source: 'local',
        });
        expect(req.uid).toBe('user.alice');
    });

    it('does NOT write _source when source is local', () => {
        const req = toBackend({
            uid: { type: 'User', id: 'alice' },
            attrs: {},
            parents: [],
            source: 'local',
        });
        expect(req.attributes['_source']).toBeUndefined();
    });

    it('writes _source when source is not local', () => {
        const req = toBackend({
            uid: { type: 'User', id: 'alice' },
            attrs: {},
            parents: [],
            source: 'scim',
        });
        expect(req.attributes['_source']).toBe('scim');
    });

    it('writes _displayName when set', () => {
        const req = toBackend({
            uid: { type: 'User', id: 'alice' },
            displayName: 'Alice Admin',
            attrs: {},
            parents: [],
            source: 'local',
        });
        expect(req.attributes['_displayName']).toBe('Alice Admin');
    });

    it('formats parents in canonical dotted form', () => {
        const req = toBackend({
            uid: { type: 'User', id: 'alice' },
            attrs: {},
            parents: [{ type: 'Group', id: 'engineering' }],
            source: 'local',
        });
        expect(req.parents).toEqual(['group.engineering']);
    });

    it('round-trips local user from backend → ui → backend without corruption', () => {
        const response: EntityResponse = {
            id: 'x',
            environmentId: 'DEFAULT',
            uid: 'group.engineering',
            attributes: { _kind: 'group', name: 'Engineering', _displayName: 'Engineering Team' },
            parents: [],
            createdAt: '',
            updatedAt: '',
        };
        const inst = fromBackend(response);
        const req = toBackend(inst);
        expect(req.uid).toBe('group.engineering');
        expect(req.attributes['name']).toBe('Engineering');
        expect(req.attributes['_displayName']).toBe('Engineering Team');
    });

    it('round-trips a SCIM 3-segment principal without re-wrapping', () => {
        // Regression for #8 — the bug was that fromBackend kept `id = "user.okta.alice"`
        // and toBackend then produced `"User::"user.okta.alice""` (legacy wrap of the
        // dotted form), corrupting the canonical entityId on every UI write.
        const response: EntityResponse = {
            id: 'x',
            environmentId: 'DEFAULT',
            uid: 'user.okta.alice',
            attributes: { _kind: 'user', _connector: 'okta', _source: 'scim' },
            parents: ['group.okta.engineering'],
            createdAt: '',
            updatedAt: '',
        };
        const inst = fromBackend(response);
        const req = toBackend(inst);
        expect(req.uid).toBe('user.okta.alice');
        expect(req.parents).toEqual(['group.okta.engineering']);
        expect(req.attributes['_source']).toBe('scim');
    });
});
