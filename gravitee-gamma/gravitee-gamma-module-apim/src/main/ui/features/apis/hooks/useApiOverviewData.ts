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
import { useState } from 'react';

import { listAlerts } from '../services/alerts';
import { getApiAnalyticsStats } from '../services/analytics';
import { getExposedEntrypoints } from '../services/entrypoints';
import { getApiMembers } from '../services/members';
import { apiAlertKeys, apiAnalyticsKeys, apiEntrypointKeys, apiMemberKeys } from '../utils/queryKeys';

const DAY_MS = 24 * 60 * 60 * 1000;
const WINDOW_MS = 5 * 60 * 1000;
const INTERVAL_MS = DAY_MS / 30;

export function useApiOverviewData(apiId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && apiId);

    const [timeRange] = useState(() => {
        const to = Math.floor(Date.now() / WINDOW_MS) * WINDOW_MS;
        return { from: to - DAY_MS, to, window: String(Math.floor(to / WINDOW_MS)) };
    });

    const membersQuery = useQuery({
        queryKey: apiMemberKeys.list(envId, apiId ?? ''),
        queryFn: () => getApiMembers(envId, apiId!),
        enabled,
    });

    const alertsQuery = useQuery({
        queryKey: apiAlertKeys.list(envId, apiId ?? ''),
        queryFn: () => listAlerts(envId, apiId!),
        enabled,
    });

    const exposedEntrypointsQuery = useQuery({
        queryKey: apiEntrypointKeys.exposed(envId, apiId ?? ''),
        queryFn: () => getExposedEntrypoints(envId, apiId!),
        enabled,
    });

    const statsQuery = useQuery({
        queryKey: apiAnalyticsKeys.stats(envId, apiId ?? '', timeRange.window),
        queryFn: () => getApiAnalyticsStats(envId, apiId!, timeRange.from, timeRange.to, INTERVAL_MS),
        enabled,
        staleTime: WINDOW_MS,
    });

    return {
        membersData: membersQuery.data,
        alertsData: alertsQuery.data,
        exposedEntrypoints: exposedEntrypointsQuery.data,
        analyticsStats: statsQuery.data,
        isLoadingTraffic: statsQuery.isLoading,
        isLoadingMembers: membersQuery.isFetching,
        isLoadingAlerts: alertsQuery.isFetching,
        isLoadingEntrypoints: exposedEntrypointsQuery.isFetching,
    };
}
