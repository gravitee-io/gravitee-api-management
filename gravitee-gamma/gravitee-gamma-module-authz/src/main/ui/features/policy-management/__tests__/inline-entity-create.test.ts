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
import { defaultResourceCanonical, makeInlineEntityCreator, slugify } from '../inline-entity-create';

describe('slugify', () => {
    it('kebab-cases and strips accents', () => {
        expect(slugify('Alice Doe')).toBe('alice-doe');
        expect(slugify('Crème Brûlée')).toBe('creme-brulee');
    });

    it('trims leading/trailing separators', () => {
        expect(slugify('  --Flight API--  ')).toBe('flight-api');
    });
});

describe('defaultResourceCanonical', () => {
    it('maps policy types to their canonical resource prefix', () => {
        expect(defaultResourceCanonical('MCP')).toBe('mcp');
        expect(defaultResourceCanonical('MODEL')).toBe('model');
        expect(defaultResourceCanonical('AGENT')).toBe('agent');
        expect(defaultResourceCanonical('API')).toBe('api');
        expect(defaultResourceCanonical('EVENT')).toBe('event');
    });

    it('falls back to "resource" for custom/unknown types', () => {
        expect(defaultResourceCanonical('CUSTOM')).toBe('resource');
    });
});

describe('makeInlineEntityCreator — PRINCIPAL', () => {
    it('builds a chip option in the principal option id format without persisting', () => {
        const create = makeInlineEntityCreator('PRINCIPAL');
        const option = create({ canonicalPrefix: 'user', slug: 'alice-doe', displayName: 'Alice Doe' });

        // id must match useEntityOptions.toChipOption (`UiType::"slug"`) so a known option reuses it
        // and an unknown one renders as a ghost chip via the same GAPL uid pattern.
        expect(option).toEqual({ id: 'User::"alice-doe"', label: 'alice-doe', group: 'User' });
    });

    it('maps the agent-identity canonical prefix to the AgentIdentity ui type', () => {
        const create = makeInlineEntityCreator('PRINCIPAL');
        const option = create({ canonicalPrefix: 'agent-identity', slug: 'bot', displayName: 'Bot' });

        expect(option.id).toBe('AgentIdentity::"bot"');
        expect(option.group).toBe('AgentIdentity');
    });
});

describe('makeInlineEntityCreator — RESOURCE', () => {
    it('builds a chip option in the service resource option id format', () => {
        const create = makeInlineEntityCreator('RESOURCE');
        const option = create({ canonicalPrefix: 'mcp', slug: 'flight-api', displayName: 'Flight API' });

        // id must match ServicePolicyPage.serviceResourceOptions (`Group::"slug"`); label keeps the display name.
        expect(option).toEqual({ id: 'MCP::"flight-api"', label: 'Flight API', group: 'MCP' });
    });

    it('maps model/agent/api/unknown prefixes to their resource groups', () => {
        const create = makeInlineEntityCreator('RESOURCE');
        const groupFor = (canonicalPrefix: string) => create({ canonicalPrefix, slug: 's', displayName: 'S' }).group;

        expect(groupFor('model')).toBe('Model');
        expect(groupFor('agent')).toBe('Agent');
        expect(groupFor('api')).toBe('API');
        expect(groupFor('webhook')).toBe('Resource');
    });
});
