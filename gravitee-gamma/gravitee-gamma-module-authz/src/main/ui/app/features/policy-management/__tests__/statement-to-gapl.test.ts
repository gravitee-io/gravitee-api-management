import { describe, expect, it } from 'vitest';
import { createEmptyStatement, statementsToGapl, statementToGapl } from '../statement-to-gapl';

describe('statementToGapl', () => {
    it('serializes a permit with single principal/action/resource', () => {
        const out = statementToGapl({
            id: 's1',
            effect: 'permit',
            principals: [{ id: 'u1', kind: 'user', label: 'alice' }],
            actions: [{ id: 'a1', label: 'read' }],
            resources: [{ id: 'r1', kind: 'tool', label: 'get_flight_status' }],
        });
        expect(out).toContain('permit (');
        expect(out).toContain('principal == user::"alice"');
        // Default action namespace is the canonical 'Action' (Cedar/GAPL
        // style). Callers can override per-ref via ActionRef.kind to
        // round-trip arbitrary casing.
        expect(out).toContain('action == Action::"read"');
        expect(out).toContain('resource == tool::"get_flight_status"');
        expect(out.trim().endsWith(';')).toBe(true);
    });

    it('uses "in" for multiple refs', () => {
        const out = statementToGapl({
            id: 's1',
            effect: 'forbid',
            principals: [
                { id: 'u1', kind: 'user', label: 'alice' },
                { id: 'u2', kind: 'user', label: 'bob' },
            ],
            actions: [{ id: 'a1', label: 'delete' }],
            resources: [{ id: 'r1', kind: 'api', label: 'orders' }],
        });
        expect(out).toContain('principal in [user::"alice", user::"bob"]');
        expect(out).toContain('forbid (');
    });

    // Bug D — when an ActionRef carries an explicit `kind`, the serialiser
    // must echo it verbatim so a stored policy with 'Action::"read"' (or
    // any other namespace casing) round-trips through the visual editor
    // without lowercasing.
    it('preserves explicit ActionRef.kind (Bug D casing roundtrip)', () => {
        const out = statementToGapl({
            id: 's1',
            effect: 'permit',
            principals: [{ id: 'u', kind: 'User', label: 'alice' }],
            actions: [{ id: 'a', label: 'read', kind: 'Action' }],
            resources: [{ id: 'r', kind: 'Resource', label: 'r1' }],
        });
        expect(out).toContain('action == Action::"read"');
        expect(out).not.toContain('action == action::"read"');
    });

    it('appends a when clause when condition is non-empty', () => {
        const out = statementToGapl({
            ...createEmptyStatement(),
            principals: [{ id: 'u1', kind: 'user', label: 'a' }],
            actions: [{ id: 'a1', label: 'read' }],
            resources: [{ id: 'r1', kind: 'tool', label: 't' }],
            condition: 'context.time.hour >= 9',
        });
        expect(out).toContain('when {');
        expect(out).toContain('context.time.hour >= 9');
    });

    // Bug B: empty chip arrays must emit the slot-only form so the backend's
    // GAPL parser accepts the policy. The previous behaviour emitted
    // `principal == []` which is invalid GAPL.
    describe('empty clauses (Bug B)', () => {
        it('emits slot-only form when all three clauses are empty', () => {
            const out = statementToGapl(createEmptyStatement('permit'));
            expect(out).toContain('principal,');
            expect(out).toContain('action,');
            // Last clause has no trailing comma — match the standalone keyword.
            expect(out).toMatch(/resource\s*\n?\)/);
            // No invalid `== []` shape may leak through.
            expect(out).not.toMatch(/principal\s*==\s*\[\]/);
            expect(out).not.toMatch(/action\s*==\s*\[\]/);
            expect(out).not.toMatch(/resource\s*==\s*\[\]/);
        });

        it('emits == when a single chip is present', () => {
            const out = statementToGapl({
                ...createEmptyStatement('permit'),
                principals: [{ id: 'u1', kind: 'User', label: 'alice' }],
            });
            expect(out).toContain('principal == User::"alice"');
            // Other empty clauses still slot-only.
            expect(out).toContain('action,');
            expect(out).toMatch(/resource\s*\n?\)/);
        });

        it('emits `in [...]` when multiple chips are present', () => {
            const out = statementToGapl({
                ...createEmptyStatement('permit'),
                principals: [
                    { id: 'u1', kind: 'User', label: 'alice' },
                    { id: 'u2', kind: 'User', label: 'bob' },
                ],
            });
            expect(out).toContain('principal in [User::"alice", User::"bob"]');
        });
    });

    it('escapes quotes in labels', () => {
        const out = statementToGapl({
            ...createEmptyStatement(),
            principals: [{ id: 'u', kind: 'user', label: 'al"ice' }],
            actions: [{ id: 'a', label: 'read' }],
            resources: [{ id: 'r', kind: 'tool', label: 't' }],
        });
        expect(out).toContain('user::"al\\"ice"');
    });
});

describe('statementsToGapl', () => {
    it('adds target line when target is present', () => {
        const full = statementsToGapl(
            'p1',
            [
                {
                    ...createEmptyStatement(),
                    principals: [{ id: 'u', kind: 'user', label: 'a' }],
                    actions: [{ id: 'a', label: 'x' }],
                    resources: [{ id: 'r', kind: 't', label: 'y' }],
                },
            ],
            { label: 'Flight Status MCP' },
        );
        expect(full).toContain('// Policy: p1');
        expect(full).toContain('// Target: Flight Status MCP');
    });
});
