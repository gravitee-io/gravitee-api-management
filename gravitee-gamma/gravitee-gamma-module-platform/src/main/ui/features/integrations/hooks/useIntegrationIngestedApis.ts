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
import { useEffect, useRef } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { listIngestedApis } from '../services/integrationDetail';
import { INGESTION_POLL_INTERVAL_MS } from '../utils/ingestion';
import { integrationKeys } from '../utils/queryKeys';

export function useIntegrationIngestedApis(
    integrationId: string,
    { page, perPage, isIngesting }: { page: number; perPage: number; isIngesting: boolean },
) {
    const env = useEnvironment();

    const query = useQuery({
        queryKey: integrationKeys.ingestedApis(env?.id ?? '', integrationId, page, perPage),
        queryFn: () => listIngestedApis(env!.id, integrationId, { page, perPage }),
        enabled: Boolean(env && integrationId),
        placeholderData: keepPreviousData,
        refetchInterval: isIngesting ? INGESTION_POLL_INTERVAL_MS : false,
    });

    useRefetchWhenIngestionEnds(isIngesting, query.refetch);

    return query;
}

// The integration poll stops as soon as the job ends, so APIs written at the very end of the job are only picked up by this extra fetch.
function useRefetchWhenIngestionEnds(isIngesting: boolean, refetch: () => Promise<unknown>) {
    const wasIngesting = useRef(isIngesting);

    useEffect(() => {
        if (wasIngesting.current && !isIngesting) {
            void refetch();
        }
        wasIngesting.current = isIngesting;
    }, [isIngesting, refetch]);
}
