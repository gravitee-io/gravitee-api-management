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

import { getV4ScoredApis } from '../services/apiScore';
import type { EnvironmentApiScore } from '../types/apiScore';
import { apiScoreKeys } from '../utils/queryKeys';

/**
 * Every V4 HTTP-proxy API with its latest score row. The proxy list is the source of truth for which
 * APIs appear; score data is merged in from `GET /scoring/apis`.
 */
export function useV4ScoredApis() {
    const env = useEnvironment();
    return useQuery<EnvironmentApiScore[]>({
        queryKey: apiScoreKeys.v4ScoredApis(env?.id ?? ''),
        queryFn: () => getV4ScoredApis(env!.id),
        enabled: Boolean(env),
        placeholderData: keepPreviousData,
    });
}
