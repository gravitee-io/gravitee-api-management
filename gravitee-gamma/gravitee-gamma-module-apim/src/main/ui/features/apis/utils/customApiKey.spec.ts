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
import { customApiKeyFormatError } from './customApiKey';

describe('customApiKeyFormatError', () => {
    it('accepts an empty key so the server can generate one', () => {
        expect(customApiKeyFormatError('')).toBeNull();
    });

    it('accepts a key of 8 to 128 characters without the classic forbidden characters', () => {
        expect(customApiKeyFormatError('validkey')).toBeNull();
        expect(customApiKeyFormatError('a'.repeat(8))).toBeNull();
        expect(customApiKeyFormatError('a'.repeat(128))).toBeNull();
        expect(customApiKeyFormatError('ok-key_1.2+3')).toBeNull();
    });

    it('rejects keys shorter than 8 or longer than 128', () => {
        expect(customApiKeyFormatError('short')).toBe('Must be longer than 8 characters');
        expect(customApiKeyFormatError('a'.repeat(7))).toBe('Must be longer than 8 characters');
        expect(customApiKeyFormatError('a'.repeat(129))).toBe('Must be shorter than 128 characters');
    });

    it('rejects the characters classic forbids, before the length message', () => {
        for (const character of ['#', '%', '@', '\\', ';', '=', '?', '|', '^', '~', ',', ' ']) {
            expect(customApiKeyFormatError(`bad${character}key`)).toBe('No special characters allowed');
        }
        expect(customApiKeyFormatError('a#')).toBe('No special characters allowed');
    });
});
