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
import { rowsFromAttrs, attrsFromRows } from '../attribute-rows';

describe('attribute-rows', () => {
    it('rowsFromAttrs seeds rows with inferred types, skipping description/_-keys', () => {
        const rows = rowsFromAttrs({ department: 'eng', clearance: 3, active: true, regions: ['us', 'eu'], description: 'x' });
        const byKey = Object.fromEntries(rows.map(r => [r.key, r]));
        expect(byKey.department).toMatchObject({ type: 'string', raw: 'eng' });
        expect(byKey.clearance).toMatchObject({ type: 'integer', raw: '3' });
        expect(byKey.active).toMatchObject({ type: 'boolean', raw: 'true' });
        expect(byKey.regions).toMatchObject({ type: 'set', raw: ['us', 'eu'] });
        expect(byKey.description).toBeUndefined();
    });

    it('attrsFromRows coerces and returns { attributes, error }', () => {
        const good = attrsFromRows([
            { id: 'a', key: 'clearance', type: 'integer', raw: '3' },
            { id: 'b', key: 'regions', type: 'set', raw: ['us', 'us'] },
        ]);
        expect(good.error).toBeNull();
        expect(good.attributes).toEqual({ clearance: 3, regions: ['us'] });

        const bad = attrsFromRows([{ id: 'a', key: 'clearance', type: 'integer', raw: 'x' }]);
        expect(bad.error).toMatch(/clearance/);
    });

    it('attrsFromRows rejects an invalid key', () => {
        const bad = attrsFromRows([{ id: 'a', key: '1bad', type: 'string', raw: 'x' }]);
        expect(bad.error).toMatch(/1bad|key/i);
    });

    it('attrsFromRows trims the key before storing it', () => {
        const res = attrsFromRows([{ id: 'a', key: '  dept  ', type: 'string', raw: 'eng' }]);
        expect(res.error).toBeNull();
        expect(res.attributes).toEqual({ dept: 'eng' });
        expect(' dept ' in res.attributes).toBe(false);
    });
});
