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
import { parseGaplSchema } from '../gapl-parser';

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

function pickEntityName(rng: Generator<number>): string {
    const len = 2 + Math.floor(rng.next().value * 8);
    const first = String.fromCharCode(65 + Math.floor(rng.next().value * 26));
    let rest = '';
    for (let i = 1; i < len; i++) {
        const r = rng.next().value;
        if (r < 0.5) {
            rest += String.fromCharCode(97 + Math.floor(rng.next().value * 26));
        } else {
            rest += String.fromCharCode(48 + Math.floor(rng.next().value * 10));
        }
    }
    return first + rest;
}

function makeEntityBatch(seed: number, count: number): string[] {
    const rng = mulberry32(seed);
    const out = new Set<string>();
    while (out.size < count) {
        out.add(pickEntityName(rng));
    }
    return [...out];
}

const BLANK_INPUTS = ['', '   ', '\n\n', '\t\t', ' \n \t \r ', '\r\n\r\n', '\n   \n\t'];

describe('parseGaplSchema — blank input invariants', () => {
    it.each(BLANK_INPUTS)('returns empty result for %j', input => {
        const result = parseGaplSchema(input);
        expect(result.entities).toEqual([]);
        expect(result.actions).toEqual([]);
        expect(result.diagnostics).toEqual([]);
    });
});

describe('parseGaplSchema — random entity declarations parse without throwing', () => {
    const seeds = [1, 7, 42, 101, 333, 9001];
    it.each(seeds)('seed %i: parses each declared entity', seed => {
        const names = makeEntityBatch(seed, 5);
        const src = names.map(n => `entity ${n} {};`).join('\n');
        expect(() => parseGaplSchema(src)).not.toThrow();
        const parsed = new Set(parseGaplSchema(src).entities.map(e => e.name));
        for (const n of names) expect(parsed.has(n)).toBe(true);
    });
});

describe('parseGaplSchema — `//` inside string literal is not a comment', () => {
    const names = ['User', 'Group', 'MCPServer', 'Tool', 'Account'];
    it.each(names)('keeps entity %s when preceded by an action with `//` in its name', name => {
        const src = `action "//${name}//" appliesTo { principal: [P] };
entity ${name} {};`;
        const result = parseGaplSchema(src);
        expect(result.entities.some(e => e.name === name)).toBe(true);
        expect(result.actions.some(a => a.name === `//${name}//`)).toBe(true);
    });
});

describe('parseGaplSchema — escaped quote in action name', () => {
    const baseNames = ['User', 'Doc', 'Resource', 'Server'];
    it.each(baseNames)('round-trips escaped quote in `before\\"%s`', baseName => {
        const src = `action "before\\"${baseName}" appliesTo { principal: [P] };`;
        const result = parseGaplSchema(src);
        expect(result.actions).toHaveLength(1);
        expect(result.actions[0].name).toBe(`before"${baseName}`);
    });
});

describe('parseGaplSchema — N distinct entities preserved', () => {
    const seeds = [2, 13, 256, 4096];
    it.each(seeds)('seed %i: parses N distinct entities without loss', seed => {
        const names = makeEntityBatch(seed, 6);
        const src = names.map(n => `entity ${n} {};`).join('\n\n');
        const result = parseGaplSchema(src);
        expect(result.entities).toHaveLength(names.length);
        expect(result.entities.map(e => e.name).sort()).toEqual([...names].sort());
    });
});

describe('parseGaplSchema — whitespace insensitivity', () => {
    const seeds = [5, 88, 777];
    const wsRuns = [' ', '  ', '\t', ' \t '];
    it.each(seeds.flatMap(s => wsRuns.map(ws => [s, ws] as const)))(
        'seed %i, ws %j: tight vs loose entity declarations parse identically',
        (seed, ws) => {
            const names = makeEntityBatch(seed, 3);
            const tight = names.map(n => `entity ${n} {};`).join('\n');
            const loose = names.map(n => `entity${ws}${n}${ws}{${ws}}${ws};`).join('\n');
            const a = parseGaplSchema(tight);
            const b = parseGaplSchema(loose);
            expect(b.entities.map(e => e.name)).toEqual(a.entities.map(e => e.name));
            expect(b.diagnostics).toEqual(a.diagnostics);
        },
    );
});
