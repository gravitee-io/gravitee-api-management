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

/** Same length and characters as the backend `@CustomApiKey` constraint. A blank value means generate. */
const CUSTOM_API_KEY_PATTERN = /^[^#%@/;=?|^~, \\]*$/;

export const SHARED_CUSTOM_API_KEY_LENGTH_ERROR = 'Should have length between 8 and 128 characters';
export const SHARED_CUSTOM_API_KEY_CHARACTER_ERROR = 'Should not contain: ^ # % @ \\ / ; = ? | ~ , (space)';

export function validateSharedCustomApiKey(raw: string): string | null {
    const value = raw.trim();
    if (value.length === 0) {
        return null;
    }
    if (value.length < 8 || value.length > 128) {
        return SHARED_CUSTOM_API_KEY_LENGTH_ERROR;
    }
    if (!CUSTOM_API_KEY_PATTERN.test(value)) {
        return SHARED_CUSTOM_API_KEY_CHARACTER_ERROR;
    }
    return null;
}

/** Empty and whitespace renewals omit the value so the API generates a key. */
export function applicationApiKeyRenewBody(customApiKey?: string): { customApiKey?: string } {
    const value = customApiKey?.trim();
    return value ? { customApiKey: value } : {};
}
