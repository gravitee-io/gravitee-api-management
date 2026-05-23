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

import { searchApis } from '../../apis/services/apiList';
import type { ApiListItem } from '../../apis/types';
import { apiListKeys } from '../../apis/utils/queryKeys';

const RECENT_PAGE = 1;
const RECENT_PER_PAGE = 6;

export interface DashboardRecentApis {
    apis: ApiListItem[];
    isLoading: boolean;
    isError: boolean;
}

export function useDashboardRecentApis(): DashboardRecentApis {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(envId);

    const query = useQuery({
        queryKey: apiListKeys.search(envId, '', RECENT_PAGE, RECENT_PER_PAGE),
        queryFn: () => searchApis(envId, {}, RECENT_PAGE, RECENT_PER_PAGE),
        enabled,
        staleTime: 60_000,
    });

    return {
        apis: query.data?.data ?? [],
        isLoading: query.isLoading,
        isError: query.isError,
    };
}
