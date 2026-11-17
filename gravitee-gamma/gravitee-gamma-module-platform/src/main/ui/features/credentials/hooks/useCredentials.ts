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

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

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
