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
import type { CurrentUser } from './auth.types';

function asRecord(raw: unknown): Record<string, unknown> {
    return raw !== null && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};
}

function pickString(...values: unknown[]): string {
    for (const value of values) {
        if (typeof value === 'string' && value.trim() !== '') {
            return value;
        }
    }
    for (const value of values) {
        if (typeof value === 'string') {
            return value;
        }
    }
    return '';
}

function pickBool(...values: unknown[]): boolean {
    return values.some(value => value === true);
}

/**
 * Maps GET /user (`UserDetails`) and PUT /user (`UserEntity`) onto CurrentUser.
 *
 * `CurrentUserResource#getCurrentUser` serializes `UserDetails` with camelCase
 * `primaryOwner` (and Jackson may also emit `isPrimaryOwner`).
 * `CurrentUserResource#updateCurrentUser` serializes `UserEntity` as `primary_owner`
 * and has no `groupsByEnvironment` field — callers that keep a session after PUT
 * must merge with {@link retainIdentityFields}.
 */
export function normalizeCurrentUser(raw: unknown): CurrentUser {
    const record = asRecord(raw);
    const firstname = pickString(record.firstname);
    const lastname = pickString(record.lastname);
    const email = pickString(record.email);
    const sourceId = pickString(record.sourceId, record.id);
    const displayName = pickString(record.displayName) || [firstname, lastname].filter(Boolean).join(' ') || sourceId;

    return {
        id: pickString(record.id) || undefined,
        displayName,
        email: email || undefined,
        firstname,
        lastname,
        source: pickString(record.source) || undefined,
        primaryOwner: pickBool(record.primaryOwner, record.isPrimaryOwner, record.primary_owner),
        roles: Array.isArray(record.roles) ? (record.roles as CurrentUser['roles']) : undefined,
        groupsByEnvironment:
            record.groupsByEnvironment && typeof record.groupsByEnvironment === 'object'
                ? (record.groupsByEnvironment as CurrentUser['groupsByEnvironment'])
                : undefined,
        customFields:
            record.customFields && typeof record.customFields === 'object'
                ? (record.customFields as CurrentUser['customFields'])
                : undefined,
    };
}

/** PUT /user returns UserEntity, which omits session fields GET /user (UserDetails) includes. */
export function retainIdentityFields(previous: CurrentUser, next: CurrentUser): CurrentUser {
    return {
        ...next,
        id: next.id || previous.id,
        source: next.source || previous.source,
        primaryOwner: Boolean(next.primaryOwner || previous.primaryOwner),
        roles: next.roles ?? previous.roles,
        groupsByEnvironment: next.groupsByEnvironment ?? previous.groupsByEnvironment,
    };
}
