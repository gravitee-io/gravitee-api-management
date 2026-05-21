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
import { permissionService, useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';

import { getEnvironmentPermissions } from '../services/environmentPermissions';
import { environmentPermissionKeys } from '../utils/queryKeys';

/**
 * Fetches environment-scoped permissions and merges them into the shared
 * `permissionService` while the module layout is mounted.
 *
 * When federated, the host permission-sync also loads this scope; duplicate
 * loads are harmless. Does not clear on unmount — the host owns that scope.
 */
export function useEnvironmentPermissions(): void {
    const env = useEnvironment();

    const { data: permissions } = useQuery({
        queryKey: environmentPermissionKeys.detail(env?.id ?? ''),
        queryFn: () => getEnvironmentPermissions(env!.id),
        enabled: Boolean(env?.id),
        staleTime: 60_000,
    });

    useEffect(() => {
        if (!permissions) return;
        permissionService.load('environment', permissions);
    }, [permissions]);
}

/**
 * True once environment-scoped permissions are loaded into {@link permissionService}.
 * Shares the query with {@link useEnvironmentPermissions} (deduped by React Query).
 */
export function useEnvironmentPermissionsReady(): boolean {
    const env = useEnvironment();

    const { isSuccess } = useQuery({
        queryKey: environmentPermissionKeys.detail(env?.id ?? ''),
        queryFn: () => getEnvironmentPermissions(env!.id),
        enabled: Boolean(env?.id),
        staleTime: 60_000,
    });

    return isSuccess;
}
