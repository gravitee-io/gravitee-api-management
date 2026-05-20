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

import { getApiV4 } from '../services/apis';
import { apiDetailKeys } from '../utils/queryKeys';

export function useApiDetail(apiId: string | undefined) {
    const env = useEnvironment();
    return useQuery({
        queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? ''),
        queryFn: () => getApiV4(env!.id, apiId!),
        enabled: Boolean(env && apiId),
        staleTime: 60_000,
    });
}
