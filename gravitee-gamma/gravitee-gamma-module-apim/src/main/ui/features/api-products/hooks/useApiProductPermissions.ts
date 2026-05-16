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
import { type PermissionScope, permissionService, useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';

import { getApiProductPermissions } from '../services/apiProductMembers';
import { apiProductKeys } from '../utils/queryKeys';

/**
 * Fetches the current user's API-product-scoped permissions and loads them into
 * the shared `permissionService` singleton for the duration of the product detail view.
 *
 * Mirrors the `useApiPermissions` pattern: load on enter, clear on leave.
 */
export function useApiProductPermissions(productId: string | undefined): { permissionsReady: boolean } {
    const env = useEnvironment();

    const { data: permissions, isSuccess } = useQuery({
        queryKey: apiProductKeys.permissions(env?.id ?? '', productId ?? ''),
        queryFn: () => getApiProductPermissions(env!.id, productId!),
        enabled: Boolean(env && productId),
        staleTime: 60_000,
    });

    useEffect(() => {
        if (!permissions) return;
        // 'api_product' is not yet in the alpha SDK's PermissionScope union — cast until SDK is updated
        const scope = 'api_product' as PermissionScope;
        permissionService.load(scope, permissions);
        return () => permissionService.clear(scope);
    }, [permissions]);

    return { permissionsReady: isSuccess };
}
