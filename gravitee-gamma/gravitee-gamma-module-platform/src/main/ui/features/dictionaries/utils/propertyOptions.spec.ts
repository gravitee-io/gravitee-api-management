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

import { toPropertyRows, buildPropertyOptions } from './propertyOptions';
import type { Dictionary } from '../types/dictionary';

const dictionary = (overrides: Partial<Dictionary>): Dictionary => ({ id: 'd1', name: 'dict', type: 'MANUAL', ...overrides });

describe('toPropertyRows', () => {
    it('flags rows whose property option says encrypted', () => {
        const rows = toPropertyRows(
            dictionary({
                properties: { PLAIN: 'visible', SECRET: '••••••••••••' },
                propertyOptions: { SECRET: { encrypted: true } },
            }),
        );

        expect(rows).toEqual([
            { key: 'PLAIN', value: 'visible', encrypted: false },
            { key: 'SECRET', value: '••••••••••••', encrypted: true },
        ]);
    });

    it('returns no rows when the dictionary has no properties', () => {
        expect(toPropertyRows(dictionary({}))).toEqual([]);
    });
});

describe('buildPropertyOptions', () => {
    it('asks to encrypt only the requested keys', () => {
        expect(buildPropertyOptions(['A'], { A: '1', B: '2' })).toEqual({ A: { encryptable: true } });
    });

    it('ignores keys that are not in the submitted properties', () => {
        expect(buildPropertyOptions(['GONE'], { A: '1' })).toBeUndefined();
    });

    it('returns undefined when nothing is to be encrypted so the API keeps stored classifications', () => {
        expect(buildPropertyOptions([], { A: '1' })).toBeUndefined();
    });
});
