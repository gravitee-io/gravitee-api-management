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
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { listIdentityProviders, listOrganizationUsers } from '../services/organizationUsers';
import { GRAVITEE_IDP } from '../types/user';
import { organizationUserKeys } from '../utils/queryKeys';

export function useOrganizationUsers({ query, page, size }: { query: string; page: number; size: number }) {
    return useQuery({
        queryKey: organizationUserKeys.list(query, page, size),
        queryFn: () => listOrganizationUsers({ query, page, size }),
        staleTime: 30_000,
        placeholderData: keepPreviousData,
    });
}

export function useIdentityProviders() {
    return useQuery({
        queryKey: organizationUserKeys.identityProviders(),
        queryFn: async () => {
            const providers = await listIdentityProviders();
            return [GRAVITEE_IDP, ...providers];
        },
        staleTime: 60_000,
    });
}
