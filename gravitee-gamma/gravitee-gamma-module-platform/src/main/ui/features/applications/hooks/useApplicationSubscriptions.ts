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

import { listApplicationSubscriptions, listSubscribedApis } from '../services/applicationSubscriptions';
import type { ApiKeyMode } from '../types/application';
import type { ApplicationSubscriptionsFilters } from '../types/applicationSubscription';
import { mapSubscriptionsPageToRows } from '../utils/applicationSubscriptionMapper';
import { applicationSubscriptionKeys } from '../utils/queryKeys';

export function useApplicationSubscriptions(
    applicationId: string | undefined,
    filters: ApplicationSubscriptionsFilters,
    page: number,
    size: number,
    apiKeyMode: ApiKeyMode | undefined,
) {
    const env = useEnvironment();

    return useQuery({
        queryKey: applicationSubscriptionKeys.list(env?.id ?? '', applicationId ?? '', filters, page, size),
        queryFn: async () => {
            const response = await listApplicationSubscriptions(env!.id, applicationId!, filters, page, size);
            return {
                rows: mapSubscriptionsPageToRows(response, apiKeyMode),
                totalCount: response.page?.total_elements ?? 0,
            };
        },
        enabled: Boolean(env && applicationId),
    });
}

export function useSubscribedApis(applicationId: string | undefined) {
    const env = useEnvironment();

    return useQuery({
        queryKey: applicationSubscriptionKeys.subscribedApis(env?.id ?? '', applicationId ?? ''),
        queryFn: () => listSubscribedApis(env!.id, applicationId!),
        enabled: Boolean(env && applicationId),
        staleTime: 60_000,
    });
}
