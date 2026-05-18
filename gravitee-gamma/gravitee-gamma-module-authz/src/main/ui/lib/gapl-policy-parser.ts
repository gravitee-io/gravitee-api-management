/**
 * GAPL → Visual roundtrip parser
 *
 * Parses the subset of GAPL that the visual policy editor can produce back
 * into `PolicyStatement[]`, so users can edit existing policies in the
 * visual builder. The supported grammar mirrors `statementToGapl` exactly:
 *
 *   policy        := statement (';' statement)* ';'?
 *   statement     := effect '(' clauseList ')' whenBlock? ';'?
 *   effect        := 'permit' | 'forbid'
 *   clauseList    := clause (',' clause){0,2}                  // any order
 *   clause        := ('principal' | 'action' | 'resource') op refs
 *   op            := '==' | 'in'
 *   refs          := uid                                       // for '=='
 *                  | '[' uid (',' uid)* ']'                    // for 'in'
 *   uid           := Type '::' '"' label '"'
 *   whenBlock     := 'when' '{' raw '}'
 *
 * Comment lines (`//`) and blank lines between statements are tolerated.
 *
 * Anything outside this grammar (e.g. `unless`, attribute equality, parent
 * lookups) causes the parser to return `null`. The caller must then fall
 * back to code-only editing — better to disable the visual toggle than to
 * silently lose information on save.
 *
 * Whitespace and semicolons inside `when { ... }` are captured verbatim
 * so we can roundtrip arbitrary condition expressions without parsing them.
 */

import type {
    ActionRef,
    PolicyEffect,
    PolicyStatement,
    PrincipalRef,
    ResourceRef,
} from '../app/features/policy-management/statement-to-gapl';

export interface ParsedPolicy {
    readonly statements: readonly PolicyStatement[];
    readonly warnings: readonly string[];
}

// ---------------------------------------------------------------------------
// Tokeniser — string-literal aware, comment-stripping, brace-aware
// ---------------------------------------------------------------------------

interface Token {
    readonly value: string;
    readonly kind: 'word' | 'string' | 'punct' | 'op' | 'when-body';
    /** For 'when-body' tokens: the raw text inside the braces, untokenised. */
    readonly raw?: string;
}

/** Strip `//` line comments while respecting double-quoted strings. */
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
                if (c === '/' && line[i + 1] === '/') return line.slice(0, i);
            }
            return line;
        })
        .join('\n');
}

/**
 * Find the matching `}` for the `{` at index `open`, respecting nested
 * braces and double-quoted strings. Returns the index of the closing brace
 * or -1 if unbalanced.
 */
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

/**
 * Tokenise GAPL text into a flat token stream. The tokeniser is naive about
 * structure — the parser handles grammar. To support arbitrary condition
 * expressions, `when { ... }` blocks are captured as opaque `when-body`
 * tokens whose `raw` field holds the body text verbatim — we never try to
 * lex what's inside.
 */
