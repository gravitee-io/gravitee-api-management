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

import type { Dictionary, DictionaryPropertyOptions, DictionaryPropertyRow } from '../types/dictionary';

export function toPropertyRows(dictionary: Dictionary, markedKeys: ReadonlySet<string>): DictionaryPropertyRow[] {
    return Object.entries(dictionary.properties ?? {}).map(([key, value]) => {
        const encrypted = dictionary.propertyOptions?.[key]?.encrypted === true;
        return { key, value, encrypted, encryptOnSave: !encrypted && markedKeys.has(key) };
    });
}

/**
 * Only `encryptable: true` is ever written, and only for keys that are submitted, because the API rejects options for an
 * unknown key and an omitted key keeps the classification it already has.
 */
export function buildPropertyOptions(
    keysToEncrypt: readonly string[],
    properties: Record<string, string>,
): Record<string, DictionaryPropertyOptions> | undefined {
    const entries = keysToEncrypt.filter(key => Object.hasOwn(properties, key)).map(key => [key, { encryptable: true }] as const);
    return entries.length > 0 ? Object.fromEntries(entries) : undefined;
}
