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
/**
 * Property-based fuzz tests for `parseGaplSchema`.
 *
 * These complement the example-based tests in `gapl-parser.test.ts` by stressing
 * the parser against generated input. The parser is contract-tolerant: it must
 * never throw, and it must preserve specific invariants (entity counts, escape
 * round-trips, comment-in-string awareness, whitespace insensitivity).
 *
 * `numRuns` is kept modest (60–80) so the suite stays well under a second on CI.
 */
import * as fc from 'fast-check';
import { describe, expect, it } from 'vitest';
import { parseGaplSchema } from '../gapl-parser';

const RUNS = 80;

// `entityName` matches the parser's name regex `[A-Za-z_][A-Za-z0-9_]*` and
// keeps things readable (PascalCase, ≤ 16 chars).
const entityName = fc
    .string({ minLength: 1, maxLength: 16, unit: 'binary-ascii' })
    .filter(s => /^[A-Z][A-Za-z0-9]*$/.test(s));

// Whitespace runs that should be invisible to the tokeniser.
const wsRun = fc.stringMatching(/^[ \t]{1,4}$/);

describe('parseGaplSchema — fuzz: empty / whitespace input', () => {
    it('always returns empty entities/actions for blank-ish input', () => {
        fc.assert(
            fc.property(fc.stringMatching(/^[ \t\n\r]*$/), text => {
                const result = parseGaplSchema(text);
                expect(result.entities).toEqual([]);
                expect(result.actions).toEqual([]);
                expect(result.diagnostics).toEqual([]);
            }),
            { numRuns: RUNS },
        );
    });
});

describe('parseGaplSchema — fuzz: random entity declarations parse without throwing', () => {
    it('never throws for generated `entity X { ... };` declarations', () => {
        fc.assert(
            fc.property(fc.array(entityName, { minLength: 1, maxLength: 6 }), names => {
                const src = names.map(n => `entity ${n} {};`).join('\n');
                expect(() => parseGaplSchema(src)).not.toThrow();
                const result = parseGaplSchema(src);
                // de-duplicated set of names should all appear in the parsed output
                const parsed = new Set(result.entities.map(e => e.name));
                for (const n of new Set(names)) {
                    expect(parsed.has(n)).toBe(true);
                }
            }),
            { numRuns: RUNS },
        );
    });
});

describe('parseGaplSchema — fuzz: `//` inside string literal', () => {
    it("preserves the entity name regardless of `//` runs in a preceding action's quoted name", () => {
        fc.assert(
            fc.property(entityName, name => {
                // A quoted action name containing `//` would, if the comment
                // stripper were buggy, swallow everything to end-of-line including
                // the following entity declaration. The parser must keep both.
                const src = `action "//${name}//" appliesTo { principal: [P] };
entity ${name} {};`;
                const result = parseGaplSchema(src);
                expect(result.entities.some(e => e.name === name)).toBe(true);
                expect(result.actions.some(a => a.name === `//${name}//`)).toBe(true);
            }),
            { numRuns: RUNS },
        );
    });
});

describe('parseGaplSchema — fuzz: action names with embedded `\\"`', () => {
    it('round-trips an embedded escaped quote in the parsed action name', () => {
        fc.assert(
            fc.property(entityName, baseName => {
                // Build an action source whose quoted name contains a `\"`
                // (escaped double quote) — the parser should unescape it to a
                // literal `"` in the returned `name`.
                const src = `action "before\\"${baseName}" appliesTo { principal: [P] };`;
                const result = parseGaplSchema(src);
                expect(result.actions).toHaveLength(1);
                expect(result.actions[0].name).toBe(`before"${baseName}`);
            }),
            { numRuns: RUNS },
        );
    });
});

describe('parseGaplSchema — fuzz: multiple entities preserved', () => {
    it('parses N distinct entities without losing any', () => {
        fc.assert(
            fc.property(fc.uniqueArray(entityName, { minLength: 2, maxLength: 8 }), names => {
                const src = names.map(n => `entity ${n} {};`).join('\n\n');
                const result = parseGaplSchema(src);
                expect(result.entities).toHaveLength(names.length);
                expect(result.entities.map(e => e.name).sort()).toEqual([...names].sort());
            }),
            { numRuns: RUNS },
        );
    });
});

describe('parseGaplSchema — fuzz: whitespace insensitivity', () => {
    it('produces the same entity list when extra whitespace is inserted between tokens', () => {
        fc.assert(
            fc.property(fc.uniqueArray(entityName, { minLength: 1, maxLength: 4 }), wsRun, (names, ws) => {
                const tight = names.map(n => `entity ${n} {};`).join('\n');
                // Insert `ws` after each significant token of every declaration.
                const loose = names.map(n => `entity${ws}${n}${ws}{${ws}}${ws};`).join('\n');

                const a = parseGaplSchema(tight);
                const b = parseGaplSchema(loose);

                expect(b.entities.map(e => e.name)).toEqual(a.entities.map(e => e.name));
                expect(b.diagnostics).toEqual(a.diagnostics);
            }),
            { numRuns: RUNS },
        );
    });
});
