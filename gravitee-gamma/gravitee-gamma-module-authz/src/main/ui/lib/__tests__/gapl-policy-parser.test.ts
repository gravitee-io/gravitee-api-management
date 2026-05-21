import { describe, expect, it } from 'vitest';
import { statementToGapl, statementsToGapl, type PolicyStatement } from '../../app/features/policy-management/statement-to-gapl';
import { parseGaplToStatements } from '../gapl-policy-parser';

/** Strip volatile fields (random ids) so we can compare statements by content. */
function shape(s: PolicyStatement) {
    return {
        effect: s.effect,
        principals: s.principals.map(p => ({ kind: p.kind, label: p.label })),
        actions: s.actions.map(a => ({ label: a.label })),
        resources: s.resources.map(r => ({ kind: r.kind, label: r.label })),
        condition: (s.condition ?? '').trim(),
    };
}

describe('parseGaplToStatements — supported shapes', () => {
    it('parses permit with single principal/action/resource', () => {
        const text = 'permit (principal == user::"alice", action == action::"read", resource == tool::"r1");';
        const result = parseGaplToStatements(text);
        expect(result).not.toBeNull();
        expect(result!.statements).toHaveLength(1);
        const s = result!.statements[0];
        expect(s.effect).toBe('permit');
        expect(s.principals).toEqual([expect.objectContaining({ kind: 'user', label: 'alice' })]);
        expect(s.actions).toEqual([expect.objectContaining({ label: 'read' })]);
        expect(s.resources).toEqual([expect.objectContaining({ kind: 'tool', label: 'r1' })]);
    });

    it('parses forbid effect', () => {
        const text = 'forbid (principal == user::"a", action == action::"x", resource == api::"o");';
        const result = parseGaplToStatements(text);
        expect(result!.statements[0].effect).toBe('forbid');
    });

    it('parses `in [ ... ]` for multi-principal', () => {
        const text = 'permit (principal in [user::"a", group::"admins"], action == action::"read", resource == tool::"t");';
        const result = parseGaplToStatements(text);
        expect(result!.statements[0].principals).toEqual([
            expect.objectContaining({ kind: 'user', label: 'a' }),
            expect.objectContaining({ kind: 'group', label: 'admins' }),
        ]);
    });

    it('parses multi-action and multi-resource', () => {
        const text = 'permit (principal == user::"a", action in [action::"read", action::"write"], resource in [tool::"r1", tool::"r2"]);';
        const result = parseGaplToStatements(text);
        const s = result!.statements[0];
        expect(s.actions.map(a => a.label)).toEqual(['read', 'write']);
        expect(s.resources.map(r => r.label)).toEqual(['r1', 'r2']);
    });

    it('parses optional when { ... } block as raw condition', () => {
        const text = 'permit (principal == user::"a", action == action::"r", resource == tool::"t") when { context.time.hour >= 9 };';
        const result = parseGaplToStatements(text);
        expect(result!.statements[0].condition).toContain('context');
        expect(result!.statements[0].condition).toContain('hour');
    });

    it('parses multiple statements separated by ;', () => {
        const text = `
            permit (principal == user::"a", action == action::"read", resource == tool::"t");
            forbid (principal == user::"b", action == action::"delete", resource == tool::"t");
        `;
        const result = parseGaplToStatements(text);
        expect(result!.statements).toHaveLength(2);
        expect(result!.statements[0].effect).toBe('permit');
        expect(result!.statements[1].effect).toBe('forbid');
    });

    it('ignores // comment lines', () => {
        const text = `
            // Policy: foo
            // Target: bar
            permit (principal == user::"a", action == action::"read", resource == tool::"t");
        `;
        const result = parseGaplToStatements(text);
        expect(result!.statements).toHaveLength(1);
    });

    it('returns empty statements for empty input', () => {
        const result = parseGaplToStatements('');
        expect(result).toEqual({ statements: [], warnings: [] });
    });

    it('returns empty statements for whitespace-only input', () => {
        const result = parseGaplToStatements('   \n\n  ');
        expect(result).toEqual({ statements: [], warnings: [] });
    });

    // Bug B: slot-only form for empty clauses must round-trip cleanly.
    it('parses slot-only form with all three clauses empty', () => {
        const text = 'permit (principal, action, resource);';
        const result = parseGaplToStatements(text);
        expect(result).not.toBeNull();
        expect(result!.statements).toHaveLength(1);
        const s = result!.statements[0];
        expect(s.effect).toBe('permit');
        expect(s.principals).toEqual([]);
        expect(s.actions).toEqual([]);
        expect(s.resources).toEqual([]);
    });

    it('parses mixed slot-only and bound clauses', () => {
        const text = 'permit (principal == User::"alice", action, resource);';
        const result = parseGaplToStatements(text);
        expect(result).not.toBeNull();
        const s = result!.statements[0];
        expect(s.principals).toHaveLength(1);
        expect(s.actions).toEqual([]);
        expect(s.resources).toEqual([]);
    });

    it('tolerates trailing semicolons and blank lines', () => {
        const text = 'permit (principal == user::"a", action == action::"r", resource == tool::"t");;\n\n';
        const result = parseGaplToStatements(text);
        expect(result!.statements).toHaveLength(1);
    });
});

