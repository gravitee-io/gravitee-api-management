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

export function useApplicationSubscriptionDetail(
    applicationId: string | undefined,
    subscriptionId: string | undefined,
    apiKeyMode: ApiKeyMode | undefined,
) {
    const env = useEnvironment();

    return useQuery({
        queryKey: applicationSubscriptionKeys.detail(env?.id ?? '', applicationId ?? '', subscriptionId ?? ''),
        queryFn: async () => {
            const entity = await getApplicationSubscription(env!.id, applicationId!, subscriptionId!);
            return {
                entity,
                detail: mapSubscriptionEntityToDetail(entity, apiKeyMode),
            };
        },
        enabled: Boolean(env && applicationId && subscriptionId),
    });
}

export function useUpdateApplicationSubscription(applicationId: string | undefined, subscriptionId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ entity, request }: { entity: ApplicationSubscriptionEntity; request: string }) =>
            updateApplicationSubscription(env!.id, applicationId!, subscriptionId!, { ...entity, request }),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: applicationSubscriptionKeys.all });
        },
    });
}
