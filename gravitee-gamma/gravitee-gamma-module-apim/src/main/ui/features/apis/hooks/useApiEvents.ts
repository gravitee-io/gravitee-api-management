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

import { getApiEvents } from '../services/apis';
import type { ApiEvent } from '../types';
import { apiEventsKeys } from '../utils/queryKeys';

export function useApiEvents(apiId: string | undefined, page: number, perPage: number, enabled = true) {
    const env = useEnvironment();
    return useQuery({
        queryKey: apiEventsKeys.list(env?.id ?? '', apiId ?? '', page, perPage),
        queryFn: () => getApiEvents(env!.id, apiId!, { page, perPage }),
        enabled: enabled && Boolean(env && apiId),
        staleTime: 30_000,
    });
}

/**
 * The deployment in use is the newest event. Page 1 of the history already lists it first, so it is
 * only fetched separately once the user leaves that page.
 */
export function useLiveDeploymentEvent(apiId: string | undefined, page: number, events: ApiEvent[]): ApiEvent | null {
    const isFirstPage = page === 1;
    const { data } = useApiEvents(apiId, 1, 1, !isFirstPage);
    return (isFirstPage ? events[0] : data?.data[0]) ?? null;
}
