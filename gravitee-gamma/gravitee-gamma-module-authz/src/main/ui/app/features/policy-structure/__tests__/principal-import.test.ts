import { describe, expect, it } from 'vitest';
import { parsePrincipalJson } from '../principal-import';

describe('parsePrincipalJson', () => {
    it('rejects malformed JSON', () => {
        const result = parsePrincipalJson('{not valid json');
        expect(result.ok).toBe(false);
        expect(result.parseError).toMatch(/invalid json/i);
    });

    it('rejects non-array top-level', () => {
        const result = parsePrincipalJson('{ "uid": { "type": "User", "id": "a" } }');
        expect(result.ok).toBe(false);
        expect(result.parseError).toMatch(/array/i);
    });

    it('accepts an empty array', () => {
        const result = parsePrincipalJson('[]');
        expect(result.ok).toBe(true);
        expect(result.items).toHaveLength(0);
    });

    it('parses a valid User', () => {
        const json = JSON.stringify([{ uid: { type: 'User', id: 'alice' }, attrs: { name: 'Alice', email: 'alice@example.com' } }]);
        const result = parsePrincipalJson(json);
        expect(result.ok).toBe(true);
        expect(result.items).toHaveLength(1);
        expect(result.items[0].error).toBeUndefined();
        expect(result.items[0].entity?.uid).toEqual({ type: 'User', id: 'alice' });
        expect(result.items[0].entity?.attrs.name).toBe('Alice');
    });

    it('rejects unknown principal type', () => {
        const json = JSON.stringify([{ uid: { type: 'Robot', id: 'r2d2' } }]);
        const result = parsePrincipalJson(json);
        expect(result.ok).toBe(true);
        expect(result.items[0].error).toMatch(/uid.type/i);
    });

    it('rejects item without uid', () => {
        const json = JSON.stringify([{ attrs: { name: 'Anon' } }]);
        const result = parsePrincipalJson(json);
        expect(result.ok).toBe(true);
        expect(result.items[0].error).toMatch(/uid/i);
    });

    it('rejects uid with empty type', () => {
        const json = JSON.stringify([{ uid: { type: '', id: 'alice' } }]);
        const result = parsePrincipalJson(json);
        expect(result.ok).toBe(true);
        expect(result.items[0].error).toMatch(/uid.type/i);
    });

    it('rejects invalid attrs value', () => {
        const json = JSON.stringify([{ uid: { type: 'Group', id: 'eng' }, attrs: { nested: { deep: 1 } } }]);
        const result = parsePrincipalJson(json);
        expect(result.ok).toBe(true);
        expect(result.items[0].error).toMatch(/attrs/i);
    });

    it('detects in-batch duplicates', () => {
        const json = JSON.stringify([
            { uid: { type: 'User', id: 'alice' } },
            { uid: { type: 'User', id: 'bob' } },
            { uid: { type: 'User', id: 'alice' } }, // duplicate
        ]);
        const result = parsePrincipalJson(json);
        expect(result.ok).toBe(true);
        expect(result.duplicateIndices.has(2)).toBe(true);
        expect(result.duplicateIndices.has(0)).toBe(false);
        expect(result.duplicateIndices.has(1)).toBe(false);
    });

    it('parses parents array', () => {
        const json = JSON.stringify([
            {
                uid: { type: 'User', id: 'alice' },
                parents: [{ type: 'Group', id: 'engineering' }],
            },
        ]);
        const result = parsePrincipalJson(json);
        expect(result.ok).toBe(true);
        expect(result.items[0].entity?.parents).toEqual([{ type: 'Group', id: 'engineering' }]);
    });

    it('handles mixed valid and invalid items', () => {
        const json = JSON.stringify([
            { uid: { type: 'User', id: 'alice' } }, // valid
            { uid: { type: 'Alien', id: 'et' } }, // invalid type
            { uid: { type: 'Group', id: 'eng' } }, // valid
        ]);
        const result = parsePrincipalJson(json);
        expect(result.ok).toBe(true);
        expect(result.items[0].error).toBeUndefined();
        expect(result.items[1].error).toBeDefined();
        expect(result.items[2].error).toBeUndefined();
    });
});
