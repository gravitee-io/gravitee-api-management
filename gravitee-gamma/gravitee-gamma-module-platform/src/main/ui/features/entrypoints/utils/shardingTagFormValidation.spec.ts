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
    findDuplicateTagKey,
    findDuplicateTagName,
    getTagNameError,
    isTagNameValid,
    slugifyTagKeyBase,
    slugifyTagKeyFinal,
    TAG_NAME_MAX,
} from './shardingTagFormValidation';
import type { ShardingTagRow } from '../types/entrypoint';

const STUB_ROWS: ShardingTagRow[] = [
    {
        id: 'tag-1',
        key: 'prod',
        name: 'Production',
        description: '',
        restrictedGroupIds: [],
        restrictedGroupNames: [],
    },
    {
        id: 'tag-2',
        key: 'staging',
        name: 'Staging',
        description: '',
        restrictedGroupIds: [],
        restrictedGroupNames: [],
    },
];

describe('shardingTagFormValidation', () => {
    describe('slugifyTagKeyBase', () => {
        it('converts a simple name to a lowercase hyphenated key', () => {
            expect(slugifyTagKeyBase('My Tag Key')).toBe('my-tag-key');
        });

        it('strips diacritics and special characters', () => {
            expect(slugifyTagKeyBase('Tâg Spécîal @#$ Nàme!')).toBe('tag-special-name');
        });
    });

    describe('slugifyTagKeyFinal', () => {
        it('strips trailing hyphens from the base slug', () => {
            expect(slugifyTagKeyFinal('My Tag Key---')).toBe('my-tag-key');
            expect(slugifyTagKeyFinal('hello---')).toBe('hello');
        });

        it('applies full Classic slugify for complex names', () => {
            expect(slugifyTagKeyFinal('My Tag Key')).toBe('my-tag-key');
            expect(slugifyTagKeyFinal('Tâg Spécîal @#$ Nàme!')).toBe('tag-special-name');
        });
    });

    describe('getTagNameError', () => {
        it('returns null for empty or whitespace-only names', () => {
            expect(getTagNameError('')).toBeNull();
            expect(getTagNameError('   ')).toBeNull();
        });

        it('returns null for valid names within max length', () => {
            expect(getTagNameError('Production')).toBeNull();
        });

        it('returns an error when name exceeds max length', () => {
            const longName = 'a'.repeat(TAG_NAME_MAX + 1);
            expect(getTagNameError(longName)).toBe(`Name must be at most ${TAG_NAME_MAX} characters`);
        });
    });

    describe('isTagNameValid', () => {
        it('returns false for empty or whitespace-only names', () => {
            expect(isTagNameValid('')).toBe(false);
            expect(isTagNameValid('   ')).toBe(false);
        });

        it('returns true for non-empty trimmed names within max length', () => {
            expect(isTagNameValid('Production')).toBe(true);
            expect(isTagNameValid('  Production  ')).toBe(true);
        });

        it('returns false when name exceeds max length', () => {
            expect(isTagNameValid('a'.repeat(TAG_NAME_MAX + 1))).toBe(false);
        });
    });

    describe('findDuplicateTagName', () => {
        it('finds a duplicate by case-insensitive name match', () => {
            expect(findDuplicateTagName(STUB_ROWS, 'production')?.id).toBe('tag-1');
            expect(findDuplicateTagName(STUB_ROWS, '  PRODUCTION  ')?.id).toBe('tag-1');
        });

        it('excludes the current tag when editing', () => {
            expect(findDuplicateTagName(STUB_ROWS, 'Production', 'tag-1')).toBeUndefined();
        });

        it('returns undefined when no duplicate exists', () => {
            expect(findDuplicateTagName(STUB_ROWS, 'Development')).toBeUndefined();
        });
    });

    describe('findDuplicateTagKey', () => {
        it('finds a duplicate by exact key match', () => {
            expect(findDuplicateTagKey(STUB_ROWS, 'prod')?.id).toBe('tag-1');
        });

        it('excludes the current tag when editing', () => {
            expect(findDuplicateTagKey(STUB_ROWS, 'prod', 'tag-1')).toBeUndefined();
        });

        it('returns undefined for empty key or no match', () => {
            expect(findDuplicateTagKey(STUB_ROWS, '')).toBeUndefined();
            expect(findDuplicateTagKey(STUB_ROWS, 'dev')).toBeUndefined();
        });
    });
});
