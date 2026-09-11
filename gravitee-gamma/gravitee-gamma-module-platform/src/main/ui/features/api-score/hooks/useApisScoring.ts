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
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { listApisScoring } from '../services/scoring';
import type { EnvironmentApiScore } from '../types/scoring';
import { environmentScoringKeys } from '../utils/queryKeys';

export function useApisScoring({ page, perPage }: { page: number; perPage: number }) {
    const env = useEnvironment();
    const result = useQuery({
        queryKey: environmentScoringKeys.apis(env?.id ?? '', page, perPage),
        queryFn: () => listApisScoring(env!.id, { page, perPage }),
        enabled: Boolean(env?.id),
        placeholderData: keepPreviousData,
    });

    const apis: EnvironmentApiScore[] = result.data?.data ?? [];

    return {
        apis,
        totalCount: result.data?.pagination.totalCount ?? 0,
        isLoading: result.isLoading,
        isError: result.isError,
        error: result.error,
    };
}
