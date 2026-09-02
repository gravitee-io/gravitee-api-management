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
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';

import { notify } from '../notify';
import { getEnvironmentPermissions } from '../services/environmentPermissions';
import { environmentPermissionKeys } from '../utils/queryKeys';

function useEnvironmentPermissionsQuery() {
    const env = useEnvironment();

    return useQuery({
        queryKey: environmentPermissionKeys.detail(env?.id ?? ''),
        queryFn: () => getEnvironmentPermissions(env!.id),
        enabled: Boolean(env?.id),
        staleTime: Infinity,
        gcTime: Infinity,
        refetchOnMount: false,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
    });
}

/**
 * Fetches environment-scoped permissions and merges them into the shared
 * `permissionService` while the module layout is mounted.
 *
 * The query key is only the environment id, so a previous login's cache is
 * dropped when the host permission service resets — not when this layout
 * remounts. Remounts must keep a live 403 patch; refetching would restore a
 * stale grant and re-arm the allow → 403 → redirect loop.
 *
 * This module cache is the source of truth while mounted: a host refetch that
 * writes a stripped grant back into permissionService is overwritten from cache.
 * Does not clear on unmount — the host owns teardown of that scope.
 */
export function useEnvironmentPermissions(): void {
    const queryClient = useQueryClient();

    // The host clears every scope on logout, so an observable drop to zero permissions is the signal
    // that this cache belongs to a previous login — see permissionService.reset() in permission-sync.ts.
    useEffect(() => {
        let previousCount = permissionService.getAllPermissions().length;
        return permissionService.subscribe(() => {
            const nextCount = permissionService.getAllPermissions().length;
            if (previousCount > 0 && nextCount === 0) {
                void queryClient.resetQueries({ queryKey: environmentPermissionKeys.all });
            }
            previousCount = nextCount;
        });
    }, [queryClient]);

    const { data: permissions, error, isError, isSuccess } = useEnvironmentPermissionsQuery();

    // An unreadable permission map is a server error, not a role decision. Without this the user only
    // sees a menu filtered down to nothing, which reads as "you were not granted anything" — Classic
    // draws the same distinction with a toast from its HTTP error interceptor.
    useEffect(() => {
        if (!isError) return;
        notify.error(error, 'Your permissions could not be loaded. Some menu items may be missing.');
    }, [error, isError]);

    useEffect(() => {
        if (!isSuccess || permissions === undefined || permissions === null) return;
        let applying = false;
        const applyFromCache = () => {
            applying = true;
            try {
                permissionService.load('environment', permissions);
            } finally {
                applying = false;
            }
        };
        applyFromCache();
        return permissionService.subscribe(() => {
            if (applying) return;
            const hostRestoredStrippedGrant = permissionService
                .getAllPermissions()
                .some(permission => permission.startsWith('environment-') && !permissions.includes(permission));
            if (hostRestoredStrippedGrant) {
                applyFromCache();
            }
        });
    }, [isSuccess, permissions]);
}

/**
 * True once environment-scoped permissions are in cache for this login, or the
 * fetch failed (fail closed with whatever the host already loaded).
 * Shares the query with {@link useEnvironmentPermissions} (deduped by React Query).
 */
export function useEnvironmentPermissionsReady(): boolean {
    const { isSuccess, isError } = useEnvironmentPermissionsQuery();
    return isSuccess || isError;
}

/**
 * Whether this module's own permission cache grants any of `anyOf`, or `undefined` when the cache
 * holds no answer (still loading, or the fetch failed).
 *
 * Route guards need that third state. Treating "no answer" as a denial while the sidebar and the
 * landing key read `permissionService` puts the two on different sources of truth, and the
 * disagreement becomes a redirect loop: landing sends the user to a page whose guard denies it,
 * and the denial sends them back to landing.
 */
export function useEnvironmentPermissionGrant(anyOf: readonly string[]): boolean | undefined {
    const { data: permissions } = useEnvironmentPermissionsQuery();
    if (permissions === undefined || permissions === null) return undefined;
    return anyOf.some(permission => permissions.includes(permission));
}

/**
 * True once at least one of `anyOf` is present among the current environment's
 * permissions. Reads the same query as {@link useEnvironmentPermissions} (deduped
 * by React Query), so it reflects a live 403 the moment {@link useForbiddenResourceRedirect}
 * strips the permission from the cache — unlike `useHasPermission` from the SDK's
 * federation stub, which isn't wired to this module's own permission fetch.
 *
 * Must keep this observer on the shared query options. A second `useQuery` on the
 * same key with `refetchOnMount: 'always'` would overwrite the 403 patch and
 * re-arm the allow → 403 → redirect loop.
 */
export function useHasEnvironmentPermission(anyOf: readonly string[]): boolean {
    const { data: permissions } = useEnvironmentPermissionsQuery();
    if (permissions === undefined || permissions === null) return false;
    return anyOf.some(permission => permissions.includes(permission));
}
