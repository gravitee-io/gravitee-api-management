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

import { getEnvironmentRequestResponseTime } from '../services/analytics';
import { envAnalyticsKeys } from '../utils/queryKeys';

const DAY_MS = 24 * 60 * 60 * 1000;

export function useEnvironmentTotalCalls(): { total: number | null; isLoading: boolean } {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(envId);

    const query = useQuery({
        queryKey: envAnalyticsKeys.requestResponseTime(envId, '24h'),
        queryFn: () => {
            const to = Date.now();
            return getEnvironmentRequestResponseTime(envId, to - DAY_MS, to);
        },
        enabled,
        staleTime: 60_000,
    });

    return {
        total: query.data?.requestsTotal ?? null,
        isLoading: query.isLoading,
    };
}
