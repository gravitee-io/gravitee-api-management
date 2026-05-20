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

import { getApiPermissions } from '../services/permissions';
import { apiPermissionKeys } from '../utils/queryKeys';

/**
 * Fetches the current user's API-scoped permissions and loads them into the
 * shared `permissionService` singleton for the duration of the API detail view.
 *
 * Mirrors the `ApisGuard.loadPermissions` pattern from the console webui:
 * load on enter, clear on leave.
 */
export function useApiPermissions(apiId: string | undefined): { permissionsReady: boolean } {
    const env = useEnvironment();

    const { data: permissions, isSuccess } = useQuery({
        queryKey: apiPermissionKeys.detail(env?.id ?? '', apiId ?? ''),
        queryFn: () => getApiPermissions(env!.id, apiId!),
        enabled: Boolean(env && apiId),
        staleTime: 60_000,
    });

    useEffect(() => {
        if (!permissions) return;
        permissionService.load('api', permissions);
        return () => permissionService.clear('api');
    }, [permissions]);

    return { permissionsReady: isSuccess };
}
