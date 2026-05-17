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
import { useLayoutEffect, useState } from 'react';

import { getApplicationPermissions } from '../services/applicationPermissions';
import { applicationPermissionKeys } from '../utils/queryKeys';

/**
 * Loads application-scoped permissions into `permissionService` for the detail view.
 * Mirrors `useApiPermissions` and the console application detail guard.
 */
export function useApplicationPermissions(applicationId: string | undefined): { permissionsReady: boolean } {
    const env = useEnvironment();
    const [permissionsApplied, setPermissionsApplied] = useState(false);

    const { data: permissions, isSuccess } = useQuery({
        queryKey: applicationPermissionKeys.detail(env?.id ?? '', applicationId ?? ''),
        queryFn: () => getApplicationPermissions(env!.id, applicationId!),
        enabled: Boolean(env && applicationId),
        staleTime: 60_000,
    });

    useLayoutEffect(() => {
        if (!permissions) {
            setPermissionsApplied(false);
            return;
        }
        permissionService.load('application', permissions);
        setPermissionsApplied(true);
        return () => {
            permissionService.clear('application');
            setPermissionsApplied(false);
        };
    }, [permissions]);

    return { permissionsReady: isSuccess && permissionsApplied };
}
