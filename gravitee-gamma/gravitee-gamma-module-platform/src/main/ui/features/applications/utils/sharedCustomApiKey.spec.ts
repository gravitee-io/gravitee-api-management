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
import {
    applicationApiKeyRenewBody,
    SHARED_CUSTOM_API_KEY_CHARACTER_ERROR,
    SHARED_CUSTOM_API_KEY_LENGTH_ERROR,
    validateSharedCustomApiKey,
} from './sharedCustomApiKey';

describe('validateSharedCustomApiKey', () => {
    it('accepts a blank value so the API generates a key', () => {
        expect(validateSharedCustomApiKey('')).toBeNull();
        expect(validateSharedCustomApiKey('   \t  ')).toBeNull();
    });

    it('rejects a value that is too short or too long', () => {
        expect(validateSharedCustomApiKey('e2e2345')).toBe(SHARED_CUSTOM_API_KEY_LENGTH_ERROR);
        expect(validateSharedCustomApiKey('e'.repeat(129))).toBe(SHARED_CUSTOM_API_KEY_LENGTH_ERROR);
        expect(validateSharedCustomApiKey('e'.repeat(8))).toBeNull();
        expect(validateSharedCustomApiKey('e'.repeat(128))).toBeNull();
    });

    it.each(['#', '%', '@', '/', ';', '=', '?', '|', '^', '~', ',', ' ', '\\'])('rejects a value containing %s', character => {
        expect(validateSharedCustomApiKey(`e2e${character}bad1`)).toBe(SHARED_CUSTOM_API_KEY_CHARACTER_ERROR);
    });
});

describe('applicationApiKeyRenewBody', () => {
    it('omits a blank value', () => {
        expect(applicationApiKeyRenewBody()).toEqual({});
        expect(applicationApiKeyRenewBody('')).toEqual({});
        expect(applicationApiKeyRenewBody('   ')).toEqual({});
    });

    it('sends a trimmed custom value', () => {
        expect(applicationApiKeyRenewBody('  custom-key-1  ')).toEqual({ customApiKey: 'custom-key-1' });
    });
});
