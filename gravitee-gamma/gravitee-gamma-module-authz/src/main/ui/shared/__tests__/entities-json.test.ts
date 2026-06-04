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
import { buildEntitiesJson } from '../entities-json';
import type { EntityInstance } from '../entity.types';

const alice: EntityInstance = { uid: { type: 'User', id: 'alice' }, attrs: { dept: 'eng' }, parents: [], source: 'local' };
const mcp: EntityInstance = {
    uid: { type: 'MCPServer', id: 'flight' },
    attrs: { url: 'https://x' },
    parents: [{ type: 'Group', id: 'team' }],
    source: 'gravitee-catalog',
};

describe('buildEntitiesJson', () => {
    it('exports a pretty JSON array of canonical GAPL shapes', () => {
        const json = buildEntitiesJson([alice, mcp]);
        expect(JSON.parse(json)).toEqual([
            { uid: { type: 'User', id: 'user.alice' }, attrs: { dept: 'eng' }, parents: [] },
            { uid: { type: 'MCPServer', id: 'mcp.flight' }, attrs: { url: 'https://x' }, parents: ['group.team'] },
        ]);
    });

    it('is pretty-printed', () => {
        expect(buildEntitiesJson([alice])).toContain('\n');
    });

    it('returns an empty array for no entities', () => {
        expect(JSON.parse(buildEntitiesJson([]))).toEqual([]);
    });
});
