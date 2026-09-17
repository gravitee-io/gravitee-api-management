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
    normalizeUserFieldKey,
    normalizeUserFieldValues,
    USER_FIELD_KEY_SUGGESTIONS,
    userFieldKeyError,
    userFieldLabelError,
} from './userFieldForm';

describe('userFieldKeyError', () => {
    it('requires a key', () => {
        expect(userFieldKeyError('')).toBe('Key is required.');
    });

    it('caps the key at 50 characters like the Classic dialog', () => {
        expect(userFieldKeyError('a'.repeat(50))).toBeNull();
        expect(userFieldKeyError('a'.repeat(51))).toBe('Key can not exceed 50 characters.');
    });

    it('accepts letters, digits, underscore and dash only', () => {
        expect(userFieldKeyError('Job_Position-2')).toBeNull();
        expect(userFieldKeyError('bad key!')).toBe('Only a-zA-Z0-9_- characters allowed');
        expect(userFieldKeyError('a.b')).toBe('Only a-zA-Z0-9_- characters allowed');
    });
});

describe('userFieldLabelError', () => {
    it('requires a non-blank label', () => {
        expect(userFieldLabelError('')).toBe('Label is required.');
        expect(userFieldLabelError('   ')).toBe('Label is required.');
    });

    it('caps the label at 50 characters', () => {
        expect(userFieldLabelError('a'.repeat(50))).toBeNull();
        expect(userFieldLabelError('a'.repeat(51))).toBe('Label length can not exceed 50 characters.');
    });
});

describe('normalizeUserFieldKey', () => {
    it('lowercases and trims, matching the backend sanitizer', () => {
        expect(normalizeUserFieldKey('  Job_Position ')).toBe('job_position');
    });
});

describe('normalizeUserFieldValues', () => {
    it('trims, drops blanks and de-duplicates while keeping order', () => {
        expect(normalizeUserFieldValues([' Engineering ', '', 'Product', 'Engineering', '  '])).toEqual(['Engineering', 'Product']);
    });
});

describe('USER_FIELD_KEY_SUGGESTIONS', () => {
    it('offers the Classic autocomplete list', () => {
        expect(USER_FIELD_KEY_SUGGESTIONS).toEqual([
            'address',
            'city',
            'country',
            'job_position',
            'organization',
            'telephone_number',
            'zip_code',
        ]);
    });
});
