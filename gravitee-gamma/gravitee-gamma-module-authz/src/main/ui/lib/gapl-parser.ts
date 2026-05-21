/**
 * GAPL outline parser — extract entity and action definitions for the Schema page outline.
 *
 * Grammar subset parsed here (for outline only, not for validation):
 *
 *   entityDef    := 'entity' Name [ 'in' '[' NameList ']' ] '{' attrList? '}' ';'
 *   attrList     := attribute ( ',' attribute )*
 *   attribute    := Name ':' TypeName
 *   actionDef    := 'action' '"' Name '"' 'appliesTo' '{' actionClauses '}' ';'
 *   actionClause := ('principal' | 'resource') ':' '[' NameList ']'
 *   NameList     := Name ( ',' Name )*
 *
 * The parser is tolerant: malformed input produces diagnostics + partial results,
 * never throws. Line numbers in the result are 1-based and refer to the original
 * (un-stripped) source text so Monaco can scroll to the correct position.
 */

export interface ParsedEntity {
    name: string;
    parents: string[];
    attributes: Array<{ name: string; type: string }>;
    /** 1-based line number in the original text where the entity keyword appears. */
    line: number;
}

export interface ParsedAction {
    name: string;
    principals: string[];
    resources: string[];
    /** 1-based line number in the original text where the action keyword appears. */
    line: number;
}

export interface ParsedSchema {
    entities: ParsedEntity[];
    actions: ParsedAction[];
    /** Soft errors that do not block the editor — malformed constructs etc. */
    diagnostics: string[];
}

// ---------------------------------------------------------------------------
// Tokeniser helpers
// ---------------------------------------------------------------------------

/** A token is a non-whitespace run of characters with its 1-based line number. */
interface Token {
    value: string;
    line: number;
}

/**
 * Strip single-line comments (`// …`) from each line.
 *
 * String-literal aware: a `//` sequence inside a double-quoted string (with `\"`
 * escape support) is preserved. Block comments (`/* … *​/`) are not currently
 * supported — the prototype grammar only uses line comments.
 *
 * Returns the modified text; DOES NOT change line numbers (lines are preserved as
 * empty or partial — original line indices remain valid).
 */
function stripComments(text: string): string {
    return text
        .split('\n')
        .map(line => {
            let inString = false;
            for (let i = 0; i < line.length; i++) {
                const c = line[i];
                if (inString) {
                    if (c === '\\' && i + 1 < line.length) {
                        // Skip the escaped character verbatim.
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

/**
 * Tokenise stripped text, associating each token with its 1-based line number
 * from the *original* text (both share the same line structure because we only
 * blank out comment suffixes, never remove lines).
 */
function tokenise(strippedText: string): Token[] {
    const tokens: Token[] = [];
    const lines = strippedText.split('\n');

    for (let lineIdx = 0; lineIdx < lines.length; lineIdx++) {
        const lineNum = lineIdx + 1; // 1-based
        const line = lines[lineIdx];

        // Match quoted strings (with `\"` escape support) and regular word /
        // punctuation tokens. The string alternative consumes escape sequences
        // so an embedded `\"` does not close the literal early.
        const re = /"(?:\\.|[^"\\])*"|[A-Za-z_][A-Za-z0-9_]*|[{}[\],;:]/g;
        let m: RegExpExecArray | null;
        while ((m = re.exec(line)) !== null) {
            tokens.push({ value: m[0], line: lineNum });
        }
    }

    return tokens;
}

// ---------------------------------------------------------------------------
// Recursive-descent over the token stream
// ---------------------------------------------------------------------------

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

    /** Skip forward until we find one of the given top-level keywords or EOF. */
    private recoverTo(...keywords: string[]): void {
        while (this.pos < this.tokens.length) {
            const tok = this.tokens[this.pos];
            if (keywords.includes(tok.value)) return;
            this.pos++;
        }
    }

    /** Collect a comma-separated list of names inside `[` … `]`. */
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

    /** Parse `entity Name [ in [ NameList ] ] { attrList? } ;` */
    private parseEntity(startLine: number): void {
        const nameTok = this.peek();
        if (!nameTok || !/^[A-Za-z_]/.test(nameTok.value)) {
            this.diagnostics.push(`Expected entity name at line ${startLine}`);
            this.recoverTo('entity', 'action');
            return;
        }
        const name = nameTok.value;
        this.advance();

        // Optional `in [ NameList ]`
        let parents: string[] = [];
        if (this.peek()?.value === 'in') {
            this.advance(); // consume 'in'
            parents = this.parseNameList();
        }

        // `{`
        if (!this.expect('{')) {
            this.recoverTo('entity', 'action');
            return;
        }

        const attributes: Array<{ name: string; type: string }> = [];

        // attributes until `}`
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
            // If we encounter a top-level keyword, the brace was never closed — report and stop consuming
            if (tok.value === 'entity' || tok.value === 'action') {
                this.diagnostics.push(`Unbalanced brace in entity '${name}' starting at line ${startLine}`);
                // Do NOT advance — let the outer loop handle this keyword
                return;
            }
            // attr := Name ':' TypeName
            if (/^[A-Za-z_]/.test(tok.value)) {
                const attrName = tok.value;
                this.advance();
                if (this.peek()?.value === ':') {
                    this.advance(); // ':'
                    const typeTok = this.peek();
                    if (typeTok && /^[A-Za-z_]/.test(typeTok.value)) {
                        attributes.push({ name: attrName, type: typeTok.value });
                        this.advance();
                    }
                }
                // skip optional trailing comma
                if (this.peek()?.value === ',') this.advance();
                continue;
            }
            // skip commas and semicolons inside the body
            if (tok.value === ',' || tok.value === ';') {
                this.advance();
                continue;
            }
            // unexpected token
            this.advance();
        }

        // optional `;`
        if (this.peek()?.value === ';') this.advance();

        this.entities.push({ name, parents, attributes, line: startLine });
    }

    /** Parse `action "Name" appliesTo { actionClauses } ;` */
    private parseAction(startLine: number): void {
        // action name is a quoted string
        const nameTok = this.peek();
        if (!nameTok || !nameTok.value.startsWith('"')) {
            this.diagnostics.push(`Expected quoted action name at line ${startLine}`);
            this.recoverTo('entity', 'action');
            return;
        }
        // Strip the surrounding quotes and unescape `\"` and `\\` so the
        // logical action name matches what the user typed.
        const name = nameTok.value.replace(/^"|"$/g, '').replace(/\\(["\\])/g, '$1');
        this.advance();

        // `appliesTo`
        if (this.peek()?.value !== 'appliesTo') {
            this.diagnostics.push(`Expected 'appliesTo' after action name at line ${startLine}`);
            this.recoverTo('entity', 'action');
            return;
        }
        this.advance();

        // `{`
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
            // skip unknown tokens
            this.advance();
        }

        // optional `;`
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
            // everything else is ignored (comments already stripped)
        }
    }
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Parse a GAPL schema text and return its outline (entities + actions).
 *
 * - Tolerant: never throws; malformed constructs produce diagnostics.
 * - Line numbers are 1-based, relative to the original (un-stripped) source.
 */
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
