/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Pipe, PipeTransform } from '@angular/core';

/** Headers whose value is masked but whose authentication scheme and trailing characters stay readable. */
const PARTIALLY_MASKED_HEADERS = new Set(['authorization', 'proxy-authorization', 'x-api-key', 'x-gravitee-api-key']);

/** Headers masked entirely: they hold several name=value pairs, a partial reveal would be both unreadable and leaky. */
const FULLY_MASKED_HEADERS = new Set(['cookie', 'set-cookie']);

/** Schemes are not secret and are valuable when diagnosing a 401 (a Basic sent where a Bearer is expected). */
const KNOWN_SCHEMES = new Set(['bearer', 'basic', 'digest', 'apikey', 'negotiate']);

/** Fixed width: a mask sized on the real value would disclose the secret length. */
const MASK = '••••••••';

/** At or below this length, revealing the last characters would expose too large a share of the secret. */
const MIN_LENGTH_FOR_SUFFIX = 20;
const SUFFIX_LENGTH = 4;

const SCHEME_SEPARATOR = /^(\S+)\s+(\S.*)$/;

/**
 * Basic encodes `user:password` in base64, so any revealed character decodes to plaintext. This holds
 * whatever the encoding looks like: a credential whose byte length is a multiple of three carries no
 * padding at all, and its trailing characters then decode to the exact tail of the password.
 */
const DECODABLE_SCHEMES = new Set(['basic']);

/** Same reasoning for a value carrying no scheme: base64 padding is the only hint that it is decodable. */
const PADDED_BASE64 = /^[A-Za-z0-9+/]+={1,2}$/;

@Pipe({ name: 'maskSensitiveHeader' })
export class MaskSensitiveHeaderPipe implements PipeTransform {
  transform(value: string | null | undefined, headerName: string): string | null | undefined {
    if (!value) {
      return value;
    }

    const normalizedName = headerName?.toLowerCase();

    if (FULLY_MASKED_HEADERS.has(normalizedName)) {
      return MASK;
    }

    if (!PARTIALLY_MASKED_HEADERS.has(normalizedName)) {
      return value;
    }

    const [, scheme, credentials] = SCHEME_SEPARATOR.exec(value) ?? [];
    const hasKnownScheme = scheme && KNOWN_SCHEMES.has(scheme.toLowerCase());

    const prefix = hasKnownScheme ? `${scheme} ` : '';
    const secret = hasKnownScheme ? credentials : value;

    const isDecodable = hasKnownScheme ? DECODABLE_SCHEMES.has(scheme.toLowerCase()) : PADDED_BASE64.test(secret);
    const suffix = !isDecodable && secret.length > MIN_LENGTH_FOR_SUFFIX ? secret.slice(-SUFFIX_LENGTH) : '';

    return `${prefix}${MASK}${suffix}`;
  }
}
