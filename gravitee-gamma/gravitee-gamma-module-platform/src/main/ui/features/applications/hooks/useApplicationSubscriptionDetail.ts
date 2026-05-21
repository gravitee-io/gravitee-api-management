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

import { getApplicationSubscription, updateApplicationSubscription } from '../services/applicationSubscriptions';
import type { ApiKeyMode } from '../types/application';
import type { ApplicationSubscriptionEntity } from '../types/applicationSubscription';
import { mapSubscriptionEntityToDetail } from '../utils/applicationSubscriptionDetailMapper';
import { applicationSubscriptionKeys } from '../utils/queryKeys';

function subscriptionDetailQueryKey(
    envId: string | undefined,
    applicationId: string | undefined,
    subscriptionId: string | undefined,
): ReturnType<typeof applicationSubscriptionKeys.detail> | null {
    if (!envId || !applicationId || !subscriptionId) {
        return null;
    }
    return applicationSubscriptionKeys.detail(envId, applicationId, subscriptionId);
}

export function useApplicationSubscriptionDetail(
    applicationId: string | undefined,
    subscriptionId: string | undefined,
    apiKeyMode: ApiKeyMode | undefined,
) {
    const env = useEnvironment();
    const envId = env?.id;

    return useQuery({
        queryKey: applicationSubscriptionKeys.detail(envId, applicationId, subscriptionId),
        queryFn: async () => {
            const entity = await getApplicationSubscription(envId!, applicationId!, subscriptionId!);
            return {
                entity,
                detail: mapSubscriptionEntityToDetail(entity, apiKeyMode),
            };
        },
        enabled: Boolean(envId && applicationId && subscriptionId),
    });
}

export function useUpdateApplicationSubscription(applicationId: string | undefined, subscriptionId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ entity, request }: { entity: ApplicationSubscriptionEntity; request: string }) =>
            updateApplicationSubscription(env!.id, applicationId!, subscriptionId!, { ...entity, request }),
        onSuccess: () => {
            const queryKey = subscriptionDetailQueryKey(env?.id, applicationId, subscriptionId);
            if (queryKey) {
                void queryClient.invalidateQueries({ queryKey });
            }
        },
    });
}
