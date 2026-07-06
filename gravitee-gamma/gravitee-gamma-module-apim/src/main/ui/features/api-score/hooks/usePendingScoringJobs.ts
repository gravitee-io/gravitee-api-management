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

import { listAsyncJobs } from '../services/asyncJobs';
import { apiScoreKeys } from '../utils/queryKeys';

const POLL_INTERVAL_MS = 1_000;

/** Polls the current user's pending SCORING_REQUEST jobs (classic per-API pattern, env-wide on overview). */
export function usePendingScoringJobs() {
    const env = useEnvironment();

    return useQuery({
        queryKey: apiScoreKeys.pendingScoringJobs(env?.id ?? ''),
        queryFn: () => listAsyncJobs(env!.id, { type: 'SCORING_REQUEST', status: 'PENDING', perPage: 100 }),
        enabled: Boolean(env),
        refetchInterval: query => {
            const pendingCount = query.state.data?.data?.length ?? 0;
            return pendingCount > 0 ? POLL_INTERVAL_MS : false;
        },
    });
}
