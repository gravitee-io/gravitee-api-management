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
import { readAgentIds, stripAgentClause, upsertAgentClause } from '../agent-condition';

const A = 'AgentIdentity::"agent-a"';
const B = 'AgentIdentity::"agent-b"';

describe('readAgentIds', () => {
    it('returns [] for empty/undefined', () => {
        expect(readAgentIds(undefined)).toEqual([]);
        expect(readAgentIds('')).toEqual([]);
    });

    it('returns [] when there is no agent clause', () => {
        expect(readAgentIds('resource.public == true')).toEqual([]);
    });

    it('reads a single agent via ==', () => {
        expect(readAgentIds(`context.agent == ${A}`)).toEqual([A]);
    });

    it('reads multiple agents via in [...]', () => {
        expect(readAgentIds(`context.agent in [${A}, ${B}]`)).toEqual([A, B]);
    });

    it('reads the agent clause when combined with other text', () => {
        expect(readAgentIds(`context.agent == ${A} && resource.public == true`)).toEqual([A]);
    });
});

describe('stripAgentClause', () => {
    it('removes a leading agent clause and its joiner', () => {
        expect(stripAgentClause(`context.agent == ${A} && resource.public == true`)).toBe('resource.public == true');
    });

    it('removes a trailing agent clause and its joiner', () => {
        expect(stripAgentClause(`resource.public == true && context.agent == ${A}`)).toBe('resource.public == true');
    });

    it('removes a lone agent clause', () => {
        expect(stripAgentClause(`context.agent in [${A}, ${B}]`)).toBe('');
    });

    it('leaves a clause-free condition untouched', () => {
        expect(stripAgentClause('resource.public == true')).toBe('resource.public == true');
    });
});

describe('upsertAgentClause', () => {
    it('returns the rest unchanged when no agents are selected', () => {
        expect(upsertAgentClause('resource.public == true', [])).toBe('resource.public == true');
    });

    it('returns empty when clearing the only clause', () => {
        expect(upsertAgentClause(`context.agent == ${A}`, [])).toBe('');
    });

    it('inserts a single-agent clause into an empty condition', () => {
        expect(upsertAgentClause('', [A])).toBe(`context.agent == ${A}`);
    });

    it('inserts a multi-agent clause', () => {
        expect(upsertAgentClause('', [A, B])).toBe(`context.agent in [${A}, ${B}]`);
    });

    it('replaces an existing clause and preserves the rest', () => {
        expect(upsertAgentClause(`context.agent == ${A} && resource.public == true`, [A, B])).toBe(
            `context.agent in [${A}, ${B}] && resource.public == true`,
        );
    });

    it('round-trips read → upsert', () => {
        const condition = `context.agent in [${A}, ${B}] && resource.public == true`;
        expect(upsertAgentClause(condition, readAgentIds(condition))).toBe(condition);
    });
});
