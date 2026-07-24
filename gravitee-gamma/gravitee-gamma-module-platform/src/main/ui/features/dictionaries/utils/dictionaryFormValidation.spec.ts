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

import { getDictionaryNameError, getHttpUrlError, isDictionaryNameValid, isHttpUrlValid } from './dictionaryFormValidation';

describe('dictionaryFormValidation', () => {
    describe('name', () => {
        it('accepts names between 3 and 50 characters', () => {
            expect(isDictionaryNameValid('abc')).toBe(true);
            expect(getDictionaryNameError('ab')).toBe('Name must be at least 3 characters');
            expect(isDictionaryNameValid('ab')).toBe(false);
        });
    });

    describe('http url', () => {
        it('requires a valid http(s) URL', () => {
            expect(getHttpUrlError('')).toBeNull();
            expect(getHttpUrlError('not-a-url')).toBe('Enter a valid URL');
            expect(getHttpUrlError('ftp://example.com')).toBe('URL must start with http:// or https://');
            expect(getHttpUrlError('https://service.internal/dictionary')).toBeNull();
            expect(isHttpUrlValid('https://service.internal/dictionary')).toBe(true);
            expect(isHttpUrlValid('')).toBe(false);
        });
    });
});
