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
import { PATH_MAPPING_PATTERN, pathMappingValidationError, replacePathMapping, samePathMappings, sortedPathMappings } from './pathMappings';

describe('pathMappings', () => {
    it('accepts form-rule values', () => {
        expect(PATH_MAPPING_PATTERN.test('/charges/:chargeId')).toBe(true);
        expect(PATH_MAPPING_PATTERN.test('/')).toBe(false);
        expect(PATH_MAPPING_PATTERN.test('/users/@me')).toBe(false);
        expect(PATH_MAPPING_PATTERN.test('/a b')).toBe(false);
    });

    it('requires a non-blank value', () => {
        expect(pathMappingValidationError('  ', [])).toBe('Path mapping is required.');
    });

    it('rejects a new value outside the form rule', () => {
        expect(pathMappingValidationError('/users/@me', [])).toMatch(/must match/);
    });

    it('rejects an unchanged stored value that is outside the form rule', () => {
        expect(pathMappingValidationError('/users/@me', ['/users/@me'], '/users/@me')).toMatch(/must match/);
    });

    it('rejects duplicates', () => {
        expect(pathMappingValidationError('/orders', ['/orders'])).toBe('Path mapping already exists.');
        expect(pathMappingValidationError('/orders', ['/orders'], '/orders')).toBeNull();
    });

    it('sorts and compares mappings', () => {
        expect(sortedPathMappings(['/b', '/a'])).toEqual(['/a', '/b']);
        expect(samePathMappings(['/a'], ['/a'])).toBe(true);
        expect(samePathMappings(['/a'], ['/b'])).toBe(false);
        expect(replacePathMapping(['/a', '/b'], '/a', '/c')).toEqual(['/c', '/b']);
    });
});
