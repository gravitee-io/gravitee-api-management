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
import {
    statementToGapl,
    statementsToGapl,
    type ActionRef,
    type PolicyEffect,
    type PolicyStatement,
    type PrincipalRef,
    type ResourceRef,
} from '../../features/policy-management/statement-to-gapl';
import { parseGaplToStatements } from '../gapl-policy-parser';

function shape(s: PolicyStatement) {
    return {
        effect: s.effect,
        principals: s.principals.map(p => ({ kind: p.kind, label: p.label })),
        actions: s.actions.map(a => ({ label: a.label })),
        resources: s.resources.map(r => ({ kind: r.kind, label: r.label })),
        condition: (s.condition ?? '').trim(),
    };
}

function* mulberry32(seed: number): Generator<number> {
    let s = seed >>> 0;
    while (true) {
        s = (s + 0x6d2b79f5) >>> 0;
        let t = s;
        t = Math.imul(t ^ (t >>> 15), t | 1);
        t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
        yield ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    }
}

function pickType(rng: Generator<number>): string {
    const len = 2 + Math.floor(rng.next().value * 6);
    const first = String.fromCharCode(65 + Math.floor(rng.next().value * 26));
    let rest = '';
    for (let i = 1; i < len; i++) {
        rest += String.fromCharCode(97 + Math.floor(rng.next().value * 26));
    }
    return first + rest;
}

function pickLabel(rng: Generator<number>): string {
    const len = 2 + Math.floor(rng.next().value * 6);
    let out = '';
    for (let i = 0; i < len; i++) {
        const r = rng.next().value;
        if (r < 0.7) {
            out += String.fromCharCode(97 + Math.floor(rng.next().value * 26));
        } else {
            out += String.fromCharCode(48 + Math.floor(rng.next().value * 10));
        }
    }
    return out;
}

function pickEffect(rng: Generator<number>): PolicyEffect {
    return rng.next().value < 0.5 ? 'permit' : 'forbid';
}

function buildStatement(rng: Generator<number>): PolicyStatement {
    const principals: PrincipalRef[] = [];
    const actions: ActionRef[] = [];
    const resources: ResourceRef[] = [];
    const pCount = Math.floor(rng.next().value * 3);
    const aCount = Math.floor(rng.next().value * 3);
    const rCount = Math.floor(rng.next().value * 3);
    for (let i = 0; i < pCount; i++) principals.push({ id: pickLabel(rng), kind: pickType(rng), label: pickLabel(rng) });
    for (let i = 0; i < aCount; i++) actions.push({ id: pickLabel(rng), kind: 'Action', label: pickLabel(rng) });
    for (let i = 0; i < rCount; i++) resources.push({ id: pickLabel(rng), kind: pickType(rng), label: pickLabel(rng) });
    return {
        id: 'seeded',
        effect: pickEffect(rng),
        principals,
        actions,
        resources,
        condition: '',
    };
}

function buildBatch(seed: number, count: number): PolicyStatement[] {
    const rng = mulberry32(seed);
    return Array.from({ length: count }, () => buildStatement(rng));
}

describe('parseGaplToStatements — roundtrip via statementsToGapl', () => {
    const seeds = [3, 17, 91, 555, 1234];
    it.each(seeds)('seed %i: parses serialised statements back to the same shapes', seed => {
        const stmts = buildBatch(seed, 3);
        const text = statementsToGapl('p', stmts, { label: 'Test' });
        const parsed = parseGaplToStatements(text);
        expect(parsed.diagnostics).toEqual([]);
        expect(parsed.statements).toHaveLength(stmts.length);
        expect(parsed.statements.map(shape)).toEqual(stmts.map(shape));
    });
});