describe('parseGaplToStatements — roundtrip with statementToGapl', () => {
    const cases: Array<[string, PolicyStatement]> = [
        [
            'single permit',
            {
                id: 's1',
                effect: 'permit',
                principals: [{ id: 'u1', kind: 'user', label: 'alice' }],
                actions: [{ id: 'a1', label: 'read' }],
                resources: [{ id: 'r1', kind: 'tool', label: 'get_flight_status' }],
                condition: '',
            },
        ],
        [
            'multi with forbid',
            {
                id: 's2',
                effect: 'forbid',
                principals: [
                    { id: 'u1', kind: 'user', label: 'alice' },
                    { id: 'u2', kind: 'group', label: 'ops' },
                ],
                actions: [
                    { id: 'a1', label: 'read' },
                    { id: 'a2', label: 'write' },
                ],
                resources: [{ id: 'r1', kind: 'api', label: 'orders' }],
                condition: '',
            },
        ],
        [
            'with condition',
            {
                id: 's3',
                effect: 'permit',
                principals: [{ id: 'u', kind: 'user', label: 'a' }],
                actions: [{ id: 'a', label: 'read' }],
                resources: [{ id: 'r', kind: 'tool', label: 't' }],
                condition: 'context.time.hour >= 9',
            },
        ],
    ];

    it.each(cases)('roundtrips %s', (_label, stmt) => {
        const gapl = statementToGapl(stmt);
        const parsed = parseGaplToStatements(gapl);
        expect(parsed).not.toBeNull();
        expect(parsed!.statements).toHaveLength(1);
        expect(shape(parsed!.statements[0])).toEqual(shape(stmt));
    });

    it('roundtrips a multi-statement document via statementsToGapl', () => {
        const stmts: PolicyStatement[] = [
            {
                id: 's1',
                effect: 'permit',
                principals: [{ id: 'u', kind: 'user', label: 'alice' }],
                actions: [{ id: 'a', label: 'read' }],
                resources: [{ id: 'r', kind: 'tool', label: 't1' }],
                condition: '',
            },
            {
                id: 's2',
                effect: 'forbid',
                principals: [{ id: 'u', kind: 'user', label: 'bob' }],
                actions: [{ id: 'a', label: 'delete' }],
                resources: [{ id: 'r', kind: 'tool', label: 't1' }],
                condition: '',
            },
        ];
        const text = statementsToGapl('p', stmts, { label: 'Test target' });
        const parsed = parseGaplToStatements(text);
        expect(parsed).not.toBeNull();
        expect(parsed!.statements.map(shape)).toEqual(stmts.map(shape));
    });
});

describe('parseGaplToStatements — unsupported / malformed input → null', () => {
    it('returns null for `unless` (Cedar-only feature)', () => {
        const text = 'permit (principal == user::"a", action == action::"r", resource == tool::"t") unless { false };';
        expect(parseGaplToStatements(text)).toBeNull();
    });

    it('returns null when `==` is missing', () => {
        const text = 'permit (principal user::"a", action == action::"r", resource == tool::"t");';
        expect(parseGaplToStatements(text)).toBeNull();
    });

    it('returns null when UID is malformed (missing :: or quotes)', () => {
        const text = 'permit (principal == user "a", action == action::"r", resource == tool::"t");';
        expect(parseGaplToStatements(text)).toBeNull();
    });

    it('returns null when UID id is not a string literal', () => {
        const text = 'permit (principal == user::alice, action == action::"r", resource == tool::"t");';
        expect(parseGaplToStatements(text)).toBeNull();
    });

    it('returns null on duplicated clause', () => {
        const text = 'permit (principal == user::"a", principal == user::"b", action == action::"r", resource == tool::"t");';
        expect(parseGaplToStatements(text)).toBeNull();
    });

    it('returns null on unrecognised top-level token', () => {
        const text = 'allow (principal == user::"a", action == action::"r", resource == tool::"t");';
        expect(parseGaplToStatements(text)).toBeNull();
    });
});
