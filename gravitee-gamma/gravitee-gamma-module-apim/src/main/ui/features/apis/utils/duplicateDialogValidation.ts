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

/** Matches legacy console `HOST_PATTERN_REGEX` (RFC 1123 hostname labels). */
export const DUPLICATE_HOST_PATTERN = /^([a-z0-9]|[a-z0-9][a-z0-9\-_]{0,61}[a-z0-9])(\.([a-z0-9]|[a-z0-9][a-z0-9\-_]{0,61}[a-z0-9]))*$/;

export function validateDuplicateHost(value: string): string | null {
    const host = value.trim();
    if (!host) return 'Host is required.';
    if (host.length > 255) return 'Max length is 255 characters';
    if (!DUPLICATE_HOST_PATTERN.test(host)) return 'Host is not valid';
    return null;
}

export function validateDuplicateVersion(value: string): string | null {
    if (!value.trim()) return 'Version is required.';
    return null;
}
