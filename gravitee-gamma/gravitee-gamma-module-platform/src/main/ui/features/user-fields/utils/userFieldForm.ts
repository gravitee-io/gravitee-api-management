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

export const USER_FIELD_KEY_MAX_LENGTH = 50;
export const USER_FIELD_LABEL_MAX_LENGTH = 50;

/** Classic custom-user-fields-dialog.component.ts's key pattern; the backend lowercases before storing. */
const USER_FIELD_KEY_PATTERN = /^[a-zA-Z0-9_-]+$/;

/** Classic's autocomplete suggestions for the key input. */
export const USER_FIELD_KEY_SUGGESTIONS: readonly string[] = [
    'address',
    'city',
    'country',
    'job_position',
    'organization',
    'telephone_number',
    'zip_code',
];

export function userFieldKeyError(key: string): string | null {
    if (key === '') return 'Key is required.';
    if (key.length > USER_FIELD_KEY_MAX_LENGTH) return `Key can not exceed ${USER_FIELD_KEY_MAX_LENGTH} characters.`;
    if (!USER_FIELD_KEY_PATTERN.test(key)) return 'Only a-zA-Z0-9_- characters allowed';
    return null;
}

export function userFieldLabelError(label: string): string | null {
    const trimmed = label.trim();
    if (trimmed === '') return 'Label is required.';
    if (trimmed.length > USER_FIELD_LABEL_MAX_LENGTH) return `Label length can not exceed ${USER_FIELD_LABEL_MAX_LENGTH} characters.`;
    return null;
}

/**
 * The backend stores the key lowercased (`CustomFieldSanitizer.formatKeyValue`). Sending it lowercased
 * keeps the row the user sees right after creation identical to what the server keeps.
 */
export function normalizeUserFieldKey(key: string): string {
    return key.trim().toLowerCase();
}

/** Trims and de-duplicates, matching the backend's `distinct()` on write. */
export function normalizeUserFieldValues(values: readonly string[]): string[] {
    const seen = new Set<string>();
    const unique: string[] = [];
    for (const value of values) {
        const trimmed = value.trim();
        if (trimmed === '' || seen.has(trimmed)) continue;
        seen.add(trimmed);
        unique.push(trimmed);
    }
    return unique;
}
