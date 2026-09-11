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
import { useEffect, useRef, useState } from 'react';

import { notify } from '../../../shared/notify';
import { evaluateApiScoring, getApiScoring, listScoringJobs } from '../services/apiScoring';
import { apiScoringKeys } from '../utils/queryKeys';
import { latestScoringJob } from '../utils/scoring';

export function useApiScoring(apiId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const queryClient = useQueryClient();
    const enabled = Boolean(env && apiId);

    const jobsQuery = useQuery({
        queryKey: apiScoringKeys.jobs(envId, apiId ?? ''),
        queryFn: () => listScoringJobs(envId, apiId!),
        enabled,
        refetchInterval: query => (latestScoringJob(query.state.data?.data ?? [])?.status === 'PENDING' ? 1000 : false),
    });

    const jobsPending = latestScoringJob(jobsQuery.data?.data ?? [])?.status === 'PENDING';
    const [awaitingJob, setAwaitingJob] = useState(false);

    const scoringQuery = useQuery({
        queryKey: apiScoringKeys.report(envId, apiId ?? ''),
        queryFn: () => getApiScoring(envId, apiId!),
        enabled,
    });

    const evaluateMutation = useMutation({
        mutationFn: () => evaluateApiScoring(envId, apiId!),
        onMutate: () => {
            setAwaitingJob(true);
        },
        onError: error => notify.error(error, 'An error occurred while evaluating API Scoring.'),
        onSettled: async () => {
            await queryClient.invalidateQueries({ queryKey: apiScoringKeys.jobs(envId, apiId ?? '') });
            await queryClient.invalidateQueries({ queryKey: apiScoringKeys.report(envId, apiId ?? '') });
            setAwaitingJob(false);
        },
    });

    const wasJobsPending = useRef(false);
    useEffect(() => {
        if (wasJobsPending.current && !jobsPending) {
            void queryClient.invalidateQueries({ queryKey: apiScoringKeys.report(envId, apiId ?? '') });
        }
        wasJobsPending.current = jobsPending;
    }, [jobsPending, envId, apiId, queryClient]);

    return {
        scoring: scoringQuery.data,
        jobs: jobsQuery.data?.data ?? [],
        isLoading: jobsQuery.isLoading || scoringQuery.isLoading,
        isError: scoringQuery.isError || jobsQuery.isError,
        error: scoringQuery.error ?? jobsQuery.error,
        pending: jobsPending || evaluateMutation.isPending || awaitingJob,
        evaluate: () => evaluateMutation.mutate(),
        isEvaluating: evaluateMutation.isPending,
    };
}
