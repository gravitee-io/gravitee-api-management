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

/**
 * Test stub for `@gravitee/gamma-modules-sdk`.
 * Replaced at runtime by the MF singleton from the host shell.
 * Individual tests mock specific exports via jest.mock().
 *
 * Permission stubs default to granted so feature tests render normally.
 * To test denied states, mock `useHasPermission` or `permissionService` per-test.
 */
import type { ReactNode } from 'react';

export const useEnvironment = (): undefined => undefined;

// ─── Permissions ──────────────────────────────────────────────────────────────

export const useHasPermission = (_check?: { anyOf?: string[]; allOf?: string[] }): boolean => true;

export const permissionService = {
    load: (_scope: string, _permissions: string[]): void => {},
    clear: (_scope: string): void => {},
    reset: (): void => {},
    getAllPermissions: (): string[] => [],
    hasAnyOf: (): boolean => true,
    hasAllOf: (): boolean => true,
    subscribe:
        (_listener: () => void): (() => void) =>
        () => {},
    getSnapshot: (): number => 0,
};

export function PermissionGate({
    children,
    fallback = null,
}: {
    children: ReactNode;
    fallback?: ReactNode;
    anyOf?: string[];
    allOf?: string[];
}): ReactNode {
    return children ?? fallback;
}

/**
 * Normalizes a CRUD permission map from the backend into flat permission strings.
 * e.g. `normalizeCrudMapRecord('api', { DEFINITION: ['R','U'] })` → `['api-definition-r', 'api-definition-u']`
 */
export function normalizeCrudMapRecord(scope: string, record: Record<string, string[] | string>): string[] {
    return Object.entries(record).flatMap(([resource, ops]) => {
        const operations = Array.isArray(ops) ? ops : [ops];
        return operations.map(op => `${scope}-${resource.toLowerCase()}-${op.toLowerCase()}`);
    });
}