describe('parseGaplToStatements — slot-only form', () => {
    const effects: PolicyEffect[] = ['permit', 'forbid'];
    it.each(effects)('always parses bare `%s (principal, action, resource);` to one empty statement', eff => {
        const text = `${eff} (principal, action, resource);`;
        const parsed = parseGaplToStatements(text);
        expect(parsed.diagnostics).toEqual([]);
        expect(parsed.statements).toHaveLength(1);
        const s = parsed.statements[0];
        expect(s.effect).toBe(eff);
        expect(s.principals).toEqual([]);
        expect(s.actions).toEqual([]);
        expect(s.resources).toEqual([]);
    });
});

describe('parseGaplToStatements — multi-statement order preservation', () => {
    const sequences: PolicyEffect[][] = [
        ['permit', 'forbid'],
        ['forbid', 'forbid', 'permit'],
        ['permit', 'permit', 'permit', 'forbid'],
        ['forbid', 'permit', 'forbid', 'permit', 'forbid'],
    ];
    it.each(sequences)('preserves order for sequence %j', (...effects) => {
        const text = effects.map(e => `${e} (principal, action, resource);`).join('\n');
        const parsed = parseGaplToStatements(text);
        expect(parsed.diagnostics).toEqual([]);
        expect(parsed.statements.map(s => s.effect)).toEqual(effects);
    });
});

describe('parseGaplToStatements — comments + blank lines are ignored', () => {
    const seeds = [4, 22, 73, 444];
    const commentSets = [['// foo', '// bar'], ['// Policy: test', '// Target: t1', '// noise'], [], ['// just one']];
    const cases = seeds.flatMap(seed => commentSets.map(comments => [seed, comments] as const));
    it.each(cases)('seed %i, comments %j: random `// ...` lines do not change parse result', (seed, comments) => {
        const stmts = buildBatch(seed, 2);
        const tight = stmts.map(statementToGapl).join('\n\n');
        const loose = [...comments, ...stmts.map(statementToGapl), ...comments].join('\n');
        const a = parseGaplToStatements(tight);
        const b = parseGaplToStatements(loose);
        expect(a.diagnostics).toEqual([]);
        expect(b.diagnostics).toEqual([]);
        expect(b.statements.map(shape)).toEqual(a.statements.map(shape));
    });
});

describe('parseGaplToStatements — effect alternation', () => {
    const sequences: PolicyEffect[][] = [
        ['permit'],
        ['forbid'],
        ['permit', 'forbid', 'permit'],
        ['forbid', 'forbid', 'permit', 'forbid'],
        ['permit', 'permit', 'forbid', 'permit', 'forbid', 'forbid'],
    ];
    it.each(sequences)('round-trips effect sequence %j with single principal per statement', (...effects) => {
        const text = effects.map(e => `${e} (principal == User::"u", action == Action::"a", resource == Tool::"t");`).join('\n');
        const parsed = parseGaplToStatements(text);
        expect(parsed.diagnostics).toEqual([]);
        expect(parsed.statements.map(s => s.effect)).toEqual(effects);
    });
});

describe('parseGaplToStatements — unsupported syntax surfaces diagnostics', () => {
    const unsupportedKeywords = ['unless', 'allow', 'deny', 'rule', 'when', 'else'];
    it.each(unsupportedKeywords)('emits a diagnostic for input starting with `%s`', kw => {
        const text = `${kw} (principal == User::"a", action == Action::"r", resource == Tool::"t");`;
        expect(() => parseGaplToStatements(text)).not.toThrow();
        const parsed = parseGaplToStatements(text);
        expect(parsed.diagnostics.length).toBeGreaterThan(0);
    });

    const trailingUnlessCases: Array<[string, string]> = [
        ['User', 'alice'],
        ['Service', 'sa1'],
        ['Group', 'admins'],
        ['Account', 'a_b'],
    ];
    it.each(trailingUnlessCases)('emits a diagnostic for trailing `unless { ... }` (type=%s, id=%s)', (t, l) => {
        const text = `permit (principal == ${t}::"${l}", action == Action::"r", resource == Tool::"t") unless { false };`;
        const parsed = parseGaplToStatements(text);
        expect(parsed.diagnostics.length).toBeGreaterThan(0);
    });
});
