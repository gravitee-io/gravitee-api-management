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
import type { EngineSchemaJson } from '../api/authz-api.types';
import { engineSchemaToParsed } from '../engine-schema';

describe('engineSchemaToParsed', () => {
    it('returns empty collections for null/empty input', () => {
        expect(engineSchemaToParsed(null)).toEqual({ entities: [], actions: [] });
        expect(engineSchemaToParsed({})).toEqual({ entities: [], actions: [] });
    });

    it('keeps top-level (empty-namespace) type names unqualified', () => {
        const json: EngineSchemaJson = {
            '': {
                entityTypes: { User: {}, Document: {} },
                actions: { view: { appliesTo: { principalTypes: ['User'], resourceTypes: ['Document'] } } },
            },
        };
        const parsed = engineSchemaToParsed(json);
        expect(parsed.entities.map(e => e.name)).toEqual(['User', 'Document']);
        expect(parsed.actions[0]).toMatchObject({ name: 'view', principals: ['User'], resources: ['Document'] });
    });

    it('qualifies namespaced types as <namespace>::<Type>', () => {
        const json: EngineSchemaJson = {
            myapp: {
                entityTypes: { Subject: {}, Report: {} },
                actions: { read: { appliesTo: { principalTypes: ['Subject'], resourceTypes: ['Report'] } } },
            },
        };
        const parsed = engineSchemaToParsed(json);
        expect(parsed.entities.map(e => e.name)).toEqual(['myapp::Subject', 'myapp::Report']);
        expect(parsed.actions[0]).toMatchObject({
            name: 'read',
            principals: ['myapp::Subject'],
            resources: ['myapp::Report'],
        });
    });

    it('qualifies memberOfTypes (parents) within the same namespace', () => {
        const json: EngineSchemaJson = {
            myapp: { entityTypes: { Admin: { memberOfTypes: ['Group'] } } },
        };
        const parsed = engineSchemaToParsed(json);
        expect(parsed.entities[0]).toMatchObject({ name: 'myapp::Admin', parents: ['myapp::Group'] });
    });

    it('does not double-qualify names that already carry a namespace', () => {
        const json: EngineSchemaJson = {
            myapp: { entityTypes: { Admin: { memberOfTypes: ['other::Group'] } } },
        };
        expect(engineSchemaToParsed(json).entities[0].parents).toEqual(['other::Group']);
    });

    // Pure-mapper contract: given well-formed grouped JSON, each name is qualified by its group.
    it('handles multiple namespaces in one document, qualifying each by its own namespace', () => {
        const json: EngineSchemaJson = {
            '': { entityTypes: { User: {} } },
            billing: {
                entityTypes: { Invoice: {}, Payer: {} },
                actions: { pay: { appliesTo: { principalTypes: ['Payer'], resourceTypes: ['Invoice'] } } },
            },
        };
        const parsed = engineSchemaToParsed(json);
        // Global type stays bare; billing's types are qualified by their namespace.
        expect(parsed.entities.map(e => e.name)).toEqual(['User', 'billing::Invoice', 'billing::Payer']);
        expect(parsed.actions[0]).toMatchObject({ name: 'pay', principals: ['billing::Payer'], resources: ['billing::Invoice'] });
    });

    it('tolerates an action without appliesTo and a namespace without entityTypes', () => {
        const json: EngineSchemaJson = {
            myapp: { actions: { ping: {} } },
        };
        const parsed = engineSchemaToParsed(json);
        expect(parsed.entities).toEqual([]);
        expect(parsed.actions[0]).toMatchObject({ name: 'ping', principals: [], resources: [] });
    });

    it('maps attribute shapes to name/type pairs', () => {
        const json: EngineSchemaJson = {
            '': {
                entityTypes: {
                    Api: {
                        shape: {
                            type: 'Record',
                            attributes: {
                                owner: { type: 'String' },
                                tags: { type: 'Set', element: { type: 'String' } },
                                team: { type: 'Entity', name: 'Group' },
                            },
                        },
                    },
                },
            },
        };
        const parsed = engineSchemaToParsed(json);
        expect(parsed.entities[0].attributes).toEqual([
            { name: 'owner', type: 'String' },
            { name: 'tags', type: 'Set<String>' },
            { name: 'team', type: 'Group' },
        ]);
    });

    // Golden fixtures below are VERBATIM engine output (captured from AuthzSchemaParser +
    // SchemaJsonExporter, alpha.14), incl. the `context` Record the engine always emits — so we
    // test the mapper against the real shape, not a hand-trimmed one. The matching engine-side
    // behavior is pinned in AuthzSchemaParsingTest.

    it('keeps a top-level type global when it sits alongside a named namespace', () => {
        // `entity Subject {}; namespace myapp { entity Report {}; action "read"
        //  appliesTo { principal:[Subject], resource:[Report] }; }`
        // Subject is global (under ""); the action in myapp references it bare. The resolver must
        // see Subject is NOT myapp::Subject and keep it global, while Report stays myapp::Report.
        const engineOutput: EngineSchemaJson = {
            '': { entityTypes: { Subject: {} }, actions: {} },
            myapp: {
                entityTypes: { Report: {} },
                actions: {
                    read: {
                        appliesTo: {
                            principalTypes: ['Subject'],
                            resourceTypes: ['Report'],
                            context: { type: 'Record', attributes: {}, additionalAttributes: true },
                        },
                    },
                },
            },
        };
        const parsed = engineSchemaToParsed(engineOutput);
        expect(parsed.entities.map(e => e.name)).toEqual(['Subject', 'myapp::Report']);
        expect(parsed.actions[0]).toMatchObject({ name: 'read', principals: ['Subject'], resources: ['myapp::Report'] });
    });

    it('keeps a global type global across multiple namespaces (no drop, no mis-qualification)', () => {
        // `entity Glob {}; namespace a { entity X {}; action "ax" appliesTo { principal:[X],
        //  resource:[Glob] }; } namespace b { entity Y {}; }`
        const engineOutput: EngineSchemaJson = {
            '': { entityTypes: { Glob: {} }, actions: {} },
            a: {
                entityTypes: { X: {} },
                actions: {
                    ax: {
                        appliesTo: {
                            principalTypes: ['X'],
                            resourceTypes: ['Glob'],
                            context: { type: 'Record', attributes: {}, additionalAttributes: true },
                        },
                    },
                },
            },
            b: { entityTypes: { Y: {} }, actions: {} },
        };
        const parsed = engineSchemaToParsed(engineOutput);
        expect(parsed.entities.map(e => e.name).sort()).toEqual(['Glob', 'a::X', 'b::Y']);
        // Glob resolves to the global type, NOT a::Glob.
        expect(parsed.actions[0]).toMatchObject({ name: 'ax', principals: ['a::X'], resources: ['Glob'] });
    });

    it('resolves a bare reference local-first, then global, then falls back to local', () => {
        const json: EngineSchemaJson = {
            '': { entityTypes: { Shared: {} } },
            a: {
                entityTypes: { Local: {} },
                actions: {
                    act: { appliesTo: { principalTypes: ['Local'], resourceTypes: ['Shared', 'Ghost'] } },
                },
            },
        };
        const action = engineSchemaToParsed(json).actions[0];
        expect(action.principals).toEqual(['a::Local']); // local declared → qualified
        // 'Shared' → global declared → bare; 'Ghost' → undeclared → best-effort local 'a::Ghost'
        expect(action.resources).toEqual(['Shared', 'a::Ghost']);
    });
});
