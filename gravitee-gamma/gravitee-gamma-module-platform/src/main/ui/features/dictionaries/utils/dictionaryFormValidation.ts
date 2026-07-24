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

export const DICTIONARY_NAME_MIN = 3;
export const DICTIONARY_NAME_MAX = 50;

export const DEFAULT_JOLT_SPECIFICATION = `[
  {
    "operation": "shift",
    "spec": {
      "*": "&"
    }
  }
]`;

export function getDictionaryNameError(name: string): string | null {
    const trimmed = name.trim();
    if (!trimmed) return null;
    if (trimmed.length < DICTIONARY_NAME_MIN) return `Name must be at least ${DICTIONARY_NAME_MIN} characters`;
    if (trimmed.length > DICTIONARY_NAME_MAX) return `Name must be at most ${DICTIONARY_NAME_MAX} characters`;
    return null;
}

export function isDictionaryNameValid(name: string): boolean {
    const trimmed = name.trim();
    return trimmed.length >= DICTIONARY_NAME_MIN && trimmed.length <= DICTIONARY_NAME_MAX && getDictionaryNameError(name) === null;
}

/** Returns an error message for invalid URLs; null when empty or valid http(s). */
export function getHttpUrlError(url: string): string | null {
    const trimmed = url.trim();
    if (!trimmed) return null;
    try {
        const parsed = new URL(trimmed);
        if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
            return 'URL must start with http:// or https://';
        }
        return null;
    } catch {
        return 'Enter a valid URL';
    }
}

export function isHttpUrlValid(url: string): boolean {
    return url.trim().length > 0 && getHttpUrlError(url) === null;
}
