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
import { useCallback, useEffect, useRef } from 'react';

import { usePendingScoringJobs } from './usePendingScoringJobs';
import { useV4ScoredApis } from './useV4ScoredApis';

/** Overview data + refresh + async-job polling (refetch scores when pending jobs complete). */
export function useApiScoreOverview() {
    const { data: apis, isLoading, isFetching, isError, refetch: refetchScores } = useV4ScoredApis();
    const { data: pendingJobs, isFetching: isFetchingJobs, refetch: refetchPendingJobs } = usePendingScoringJobs();

    const pendingCount = pendingJobs?.data?.length ?? 0;
    const hasPendingJobs = pendingCount > 0;
    const hadPendingJobs = useRef(hasPendingJobs);

    useEffect(() => {
        if (hadPendingJobs.current && !hasPendingJobs) {
            void refetchScores();
        }
        hadPendingJobs.current = hasPendingJobs;
    }, [hasPendingJobs, refetchScores]);

    const refresh = useCallback(async () => {
        await Promise.all([refetchScores(), refetchPendingJobs()]);
    }, [refetchPendingJobs, refetchScores]);

    const isRefreshing = (isFetching && !isLoading) || isFetchingJobs;

    return {
        apis,
        isLoading,
        isRefreshing,
        isError,
        hasPendingJobs,
        pendingCount,
        refresh,
    };
}
