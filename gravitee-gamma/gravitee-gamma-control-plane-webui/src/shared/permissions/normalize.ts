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
import type { PermissionScope, UserRole } from './types';

/**
 * Normalizes management API permission maps to lowercase tokens:
 * `<scope>-<featureKey>-<c|r|u|d>` (aligned with Console GioPermissionService).
 */
export function normalizeCrudMapRecord(scope: PermissionScope, record: Record<string, string[] | string>): string[] {
    return Object.entries(record).flatMap(([key, crudValues]) => {
        const keyPart = key.toLowerCase();
        if (Array.isArray(crudValues)) {
            return crudValues.map(letter => `${scope}-${keyPart}-${letter.toLowerCase()}`);
        }
        return crudValues.split('').map(letter => `${scope}-${keyPart}-${letter.toLowerCase()}`);
    });
}

/**
 * Organization-scoped permissions from current user roles (same rules as Console).
 */
export function normalizeOrganizationPermissionsFromRoles(roles: UserRole[] | undefined): string[] {
    if (!roles?.length) {
        return [];
    }
    return roles
        .filter(role => role.scope === 'ORGANIZATION')
        .flatMap(role => Object.entries(role.permissions ?? {}))
        .flatMap(([key, crudValues]) =>
            (crudValues ?? []).map(crudValue => `organization-${key.toLowerCase()}-${crudValue.toLowerCase()}`),
        );
}
