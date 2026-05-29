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
import { useQuery } from '@tanstack/react-query';

import type { ApiRole, MembersResponse } from '../../apis/types/members.types';
import { getApiProductMembers, getApiProductRoles } from '../services/apiProductMembers';
import { apiProductKeys } from '../utils/queryKeys';

export function useApiProductMembers(productId: string | undefined) {
    const env = useEnvironment();
    return useQuery<MembersResponse>({
        queryKey: apiProductKeys.members(env?.id ?? '', productId ?? ''),
        queryFn: () => getApiProductMembers(env!.id, productId!),
        enabled: Boolean(env) && Boolean(productId),
    });
}

export function useApiProductRoles() {
    return useQuery<ApiRole[]>({
        queryKey: apiProductKeys.roles(),
        queryFn: () => getApiProductRoles(),
        staleTime: 5 * 60_000,
    });
}
