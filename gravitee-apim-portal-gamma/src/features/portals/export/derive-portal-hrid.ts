/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
const HRID_PATTERN = /^[a-zA-Z0-9][a-zA-Z0-9_-]+[a-zA-Z0-9]$/;

function slugifyName(name: string): string {
    return name
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
}

function padHrid(candidate: string): string {
    let hrid = candidate.replace(/-+/g, '-');
    if (!/^[a-zA-Z0-9]/.test(hrid)) {
        hrid = `p-${hrid}`;
    }
    if (!/[a-zA-Z0-9]$/.test(hrid)) {
        hrid = `${hrid}-p`;
    }
    return hrid.slice(0, 64);
}

export function derivePortalHrid(portal: { readonly id: string; readonly name: string }): string {
    if (HRID_PATTERN.test(portal.id)) {
        return portal.id;
    }

    const fromName = padHrid(slugifyName(portal.name) || 'portal');
    if (HRID_PATTERN.test(fromName)) {
        return fromName;
    }

    return padHrid(`${fromName}-portal`);
}

export function deriveResourceName(value: string): string {
    return value
        .toLowerCase()
        .replace(/[^a-z0-9-]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 63);
}
