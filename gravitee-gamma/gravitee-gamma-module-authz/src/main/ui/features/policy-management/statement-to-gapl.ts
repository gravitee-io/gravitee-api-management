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
export type PolicyEffect = 'permit' | 'forbid';

export interface PrincipalRef {
    readonly id: string;
    readonly kind: string;
    readonly label: string;
}

export interface ActionRef {
    readonly id: string;
    readonly label: string;
    readonly kind?: string;
}

export interface ResourceRef {
    readonly id: string;
    readonly kind: string;
    readonly label: string;
}

export interface PolicyStatement {
    readonly id: string;
    readonly effect: PolicyEffect;
    readonly principals: readonly PrincipalRef[];
    readonly actions: readonly ActionRef[];
    readonly resources: readonly ResourceRef[];
    readonly condition?: string;
}

const DEFAULT_ACTION_KIND = 'Action';
const CANONICAL_UID_RE = /^[^:]+::".+"$/;

function quote(value: string): string {
    return `"${value.replace(/"/g, '\\"')}"`;
}

function emitToken(kind: string | undefined, id: string, label: string): string {
    if (CANONICAL_UID_RE.test(id)) return id;
    return `${kind ?? DEFAULT_ACTION_KIND}::${quote(label)}`;
}

function formatPrincipalTokens(refs: readonly PrincipalRef[]): string[] {
    return refs.map(r => emitToken(r.kind, r.id, r.label));
}

function formatActionTokens(refs: readonly ActionRef[]): string[] {
    return refs.map(a => emitToken(a.kind, a.id, a.label));
}

function formatResourceTokens(refs: readonly ResourceRef[]): string[] {
    return refs.map(r => emitToken(r.kind, r.id, r.label));
}

function formatClause(keyword: string, tokens: readonly string[]): string {
    if (tokens.length === 0) return keyword;
    if (tokens.length === 1) return `${keyword} == ${tokens[0]}`;
    return `${keyword} in [${tokens.join(', ')}]`;
}

export function statementToGapl(stmt: PolicyStatement): string {
    const lines: string[] = [];
    lines.push(`${stmt.effect} (`);
    const parts: string[] = [
        `  ${formatClause('principal', formatPrincipalTokens(stmt.principals))}`,
        `  ${formatClause('action', formatActionTokens(stmt.actions))}`,
        `  ${formatClause('resource', formatResourceTokens(stmt.resources))}`,
    ];
    lines.push(parts.join(',\n'));
    lines.push(')');
    if (stmt.condition !== undefined && stmt.condition.trim() !== '') {
        lines.push('when {');
        stmt.condition
            .trim()
            .split('\n')
            .forEach(l => lines.push(`  ${l}`));
        lines.push('}');
    }
    return `${lines.join('\n')};`;
}

export function statementsToGapl(name: string, statements: readonly PolicyStatement[], target?: { label: string }): string {
    const header = target ? `// Policy: ${name}\n// Target: ${target.label}` : `// Policy: ${name}`;
    const body = statements.map(statementToGapl).join('\n\n');
    return `${header}\n\n${body}`;
}

export function createEmptyStatement(effect: PolicyEffect = 'permit'): PolicyStatement {
    return {
        id: `stmt-${Math.random().toString(36).slice(2, 9)}`,
        effect,
        principals: [],
        actions: [],
        resources: [],
        condition: '',
    };
}
