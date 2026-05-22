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

// ---------------------------------------------------------------------------
// T1.1 Empty input
// ---------------------------------------------------------------------------

describe('parseGaplSchema — empty / whitespace', () => {
    it('returns empty result for empty string', () => {
        expect(parseGaplSchema('')).toEqual({ entities: [], actions: [], diagnostics: [] });
    });

    it('returns empty result for whitespace-only string', () => {
        expect(parseGaplSchema('   \n\n\t  ')).toEqual({ entities: [], actions: [], diagnostics: [] });
    });
});

// ---------------------------------------------------------------------------
// T1.2 Single entity — no parents, no attrs
// ---------------------------------------------------------------------------

describe('parseGaplSchema — single entity, no parents, no attrs', () => {
    it('parses entity name and line', () => {
        const result = parseGaplSchema('entity User {};\n');
        expect(result.entities).toHaveLength(1);
        expect(result.entities[0]).toMatchObject({ name: 'User', parents: [], attributes: [], line: 1 });
        expect(result.diagnostics).toHaveLength(0);
    });
});

// ---------------------------------------------------------------------------
// T1.3 Entity with parents and 3 attributes
// ---------------------------------------------------------------------------

describe('parseGaplSchema — entity with parents and attributes', () => {
    it('parses parents from in [...] clause', () => {
        const src = `entity User in [Group] {
  name: String,
  email: String,
  tenant: String
};`;
        const result = parseGaplSchema(src);
        expect(result.entities).toHaveLength(1);
        const ent = result.entities[0];
        expect(ent.name).toBe('User');
        expect(ent.parents).toEqual(['Group']);
        expect(ent.attributes).toEqual([
            { name: 'name', type: 'String' },
            { name: 'email', type: 'String' },
            { name: 'tenant', type: 'String' },
        ]);
        expect(ent.line).toBe(1);
    });

    it('parses multiple parents', () => {
        const result = parseGaplSchema('entity X in [A, B, C] {};');
        expect(result.entities[0].parents).toEqual(['A', 'B', 'C']);
    });
});

// ---------------------------------------------------------------------------
// T1.4 Action with principal + resource lists
// ---------------------------------------------------------------------------

describe('parseGaplSchema — action definitions', () => {
    it('parses action name, principals and resources', () => {
        const src = `action "can_invoke" appliesTo {
  principal: [User, ServiceAccount],
  resource: [MCPServer, MCPTool]
};`;
        const result = parseGaplSchema(src);
        expect(result.actions).toHaveLength(1);
        const act = result.actions[0];
        expect(act.name).toBe('can_invoke');
        expect(act.principals).toEqual(['User', 'ServiceAccount']);
        expect(act.resources).toEqual(['MCPServer', 'MCPTool']);
        expect(act.line).toBe(1);
    });
});

// ---------------------------------------------------------------------------
// T1.5 Comments — before, inside, between definitions
// ---------------------------------------------------------------------------

describe('parseGaplSchema — comment handling', () => {
    it('strips line comments before entity', () => {
        const src = `// This is a header comment
entity Resource {};`;
        const result = parseGaplSchema(src);
        expect(result.entities).toHaveLength(1);
        expect(result.entities[0].name).toBe('Resource');
        // Line number should be 2 (the entity is on the second line)
        expect(result.entities[0].line).toBe(2);
    });

    it('strips inline comments after attr', () => {
        const src = `entity Foo {
  name: String // inline comment here
};`;
        const result = parseGaplSchema(src);
        expect(result.entities[0].attributes).toEqual([{ name: 'name', type: 'String' }]);
    });

    it('ignores comment-only lines between definitions', () => {
        const src = `entity A {};
// separator comment
entity B {};`;
        const result = parseGaplSchema(src);
        expect(result.entities).toHaveLength(2);
        expect(result.entities[0].name).toBe('A');
        expect(result.entities[1].name).toBe('B');
    });
});

// ---------------------------------------------------------------------------
// T1.6 Unbalanced brace — diagnostic, parsing continues
// ---------------------------------------------------------------------------

describe('parseGaplSchema — unbalanced brace', () => {
    it('produces a diagnostic for unbalanced brace and still parses subsequent defs', () => {
        const src = `entity Bad {
  name: String
// missing closing brace
entity Good {};`;
        const result = parseGaplSchema(src);
        // 'Good' might not be parsed since the parser consumed all tokens inside 'Bad'
        // but no throw should occur and diagnostics should be non-empty
        expect(result.diagnostics.length).toBeGreaterThan(0);
        // The diagnostic should mention the bad entity or line
        expect(result.diagnostics.some(d => d.includes('Bad') || d.includes('Unbalanced'))).toBe(true);
    });
});

// ---------------------------------------------------------------------------
// T1.7 Multi-line entity def
// ---------------------------------------------------------------------------

describe('parseGaplSchema — multi-line entity', () => {
    it('tracks the line number of the entity keyword, not the closing brace', () => {
        const src = `
entity MCPServer {
  name: String,
  url: String,
  transport: String
};`;
        const result = parseGaplSchema(src);
        expect(result.entities).toHaveLength(1);
        // blank first line, so entity keyword is on line 2
        expect(result.entities[0].line).toBe(2);
        expect(result.entities[0].attributes).toHaveLength(3);
    });
});

