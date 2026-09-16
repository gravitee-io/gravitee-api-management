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
import { isForbiddenError } from './apiRequestError';

describe('isForbiddenError', () => {
    it('recognizes an error carrying a 403 status', () => {
        expect(isForbiddenError({ status: 403 })).toBe(true);
    });

    it('recognizes a 403 status carried as a string', () => {
        expect(isForbiddenError({ status: '403' })).toBe(true);
    });

    it('rejects an error carrying another status', () => {
        expect(isForbiddenError({ status: 500 })).toBe(false);
    });

    it('rejects an error carrying no status', () => {
        expect(isForbiddenError(new Error('Network request failed'))).toBe(false);
    });

    it.each([
        ['null', null],
        ['undefined', undefined],
        ['a string', 'Forbidden'],
        ['a number', 403],
    ])('rejects %s', (_case, error) => {
        expect(isForbiddenError(error)).toBe(false);
    });
});
