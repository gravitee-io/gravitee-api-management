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

/** What the server sends in place of a stored secret, and restores when it gets it back unchanged. */
export const MASKED_SECRET = '********';

const ADDRESS_FIELD = /^(repository|.*[uU]rl)$/;

/**
 * Whether two configurations of the same fetcher point at the same address. The server only restores a masked secret
 * for the same address, so that changing it never sends the stored credentials somewhere else. This mirrors its rule
 * (`FetcherConfigurationAddress.sameAddress`): the repository and every field ending in `url` or `Url` must match.
 */
export function sameSourceAddress(stored: Record<string, unknown>, edited: Record<string, unknown>): boolean {
    const addressFields = new Set([...Object.keys(stored), ...Object.keys(edited)].filter(isAddressField));
    return [...addressFields].every(field => (stored[field] ?? null) === (edited[field] ?? null));
}

/** Whether a setting is part of the address a masked secret is kept for: the repository, or a field ending in `url` or `Url`. */
export function isAddressField(field: string): boolean {
    return ADDRESS_FIELD.test(field);
}

export function maskedSecretKeys(configuration: Record<string, unknown>): string[] {
    return Object.keys(configuration).filter(key => configuration[key] === MASKED_SECRET);
}
