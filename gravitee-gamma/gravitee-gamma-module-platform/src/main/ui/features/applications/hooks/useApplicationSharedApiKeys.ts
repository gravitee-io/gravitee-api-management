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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { listApplicationApiKeys, renewApplicationApiKey, revokeApplicationApiKey } from '../services/applicationSharedApiKeys';
import { mapApiKeysToRows } from '../utils/applicationSubscriptionApiKeyMapper';
import { applicationSubscriptionKeys } from '../utils/queryKeys';

function sharedApiKeysQueryKey(envId: string | undefined, applicationId: string | undefined) {
    if (!envId || !applicationId) {
        return null;
    }
    return applicationSubscriptionKeys.sharedApiKeys(envId, applicationId);
}

export function useApplicationSharedApiKeys(applicationId: string | undefined, enabled: boolean) {
    const env = useEnvironment();
    const envId = env?.id;

    return useQuery({
        queryKey: applicationSubscriptionKeys.sharedApiKeys(envId, applicationId),
        queryFn: async () => {
            const entities = await listApplicationApiKeys(envId!, applicationId!);
            return mapApiKeysToRows(entities);
        },
        enabled: Boolean(enabled && envId && applicationId),
        staleTime: 30_000,
    });
}

export function useRenewApplicationApiKey(applicationId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (customApiKey?: string) => {
            if (!env?.id || !applicationId) {
                throw new Error('Failed to renew API key. Please try again.');
            }
            return renewApplicationApiKey(env.id, applicationId, customApiKey);
        },
        onSuccess: async () => {
            const queryKey = sharedApiKeysQueryKey(env?.id, applicationId);
            if (queryKey) {
                await queryClient.refetchQueries({ queryKey });
            }
        },
    });
}

export function useRevokeApplicationApiKey(applicationId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (apiKeyId: string) => {
            if (!env?.id || !applicationId) {
                throw new Error('Failed to revoke API key. Please try again.');
            }
            return revokeApplicationApiKey(env.id, applicationId, apiKeyId);
        },
        onSuccess: async () => {
            const queryKey = sharedApiKeysQueryKey(env?.id, applicationId);
            if (queryKey) {
                await queryClient.refetchQueries({ queryKey });
            }
        },
    });
}
