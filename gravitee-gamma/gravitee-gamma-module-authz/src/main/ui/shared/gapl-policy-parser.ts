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
import type { ActionRef, PolicyEffect, PolicyStatement, PrincipalRef, ResourceRef } from '../features/policy-management/statement-to-gapl';
import { stripGaplComments } from './strip-gapl-comments';

export interface ParsedPolicy {
    readonly statements: readonly PolicyStatement[];
    readonly diagnostics: readonly string[];
}

interface Token {
    readonly value: string;
    readonly kind: 'word' | 'string' | 'punct' | 'op' | 'when-body';
    readonly raw?: string;
}

function findMatchingBrace(text: string, open: number): number {
    let depth = 0;
    let inString = false;
    for (let i = open; i < text.length; i++) {
        const c = text[i];
        if (inString) {
            if (c === '\\' && i + 1 < text.length) {
                i++;
                continue;
            }
            if (c === '"') inString = false;
            continue;
        }
        if (c === '"') {
            inString = true;
            continue;
        }
        if (c === '{') depth++;
        else if (c === '}') {
            depth--;
            if (depth === 0) return i;
        }
    }
    return -1;
}

interface TokeniseResult {
    readonly tokens?: Token[];
    readonly diagnostic?: string;
}

function tokenise(text: string): TokeniseResult {
    const tokens: Token[] = [];
    const re = /"(?:\\.|[^"\\])*"|==|::|[A-Za-z_][A-Za-z0-9_]*|[(){}\[\],;]/g;
    let m: RegExpExecArray | null;
    let lastIndex = 0;
    while ((m = re.exec(text)) !== null) {
        const between = text.slice(lastIndex, m.index);
        const stray = between.match(/\S/);
        if (stray) {
            return { diagnostic: `Unexpected character '${stray[0]}' at position ${lastIndex + (stray.index ?? 0)}` };
        }
        lastIndex = re.lastIndex;
        const v = m[0];
        if (v.startsWith('"')) {
            tokens.push({ value: v, kind: 'string' });
        } else if (v === '==' || v === '::') {
            tokens.push({ value: v, kind: 'op' });
        } else if (/^[A-Za-z_]/.test(v)) {
            if (v === 'when') {
                let i = re.lastIndex;
                while (i < text.length && /\s/.test(text[i])) i++;
                if (text[i] === '{') {
                    const close = findMatchingBrace(text, i);
                    if (close < 0) {
                        return { diagnostic: "Unterminated 'when {' block" };
                    }
                    const raw = text.slice(i + 1, close);
                    tokens.push({ value: 'when', kind: 'word' });
                    tokens.push({ value: '{', kind: 'punct' });
                    tokens.push({ value: '__raw__', kind: 'when-body', raw });
                    tokens.push({ value: '}', kind: 'punct' });
                    re.lastIndex = close + 1;
                    lastIndex = re.lastIndex;
                    continue;
                }
            }
            tokens.push({ value: v, kind: 'word' });
        } else {
            tokens.push({ value: v, kind: 'punct' });
        }
    }
    const trailingStray = text.slice(lastIndex).match(/\S/);
    if (trailingStray) {
        return { diagnostic: `Unexpected character '${trailingStray[0]}' near end of input` };
    }
    return { tokens };
}

class PolicyParser {
    private pos = 0;
    private readonly statements: PolicyStatement[] = [];
    private readonly diagnostics: string[] = [];
    private failed = false;

    constructor(private readonly tokens: readonly Token[]) {}

    private peek(o = 0): Token | undefined {
        return this.tokens[this.pos + o];
    }
    private next(): Token | undefined {
        return this.tokens[this.pos++];
    }
    private fail(msg: string): null {
        this.diagnostics.push(msg);
        this.failed = true;
        return null;
    }

    parse(): ParsedPolicy {
        while (this.pos < this.tokens.length && !this.failed) {
            const t = this.peek();
            if (!t) break;
            if (t.value === ';') {
                this.next();
                continue;
            }
            if (t.kind === 'word' && (t.value === 'permit' || t.value === 'forbid')) {
                if (this.parseStatement() === null) break;
                continue;
            }
            this.fail(`Unexpected token '${t.value}' at top level`);
            break;
        }
        return { statements: this.statements, diagnostics: this.diagnostics };
    }

