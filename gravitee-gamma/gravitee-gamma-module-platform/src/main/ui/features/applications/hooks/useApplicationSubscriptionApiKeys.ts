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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
    expireSubscriptionApiKey,
    listSubscriptionApiKeys,
    renewSubscriptionApiKey,
    revokeSubscriptionApiKey,
    type SubscriptionApiKeyV2Parent,
} from '../services/applicationSubscriptions';
import { mapApiKeysToRows } from '../utils/applicationSubscriptionApiKeyMapper';
import { applicationSubscriptionKeys } from '../utils/queryKeys';

function apiKeysQueryKey(
    envId: string | undefined,
    applicationId: string | undefined,
    subscriptionId: string | undefined,
): ReturnType<typeof applicationSubscriptionKeys.apiKeys> | null {
    if (!envId || !applicationId || !subscriptionId) {
        return null;
    }
    return applicationSubscriptionKeys.apiKeys(envId, applicationId, subscriptionId);
}

export function useApplicationSubscriptionApiKeys(applicationId: string | undefined, subscriptionId: string | undefined, enabled: boolean) {
    const env = useEnvironment();
    const envId = env?.id;

    return useQuery({
        queryKey: applicationSubscriptionKeys.apiKeys(envId, applicationId, subscriptionId),
        queryFn: async () => {
            const entities = await listSubscriptionApiKeys(envId!, applicationId!, subscriptionId!);
            return mapApiKeysToRows(entities);
        },
        enabled: Boolean(enabled && envId && applicationId && subscriptionId),
        staleTime: 30_000,
    });
}

export function useRenewSubscriptionApiKey(applicationId: string | undefined, subscriptionId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: () => {
            if (!env?.id || !applicationId || !subscriptionId) {
                throw new Error('Failed to renew API key. Please try again.');
            }
            return renewSubscriptionApiKey(env.id, applicationId, subscriptionId);
        },
        onSuccess: async () => {
            const queryKey = apiKeysQueryKey(env?.id, applicationId, subscriptionId);
            if (queryKey) {
                await queryClient.refetchQueries({ queryKey });
            }
        },
    });
}

export function useRevokeSubscriptionApiKey(applicationId: string | undefined, subscriptionId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (apiKeyId: string) => {
            if (!env?.id || !applicationId || !subscriptionId) {
                throw new Error('Failed to revoke API key. Please try again.');
            }
            return revokeSubscriptionApiKey(env.id, applicationId, subscriptionId, apiKeyId);
        },
        onSuccess: async () => {
            const queryKey = apiKeysQueryKey(env?.id, applicationId, subscriptionId);
            if (queryKey) {
                await queryClient.refetchQueries({ queryKey });
            }
        },
    });
}

export function useExpireSubscriptionApiKey(
    applicationId: string | undefined,
    subscriptionId: string | undefined,
    parent: SubscriptionApiKeyV2Parent | null,
) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ apiKeyId, expireAt }: { apiKeyId: string; expireAt: Date }) => {
            if (!env?.id || !applicationId || !subscriptionId || !parent) {
                throw new Error('Failed to update API key expiration. Please try again.');
            }
            return expireSubscriptionApiKey(env.id, parent, subscriptionId, apiKeyId, expireAt);
        },
        onSuccess: async () => {
            const queryKey = apiKeysQueryKey(env?.id, applicationId, subscriptionId);
            if (queryKey) {
                await queryClient.refetchQueries({ queryKey });
            }
        },
    });
}
