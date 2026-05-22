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
export interface ParsedEntity {
    name: string;
    parents: string[];
    attributes: Array<{ name: string; type: string }>;
    line: number;
}

export interface ParsedAction {
    name: string;
    principals: string[];
    resources: string[];
    line: number;
}

export interface ParsedSchema {
    entities: ParsedEntity[];
    actions: ParsedAction[];
    diagnostics: string[];
}

interface Token {
    value: string;
    line: number;
}

function stripComments(text: string): string {
    return text
        .split('\n')
        .map(line => {
            let inString = false;
            for (let i = 0; i < line.length; i++) {
                const c = line[i];
                if (inString) {
                    if (c === '\\' && i + 1 < line.length) {
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
                if (c === '/' && line[i + 1] === '/') {
                    return line.slice(0, i);
                }
            }
            return line;
        })
        .join('\n');
}

function tokenise(strippedText: string): Token[] {
    const tokens: Token[] = [];
    const lines = strippedText.split('\n');

    for (let lineIdx = 0; lineIdx < lines.length; lineIdx++) {
        const lineNum = lineIdx + 1;
        const line = lines[lineIdx];

        const re = /"(?:\\.|[^"\\])*"|[A-Za-z_][A-Za-z0-9_]*|[{}[\],;:]/g;
        let m: RegExpExecArray | null;
        while ((m = re.exec(line)) !== null) {
            tokens.push({ value: m[0], line: lineNum });
        }
    }

    return tokens;
}

class Parser {
    private pos = 0;
    readonly entities: ParsedEntity[] = [];
    readonly actions: ParsedAction[] = [];
    readonly diagnostics: string[] = [];

    constructor(private readonly tokens: Token[]) {}

    private peek(): Token | undefined {
        return this.tokens[this.pos];
    }

    private advance(): Token | undefined {
        return this.tokens[this.pos++];
    }

    private expect(value: string): boolean {
        const tok = this.peek();
        if (tok?.value === value) {
            this.advance();
            return true;
        }
        this.diagnostics.push(`Expected '${value}' at line ${tok?.line ?? '?'}, got '${tok?.value ?? 'EOF'}'`);
        return false;
    }

    private recoverTo(...keywords: string[]): void {
        while (this.pos < this.tokens.length) {
            const tok = this.tokens[this.pos];
            if (keywords.includes(tok.value)) return;
            this.pos++;
        }
    }

    private parseNameList(): string[] {
        const names: string[] = [];
        if (!this.expect('[')) return names;

        while (this.pos < this.tokens.length) {
            const tok = this.peek();
            if (!tok || tok.value === ']') break;
            if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(tok.value)) {
                names.push(tok.value);
                this.advance();
            }
            // skip comma
            if (this.peek()?.value === ',') this.advance();
        }

        this.expect(']');
        return names;
    }

    private parseEntity(startLine: number): void {
        const nameTok = this.peek();
        if (!nameTok || !/^[A-Za-z_]/.test(nameTok.value)) {
            this.diagnostics.push(`Expected entity name at line ${startLine}`);
            this.recoverTo('entity', 'action');
            return;
        }
        const name = nameTok.value;
        this.advance();

        let parents: string[] = [];
        if (this.peek()?.value === 'in') {
            this.advance();
            parents = this.parseNameList();
        }

        if (!this.expect('{')) {
            this.recoverTo('entity', 'action');
            return;
        }

        const attributes: Array<{ name: string; type: string }> = [];

        while (this.pos < this.tokens.length) {
            const tok = this.peek();
            if (!tok) {
                this.diagnostics.push(`Unbalanced brace in entity '${name}' starting at line ${startLine}`);
                return;
            }
            if (tok.value === '}') {
                this.advance();
                break;
            }
            if (tok.value === 'entity' || tok.value === 'action') {
                this.diagnostics.push(`Unbalanced brace in entity '${name}' starting at line ${startLine}`);
                return;
            }
            if (/^[A-Za-z_]/.test(tok.value)) {
                const attrName = tok.value;
                this.advance();
                if (this.peek()?.value === ':') {
                    this.advance();
                    const typeTok = this.peek();
                    if (typeTok && /^[A-Za-z_]/.test(typeTok.value)) {
                        attributes.push({ name: attrName, type: typeTok.value });
                        this.advance();
                    }
                }
                if (this.peek()?.value === ',') this.advance();
                continue;
            }
            if (tok.value === ',' || tok.value === ';') {
                this.advance();
                continue;
            }
            this.advance();
        }

        if (this.peek()?.value === ';') this.advance();

        this.entities.push({ name, parents, attributes, line: startLine });
    }

    private parseAction(startLine: number): void {
        const nameTok = this.peek();
        if (!nameTok || !nameTok.value.startsWith('"')) {
            this.diagnostics.push(`Expected quoted action name at line ${startLine}`);
            this.recoverTo('entity', 'action');
            return;
        }
        const name = nameTok.value.replace(/^"|"$/g, '').replace(/\\(["\\])/g, '$1');
        this.advance();

        if (this.peek()?.value !== 'appliesTo') {
            this.diagnostics.push(`Expected 'appliesTo' after action name at line ${startLine}`);
            this.recoverTo('entity', 'action');
            return;
        }
        this.advance();

        if (!this.expect('{')) {
            this.recoverTo('entity', 'action');
            return;
        }

        let principals: string[] = [];
        let resources: string[] = [];

        while (this.pos < this.tokens.length) {
            const tok = this.peek();
            if (!tok) {
                this.diagnostics.push(`Unbalanced brace in action '${name}' starting at line ${startLine}`);
                return;
            }
            if (tok.value === '}') {
                this.advance();
                break;
            }
            if (tok.value === 'entity' || tok.value === 'action') {
                this.diagnostics.push(`Unbalanced brace in action '${name}' starting at line ${startLine}`);
                return;
            }
            if (tok.value === 'principal') {
                this.advance();
                if (this.peek()?.value === ':') this.advance();
                principals = this.parseNameList();
                if (this.peek()?.value === ',') this.advance();
                continue;
            }
            if (tok.value === 'resource') {
                this.advance();
                if (this.peek()?.value === ':') this.advance();
                resources = this.parseNameList();
                if (this.peek()?.value === ',') this.advance();
                continue;
            }
            this.advance();
        }

        if (this.peek()?.value === ';') this.advance();

        this.actions.push({ name, principals, resources, line: startLine });
    }

    parse(): void {
        while (this.pos < this.tokens.length) {
            const tok = this.advance();
            if (!tok) break;

            if (tok.value === 'entity') {
                this.parseEntity(tok.line);
            } else if (tok.value === 'action') {
                this.parseAction(tok.line);
            }
        }
    }
}

export function parseGaplSchema(text: string): ParsedSchema {
    if (!text.trim()) {
        return { entities: [], actions: [], diagnostics: [] };
    }

    const stripped = stripComments(text);
    const tokens = tokenise(stripped);

    const parser = new Parser(tokens);
    parser.parse();

    return {
        entities: parser.entities,
        actions: parser.actions,
        diagnostics: parser.diagnostics,
    };
}