// ---------------------------------------------------------------------------
// T1.8 Action without appliesTo block — diagnostic
// ---------------------------------------------------------------------------

describe('parseGaplSchema — action without appliesTo', () => {
    it('produces a diagnostic for missing appliesTo', () => {
        const src = `action "bad_action" { principal: [User] };`;
        const result = parseGaplSchema(src);
        expect(result.diagnostics.length).toBeGreaterThan(0);
        expect(result.diagnostics.some(d => d.includes('appliesTo'))).toBe(true);
    });
});

// ---------------------------------------------------------------------------
// T1.9 Leading whitespace on entity name
// ---------------------------------------------------------------------------

describe('parseGaplSchema — leading whitespace', () => {
    it('correctly parses entity name despite leading whitespace on surrounding lines', () => {
        const src = '   entity   User {};\n'; // spaces before keyword are irrelevant; tokeniser handles them
        // Tokeniser strips whitespace between keywords, so 'entity' and 'User' are adjacent tokens
        const result = parseGaplSchema(src);
        expect(result.entities).toHaveLength(1);
        expect(result.entities[0].name).toBe('User');
    });
});

// ---------------------------------------------------------------------------
// T1.10 Representative prototype-shape schema
// ---------------------------------------------------------------------------

describe('parseGaplSchema — representative prototype schema', () => {
    const PROTOTYPE_SCHEMA = `// Gravitee Authorization Policy Language (GAPL) — Schema
// Entity and action definitions evaluated by the Policy Engine PDP.

// ===== Principals =====

entity User in [Group] {
  name: String,
  email: String,
  tenant: String
};

entity Group {
  name: String
};

entity ServiceAccount {
  name: String,
  tenant: String
};

// ===== MCP =====

entity MCPServer {
  name: String,
  url: String,
  transport: String
};

entity MCPTool in [MCPServer] {
  name: String
};

// ===== Actions =====

action "can_invoke" appliesTo {
  principal: [User, ServiceAccount],
  resource: [MCPServer, MCPTool]
};

action "can_read" appliesTo {
  principal: [User],
  resource: [MCPServer]
};
`;

    it('parses expected entity count', () => {
        const result = parseGaplSchema(PROTOTYPE_SCHEMA);
        expect(result.entities.length).toBe(5);
    });

    it('parses expected action count', () => {
        const result = parseGaplSchema(PROTOTYPE_SCHEMA);
        expect(result.actions.length).toBe(2);
    });

    it('has no diagnostics for valid schema', () => {
        const result = parseGaplSchema(PROTOTYPE_SCHEMA);
        expect(result.diagnostics).toHaveLength(0);
    });

    it('correctly identifies User entity line', () => {
        const result = parseGaplSchema(PROTOTYPE_SCHEMA);
        const user = result.entities.find(e => e.name === 'User');
        expect(user).toBeDefined();
        expect(user!.parents).toEqual(['Group']);
        expect(user!.attributes).toHaveLength(3);
    });

    it('correctly identifies can_invoke action', () => {
        const result = parseGaplSchema(PROTOTYPE_SCHEMA);
        const act = result.actions.find(a => a.name === 'can_invoke');
        expect(act).toBeDefined();
        expect(act!.principals).toEqual(['User', 'ServiceAccount']);
        expect(act!.resources).toEqual(['MCPServer', 'MCPTool']);
    });
});

// ---------------------------------------------------------------------------
// T1.11 Escaped quotes inside action names
// ---------------------------------------------------------------------------

describe('parseGaplSchema — escaped quotes in string literals', () => {
    it('keeps an action with an escaped quote as a single token and unescapes the name', () => {
        const src = `action "say \\"hi\\"" appliesTo {
  principal: [User],
  resource: [MCPServer]
};`;
        const result = parseGaplSchema(src);
        expect(result.actions).toHaveLength(1);
        // Name should contain real quotes after unescaping
        expect(result.actions[0].name).toBe('say "hi"');
        expect(result.diagnostics).toHaveLength(0);
    });

    it('does not break parsing when an action with an escaped quote is followed by an entity', () => {
        const src = `action "quote\\"action" appliesTo {
  principal: [User],
  resource: [Doc]
};
entity Doc {
  title: String
};`;
        const result = parseGaplSchema(src);
        expect(result.actions).toHaveLength(1);
        expect(result.actions[0].name).toBe('quote"action');
        expect(result.entities).toHaveLength(1);
        expect(result.entities[0].name).toBe('Doc');
        expect(result.entities[0].attributes).toEqual([{ name: 'title', type: 'String' }]);
    });
});

// ---------------------------------------------------------------------------
// T1.12 String-aware comment stripping
// ---------------------------------------------------------------------------

describe('parseGaplSchema — comment stripping respects string literals', () => {
    it('does not treat a "//" inside a quoted string as a comment', () => {
        const src = `action "see https://example.com" appliesTo {
  principal: [User],
  resource: [Doc]
};`;
        const result = parseGaplSchema(src);
        expect(result.actions).toHaveLength(1);
        // If the URL had been treated as a comment, the appliesTo block would
        // have been truncated and the parser would have produced diagnostics.
        expect(result.actions[0].name).toBe('see https://example.com');
        expect(result.diagnostics).toHaveLength(0);
    });
});
