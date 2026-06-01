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
const TOKEN_SOURCE = '[A-Za-z][\\w-]*::"(?:[^"\\\\]|\\\\.)*"';
const AGENT_CLAUSE_SOURCE = `context\\.agent\\s*(?:==|in)\\s*(?:\\[[^\\]]*\\]|${TOKEN_SOURCE})`;
const TOKEN = new RegExp(TOKEN_SOURCE, 'g');

/**
 * Reads the agent tokens (e.g. `AgentIdentity::"x"`) from a managed
 * `context.agent` clause embedded in a free-text GAPL condition.
 */
export function readAgentIds(condition: string | undefined): string[] {
    if (!condition) {
        return [];
    }
    const match = condition.match(new RegExp(`context\\.agent\\s*(?:==|in)\\s*(\\[[^\\]]*\\]|${TOKEN_SOURCE})`));
    if (!match) {
        return [];
    }
    return match[1].match(TOKEN) ?? [];
}

/**
 * Removes the managed `context.agent` clause (and a single adjacent `&&`
 * joiner) from a condition, leaving the rest of the user's text intact.
 */
export function stripAgentClause(condition: string): string {
    const clause = AGENT_CLAUSE_SOURCE;
    let next = condition.replace(new RegExp(`\\s*&&\\s*${clause}`), '');
    if (next === condition) {
        next = condition.replace(new RegExp(`${clause}\\s*&&\\s*`), '');
    }
    if (next === condition) {
        next = condition.replace(new RegExp(clause), '');
    }
    return next.trim();
}

/**
 * Inserts or replaces the managed `context.agent` clause for the given
 * agent tokens, preserving any other user-authored condition text.
 */
export function upsertAgentClause(condition: string, agentIds: readonly string[]): string {
    const rest = stripAgentClause(condition || '');
    if (agentIds.length === 0) {
        return rest;
    }
    const clause = agentIds.length === 1 ? `context.agent == ${agentIds[0]}` : `context.agent in [${agentIds.join(', ')}]`;
    return rest ? `${clause} && ${rest}` : clause;
}
