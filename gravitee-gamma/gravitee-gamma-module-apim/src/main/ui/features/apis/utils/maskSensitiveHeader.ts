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

/** Classic `MaskSensitiveHeaderPipe`. */

const PARTIALLY_MASKED_HEADERS = new Set(['authorization', 'proxy-authorization', 'x-api-key', 'x-gravitee-api-key']);
const FULLY_MASKED_HEADERS = new Set(['cookie', 'set-cookie']);
const KNOWN_SCHEMES = new Set(['bearer', 'basic', 'digest', 'apikey', 'negotiate']);
const DECODABLE_SCHEMES = new Set(['basic']);
const MASK = '••••••••';
const MIN_LENGTH_FOR_SUFFIX = 20;
const SUFFIX_LENGTH = 4;
const SCHEME_SEPARATOR = /^(\S+)\s+(\S.*)$/;
const PADDED_BASE64 = /^[A-Za-z0-9+/]+={1,2}$/;

export function maskSensitiveHeader(value: string | null | undefined, headerName: string): string | null | undefined {
    if (!value) return value;

    const normalizedName = headerName?.toLowerCase();
    if (FULLY_MASKED_HEADERS.has(normalizedName)) return MASK;
    if (!PARTIALLY_MASKED_HEADERS.has(normalizedName)) return value;

    const [, scheme, credentials] = SCHEME_SEPARATOR.exec(value) ?? [];
    const normalizedScheme = scheme?.toLowerCase();
    const hasKnownScheme = !!normalizedScheme && KNOWN_SCHEMES.has(normalizedScheme);
    const prefix = hasKnownScheme ? `${scheme} ` : '';
    const secret = hasKnownScheme ? credentials : value;
    const isDecodable = hasKnownScheme ? DECODABLE_SCHEMES.has(normalizedScheme) : PADDED_BASE64.test(secret);
    const suffix = !isDecodable && secret.length > MIN_LENGTH_FOR_SUFFIX ? secret.slice(-SUFFIX_LENGTH) : '';
    return `${prefix}${MASK}${suffix}`;
}
