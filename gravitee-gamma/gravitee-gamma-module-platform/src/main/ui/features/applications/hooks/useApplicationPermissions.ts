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

import { getApplicationPermissions } from '../services/applicationPermissions';
import { applicationPermissionKeys } from '../utils/queryKeys';

export interface UseApplicationPermissionsResult {
    readonly permissionsReady: boolean;
    readonly isError: boolean;
    readonly isLoading: boolean;
    readonly refetch: () => void;
}

/**
 * Loads application-scoped permissions into `permissionService` for the detail view.
 * Mirrors `useApiPermissions`: load on enter, clear on leave.
 */
export function useApplicationPermissions(applicationId: string | undefined): UseApplicationPermissionsResult {
    const env = useEnvironment();

    const {
        data: permissions,
        isSuccess,
        isError,
        isLoading,
        refetch,
    } = useQuery({
        queryKey: applicationPermissionKeys.detail(env?.id ?? '', applicationId ?? ''),
        queryFn: () => getApplicationPermissions(env!.id, applicationId!),
        enabled: Boolean(env && applicationId),
        staleTime: 60_000,
    });

    useEffect(() => {
        if (!permissions) {
            return;
        }
        permissionService.load('application', permissions);
        return () => permissionService.clear('application');
    }, [permissions]);

    return {
        permissionsReady: isSuccess,
        isError,
        isLoading,
        refetch: () => {
            void refetch();
        },
    };
}
