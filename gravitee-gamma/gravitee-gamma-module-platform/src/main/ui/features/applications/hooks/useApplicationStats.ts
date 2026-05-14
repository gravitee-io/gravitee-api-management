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

import { listApplications } from '../services/applicationList';
import { applicationListKeys } from '../utils/queryKeys';

const STATS_PAGE = 1;
const STATS_PER_PAGE = 1;

export interface ApplicationStats {
    active: number | null;
    archived: number | null;
    /** Sum of active + archived when both counts are known; used for empty-landing gating only. */
    total: number | null;
    isLoading: boolean;
    isLoadingActive: boolean;
    isLoadingArchived: boolean;
}

export interface UseApplicationStatsOptions {
    readonly knownCounts?: {
        readonly active?: number;
        readonly archived?: number;
    };
}

export function useApplicationStats(query?: string, options?: UseApplicationStatsOptions): ApplicationStats {
    const env = useEnvironment();
    const enabled = Boolean(env);
    const envId = env?.id ?? '';
    const knownActive = options?.knownCounts?.active;
    const knownArchived = options?.knownCounts?.archived;

    const activeQuery = useQuery({
        queryKey: applicationListKeys.count(envId, { status: 'ACTIVE', query }),
        queryFn: () =>
            listApplications(envId, {
                page: STATS_PAGE,
                size: STATS_PER_PAGE,
                status: 'ACTIVE',
                query,
            }),
        enabled: enabled && knownActive === undefined,
        staleTime: 60_000,
    });

    const archivedQuery = useQuery({
        queryKey: applicationListKeys.count(envId, { status: 'ARCHIVED', query }),
        queryFn: () =>
            listApplications(envId, {
                page: STATS_PAGE,
                size: STATS_PER_PAGE,
                status: 'ARCHIVED',
                query,
            }),
        enabled: enabled && knownArchived === undefined,
        staleTime: 60_000,
    });

    const isLoadingActive = knownActive === undefined && activeQuery.isLoading;
    const isLoadingArchived = knownArchived === undefined && archivedQuery.isLoading;

    const active = knownActive ?? activeQuery.data?.page.total_elements ?? null;
    const archived = knownArchived ?? archivedQuery.data?.page.total_elements ?? null;
    const total = active !== null && archived !== null ? active + archived : null;

    return {
        active,
        archived,
        total,
        isLoading: isLoadingActive || isLoadingArchived,
        isLoadingActive,
        isLoadingArchived,
    };
}