    private parseStatement(): PolicyStatement | null {
        const effectTok = this.next();
        if (!effectTok) return this.fail('Expected effect keyword');
        const effect = effectTok.value as PolicyEffect;

        if (this.next()?.value !== '(') return this.fail("Expected '(' after effect");

        let principals: readonly PrincipalRef[] = [];
        let actions: readonly ActionRef[] = [];
        let resources: readonly ResourceRef[] = [];
        const seen = new Set<string>();

        while (this.peek() && this.peek()!.value !== ')') {
            const head = this.peek();
            if (!head || head.kind !== 'word') {
                return this.fail(`Expected 'principal'|'action'|'resource', got '${head?.value ?? 'EOF'}'`);
            }
            if (head.value !== 'principal' && head.value !== 'action' && head.value !== 'resource') {
                return this.fail(`Unsupported clause keyword '${head.value}'`);
            }
            if (seen.has(head.value)) return this.fail(`Duplicate clause '${head.value}'`);
            seen.add(head.value);
            this.next();

            const lookahead = this.peek();
            if (!lookahead || lookahead.value === ',' || lookahead.value === ')') {
                if (lookahead?.value === ',') this.next();
                continue;
            }

            const op = this.next();
            if (!op || (op.value !== '==' && op.value !== 'in')) {
                return this.fail(`Expected '==' or 'in' after '${head.value}'`);
            }

            const uids = op.value === '==' ? this.parseSingleUid() : this.parseUidList();
            if (uids === null) return null;

            if (head.value === 'principal') {
                principals = uids.map(u => ({ id: u.uid, kind: u.type, label: u.id }));
            } else if (head.value === 'action') {
                actions = uids.map(u => ({ id: u.uid, kind: u.type, label: u.id }));
            } else {
                resources = uids.map(u => ({ id: u.uid, kind: u.type, label: u.id }));
            }

            if (this.peek()?.value === ',') this.next();
        }

        if (this.next()?.value !== ')') return this.fail("Expected ')' to close statement body");

        let condition = '';
        if (this.peek()?.value === 'when') {
            this.next();
            const conditionResult = this.parseWhenBlock();
            if (conditionResult === null) return null;
            condition = conditionResult;
        }

        if (this.peek()?.value === 'unless') {
            return this.fail("'unless' clauses are not supported by the GAPL visual editor");
        }

        if (this.peek()?.value === ';') this.next();

        const stmt: PolicyStatement = {
            id: `stmt-${this.statements.length}`,
            effect,
            principals,
            actions,
            resources,
            condition,
        };
        this.statements.push(stmt);
        return stmt;
    }

    private parseSingleUid(): Array<{ uid: string; type: string; id: string }> | null {
        const uid = this.consumeUid();
        if (uid === null) return null;
        return [uid];
    }

    private parseUidList(): Array<{ uid: string; type: string; id: string }> | null {
        if (this.next()?.value !== '[') {
            return this.fail("Expected '[' after 'in'");
        }
        const out: Array<{ uid: string; type: string; id: string }> = [];
        while (this.peek() && this.peek()!.value !== ']') {
            const u = this.consumeUid();
            if (u === null) return null;
            out.push(u);
            if (this.peek()?.value === ',') this.next();
        }
        if (this.next()?.value !== ']') return this.fail("Expected ']' to close UID list");
        return out;
    }

    private consumeUid(): { uid: string; type: string; id: string } | null {
        const typeTok = this.next();
        if (!typeTok || typeTok.kind !== 'word') {
            return this.fail(`Expected entity type, got '${typeTok?.value ?? 'EOF'}'`);
        }
        if (this.next()?.value !== '::') {
            return this.fail(`Expected '::' after type '${typeTok.value}'`);
        }
        const idTok = this.next();
        if (!idTok || idTok.kind !== 'string') {
            return this.fail(`Expected quoted id after '${typeTok.value}::'`);
        }
        const id = idTok.value.slice(1, -1).replace(/\\(["\\])/g, '$1');
        return { uid: `${typeTok.value}::${idTok.value}`, type: typeTok.value, id };
    }

    private parseWhenBlock(): string | null {
        if (this.next()?.value !== '{') return this.fail("Expected '{' after 'when'");
        const body = this.next();
        if (!body || body.kind !== 'when-body') {
            return this.fail("Expected condition body after 'when {'");
        }
        if (this.next()?.value !== '}') return this.fail("Expected '}' to close 'when' block");
        return (body.raw ?? '').trim();
    }
}

export function parseGaplToStatements(text: string): ParsedPolicy {
    if (!text || !text.trim()) return { statements: [], diagnostics: [] };

    const stripped = stripGaplComments(text);
    const { tokens, diagnostic } = tokenise(stripped);
    if (diagnostic !== undefined) {
        return { statements: [], diagnostics: [diagnostic] };
    }
    if (!tokens || tokens.length === 0) return { statements: [], diagnostics: [] };

    return new PolicyParser(tokens).parse();
}
