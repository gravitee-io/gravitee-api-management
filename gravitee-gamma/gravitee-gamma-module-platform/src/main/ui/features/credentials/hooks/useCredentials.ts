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
import { keepPreviousData, useQueries, useQuery } from '@tanstack/react-query';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { getOrganizationUser } from '../../users/services/organizationUsers';
import { formatUserDisplayName } from '../../users/utils/userDetailDisplay';
import { getCredential, listCredentialConsumers, listCredentials } from '../services/credentials';
import { credentialKeys } from '../utils/queryKeys';

export function useCredentialsPaged({ query, page, perPage }: { query: string; page: number; perPage: number }) {
    const env = useEnvironment();

    return useQuery({
        queryKey: credentialKeys.list(env?.id ?? '', query, page, perPage),
        queryFn: () => listCredentials(env!.id, { query, page, perPage }),
        enabled: Boolean(env),
        placeholderData: keepPreviousData,
    });
}

export function useCredential(credentialId: string | undefined) {
    const env = useEnvironment();

    return useQuery({
        queryKey: credentialKeys.detail(env?.id ?? '', credentialId ?? ''),
        queryFn: () => getCredential(env!.id, credentialId!),
        enabled: Boolean(env) && Boolean(credentialId),
    });
}

export function useCredentialConsumers(credentialId: string | undefined, enabled = true) {
    const env = useEnvironment();

    return useQuery({
        queryKey: credentialKeys.consumers(env?.id ?? '', credentialId ?? ''),
        queryFn: () => listCredentialConsumers(env!.id, credentialId!),
        enabled: enabled && Boolean(env) && Boolean(credentialId),
    });
}

/**
 * Owner display names by user id. Reading a user needs an organization permission a credential
 * manager may lack, so an owner that cannot be read keeps its id.
 */
export function useCredentialOwnerNames(ownerIds: readonly (string | undefined)[]): Readonly<Record<string, string>> {
    const ids = [...new Set(ownerIds.filter((id): id is string => Boolean(id)))];
    const results = useQueries({
        queries: ids.map(id => ({
            queryKey: credentialKeys.owner(id),
            queryFn: () => getOrganizationUser(id),
            staleTime: 5 * 60_000,
            retry: false,
        })),
    });
    const names: Record<string, string> = {};
    ids.forEach((id, index) => {
        const user = results[index]?.data;
        names[id] = user ? formatUserDisplayName(user) : id;
    });
    return names;
}
