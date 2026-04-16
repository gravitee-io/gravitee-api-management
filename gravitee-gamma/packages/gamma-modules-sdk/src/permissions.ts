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
import type { ReactElement, ReactNode } from 'react';

/**
 * Permissions domain — types and runtime stubs for `@gravitee/gamma-modules-sdk`.
 *
 * Stubs are replaced at runtime by the host implementation via Module Federation
 * (`additionalShared`, singleton). Remotes bundle this file only for typing and
 * a minimal runtime shape.
 */

// ── Types ────────────────────────────────────────────────────────────────────

export type PermissionScope = 'organization' | 'environment' | 'api' | 'application';

export type PermissionCheck = { readonly anyOf: string[]; readonly allOf?: never } | { readonly allOf: string[]; readonly anyOf?: never };

export interface UserRole {
    id?: string;
    name?: string;
    scope?: 'API' | 'APPLICATION' | 'GROUP' | 'ENVIRONMENT' | 'ORGANIZATION' | 'PLATFORM';
    permissions?: Record<string, string[]>;
}

export type PermissionGateProps = PermissionCheck & {
    readonly children: ReactNode;
    readonly fallback?: ReactNode;
};

export type UseHasPermissionOptions = PermissionCheck;

// ── Runtime stubs (replaced by Module Federation singleton) ──────────────────

export class PermissionService {
    load(_scope: PermissionScope, _permissions: string[]): void {}
    clear(_scope: PermissionScope): void {}
    reset(): void {}
    getAllPermissions(): string[] {
        return [];
    }
    hasAnyOf(_required: string[] | undefined): boolean {
        return false;
    }
    hasAllOf(_required: string[] | undefined): boolean {
        return false;
    }
    subscribe(_listener: () => void): () => void {
        return () => {};
    }
    getSnapshot(): number {
        return 0;
    }
}

export const permissionService = new PermissionService();

export function useHasPermission(_options: UseHasPermissionOptions): boolean {
    return false;
}

export function PermissionGate(_props: PermissionGateProps): ReactElement | null {
    return null;
}

export function normalizeCrudMapRecord(_scope: PermissionScope, _record: Record<string, string[] | string>): string[] {
    return [];
}

export function normalizeOrganizationPermissionsFromRoles(_roles: UserRole[] | undefined): string[] {
    return [];
}
