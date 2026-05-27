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

describe('parseEntityUid', () => {
    // Canonical dotted form — what the backend EntityIdValidator accepts.
    it('parses dotted user form', () => {
        expect(parseEntityUid('user.alice')).toEqual({ type: 'User', id: 'alice' });
    });

    it('parses dotted group form', () => {
        expect(parseEntityUid('group.engineering')).toEqual({ type: 'Group', id: 'engineering' });
    });

    it('parses a multi-segment dotted id, keeping the full tail as the id', () => {
        expect(parseEntityUid('mcp.flight-api.search')).toEqual({ type: 'MCPServer', id: 'flight-api.search' });
    });

    it('maps the mcp short kind back to MCPServer', () => {
        expect(parseEntityUid('mcp.flight-mcp')).toEqual({ type: 'MCPServer', id: 'flight-mcp' });
    });

    it('returns Unknown type for a non-dotted string', () => {
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

describe('formatEntityUid', () => {
    it('formats to canonical dotted form', () => {
        expect(formatEntityUid({ type: 'User', id: 'alice' })).toBe('user.alice');
    });

    it('collapses MCPServer to the mcp prefix the backend expects', () => {
        expect(formatEntityUid({ type: 'MCPServer', id: 'flight-mcp' })).toBe('mcp.flight-mcp');
    });

    it('keeps multi-segment ids intact', () => {
        expect(formatEntityUid({ type: 'MCPServer', id: 'flight-api.search' })).toBe('mcp.flight-api.search');
    });

    it('round-trips a 2-segment local uid through parse', () => {
        const uid = { type: 'Group', id: 'engineering' };
        const formatted = formatEntityUid(uid);
        const parsed = parseEntityUid(formatted);
        expect(parsed).toEqual(uid);
    });

    it('round-trips a multi-segment uid through parse', () => {
        const uid = { type: 'MCPServer', id: 'flight-api.search' };
        const formatted = formatEntityUid(uid);
        const parsed = parseEntityUid(formatted);
        expect(parsed).toEqual(uid);
    });
});

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

    it('parses a multi-segment uid, keeping the tail as the id', () => {
        const inst = fromBackend({ ...base, uid: 'user.okta.alice', attributes: { _kind: 'user' } });
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
        const inst = fromBackend({ ...base, attributes: { _kind: 'user', _source: 'apim' } });
        expect(inst.source).toBe('apim');
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

    it('preserves backend id', () => {
        const inst = fromBackend(base);
        expect(inst._backendId).toBe('backend-id-1');
    });
});

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
            source: 'apim',
        });
        expect(req.attributes['_source']).toBe('apim');
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
        // _kind is consumed by fromBackend into the structured uid.type and
        // must be re-emitted by toBackend so the backend keeps the explicit
        // kind on the next PUT.
        expect(req.attributes['_kind']).toBe('group');
    });

    it('writes _kind back even when the source response did not carry it', () => {
        const inst = fromBackend({
            id: 'x',
            environmentId: 'DEFAULT',
            uid: 'mcp.flight',
            attributes: { name: 'Flight MCP' },
            parents: [],
            createdAt: '',
            updatedAt: '',
        });
        const req = toBackend(inst);
        // uid.type was inferred from the dotted prefix (mcp → MCPServer);
        // toBackend re-emits _kind so the round-trip keeps the kind hint.
        expect(req.attributes['_kind']).toBe('mcp');
    });

    it('round-trips a multi-segment principal without re-wrapping', () => {
        const response: EntityResponse = {
            id: 'x',
            environmentId: 'DEFAULT',
            uid: 'user.okta.alice',
            attributes: { _kind: 'user', _source: 'apim' },
            parents: ['group.okta.engineering'],
            createdAt: '',
            updatedAt: '',
        };
        const inst = fromBackend(response);
        const req = toBackend(inst);
        expect(req.uid).toBe('user.okta.alice');
        expect(req.parents).toEqual(['group.okta.engineering']);
        expect(req.attributes['_source']).toBe('apim');
    });
});