function tokenise(text: string): Token[] | null {
    const tokens: Token[] = [];
    const re = /"(?:\\.|[^"\\])*"|==|::|[A-Za-z_][A-Za-z0-9_]*|[(){}\[\],;]/g;
    let m: RegExpExecArray | null;
    let lastIndex = 0;
    while ((m = re.exec(text)) !== null) {
        // Reject if there are unexpected non-whitespace characters between matches.
        const between = text.slice(lastIndex, m.index);
        if (/[^\s]/.test(between)) return null;
        lastIndex = re.lastIndex;
        const v = m[0];
        if (v.startsWith('"')) {
            tokens.push({ value: v, kind: 'string' });
        } else if (v === '==' || v === '::') {
            tokens.push({ value: v, kind: 'op' });
        } else if (/^[A-Za-z_]/.test(v)) {
            // If this is `when`, look ahead for `{` (skipping whitespace) and
            // capture the body raw — anything goes inside.
            if (v === 'when') {
                let i = re.lastIndex;
                while (i < text.length && /\s/.test(text[i])) i++;
                if (text[i] === '{') {
                    const close = findMatchingBrace(text, i);
                    if (close < 0) return null;
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
    if (/[^\s]/.test(text.slice(lastIndex))) return null;
    return tokens;
}

// ---------------------------------------------------------------------------
// Parser
// ---------------------------------------------------------------------------

class PolicyParser {
    private pos = 0;
    readonly statements: PolicyStatement[] = [];
    readonly warnings: string[] = [];
    private failed = false;

    constructor(private readonly tokens: readonly Token[]) {}

    private peek(o = 0): Token | undefined {
        return this.tokens[this.pos + o];
    }
    private next(): Token | undefined {
        return this.tokens[this.pos++];
    }
    private fail(msg: string): null {
        this.warnings.push(msg);
        this.failed = true;
        return null;
    }

    parse(): ParsedPolicy | null {
        while (this.pos < this.tokens.length && !this.failed) {
            const t = this.peek();
            if (!t) break;
            // Tolerate stray semicolons between statements.
            if (t.value === ';') {
                this.next();
                continue;
            }
            if (t.kind === 'word' && (t.value === 'permit' || t.value === 'forbid')) {
                if (this.parseStatement() === null) return null;
                continue;
            }
            return this.fail(`Unexpected token '${t.value}' at top level`) as null;
        }
        if (this.failed) return null;
        return { statements: this.statements, warnings: this.warnings };
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

        // clauseList — any order, separated by ','. Tolerate empty body for
        // `permit ()`-style placeholders the visual editor produces.
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
            this.next(); // consume keyword

            // Slot-only form: bare `principal` / `action` / `resource` with no
            // operator means "match anything". The visual editor emits this
            // shape for clauses with zero chips, and the backend GAPL parser
            // accepts it. Detect it by peeking for the next clause separator.
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
                // Preserve the source namespace casing (e.g. 'Action' vs
                // 'action') so the round-trip serialiser can echo it back
                // unchanged — otherwise editing a stored policy in the
                // visual builder silently lowercases the namespace.
                actions = uids.map(u => ({ id: u.uid, kind: u.type, label: u.id }));
            } else {
                resources = uids.map(u => ({ id: u.uid, kind: u.type, label: u.id }));
            }

            if (this.peek()?.value === ',') this.next();
        }

        if (this.next()?.value !== ')') return this.fail("Expected ')' to close statement body");

        // Optional `when { ... }` block — capture body verbatim.
        let condition = '';
        if (this.peek()?.value === 'when') {
            this.next(); // consume 'when'
            const conditionResult = this.parseWhenBlock();
            if (conditionResult === null) return null;
            condition = conditionResult;
        }

        // Optional trailing semicolon.
        if (this.peek()?.value === ';') this.next();

        const stmt: PolicyStatement = {
            id: `stmt-${Math.random().toString(36).slice(2, 9)}`,
            effect,
            principals,
            actions,
            resources,
            condition,
        };
        this.statements.push(stmt);
        return stmt;
    }

    /** Parse a single `Type::"label"` token sequence. */
    private parseSingleUid(): Array<{ uid: string; type: string; id: string }> | null {
        const uid = this.consumeUid();
        if (uid === null) return null;
        return [uid];
    }

    /** Parse `[ uid (, uid)* ]`. Empty list is allowed and yields []. */
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

    /** Consume one `Type :: "id"` triplet. */
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

    /**
     * The tokeniser packed `when { ... }` into a fixed 4-token sequence:
     * `{`, `<when-body>`, `}`. We just unpack them here.
     */
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

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Parse a GAPL policy string into structured `PolicyStatement[]`.
 *
 * Returns `null` when the input contains GAPL features the visual editor
 * cannot represent. The caller must fall back to code-only editing.
 *
 * The return value's `statements` are roundtrip-equivalent to those that
 * `statementToGapl` would re-serialise — only statement ids differ (they
 * are regenerated since GAPL has no notion of a stable statement id).
 */
export function parseGaplToStatements(text: string): ParsedPolicy | null {
    if (!text || !text.trim()) return { statements: [], warnings: [] };

    const stripped = stripComments(text);
    const tokens = tokenise(stripped);
    if (tokens === null) return null;
    if (tokens.length === 0) return { statements: [], warnings: [] };

    const parser = new PolicyParser(tokens);
    return parser.parse();
}
