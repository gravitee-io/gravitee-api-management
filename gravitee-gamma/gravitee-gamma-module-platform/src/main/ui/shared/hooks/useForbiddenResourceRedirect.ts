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

import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import { environmentPermissionKeys } from '../utils/queryKeys';

// A live 403 overrides the cached environment-permissions map, which can otherwise still say access is granted.
// Deliberately don't invalidate/refetch the permissions map here: if the backend is still serving the same
// stale grant that caused this 403, a refetch would just restore it, flipping the nav back to "allowed" and
// re-arming the same allow -> 403 -> redirect loop on the next visit.
export function useForbiddenResourceRedirect({
    isForbidden,
    permissionPrefix,
    redirectTo,
}: {
    isForbidden: boolean;
    permissionPrefix: string;
    redirectTo: string;
}): void {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const navigate = useNavigate();

    useEffect(() => {
        if (!isForbidden) return;
        if (env?.id) {
            const permissionsKey = environmentPermissionKeys.detail(env.id);
            queryClient.setQueryData<string[]>(permissionsKey, previous =>
                (previous ?? []).filter(permission => !permission.startsWith(permissionPrefix)),
            );
        }
        navigate(redirectTo, { replace: true });
    }, [isForbidden, permissionPrefix, redirectTo, env?.id, queryClient, navigate]);
}
