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
import { useMemo } from 'react';

import { listGatewayInstances } from '../services/instances';
import type { GatewayInstanceRow } from '../types/instance';
import { mapInstanceListItem } from '../utils/mapInstanceListItem';
import { gatewayInstanceKeys } from '../utils/queryKeys';

/** Classic list has no search; page is 1-based in UI, 0-based for the API. */
export function useGatewayInstanceList({ page, pageSize }: { page: number; pageSize: number }) {
    const env = useEnvironment();

    const query = useQuery({
        queryKey: gatewayInstanceKeys.list(env?.id ?? '', page, pageSize),
        queryFn: () =>
            listGatewayInstances(env!.id, {
                includeStopped: true,
                from: 0,
                to: 0,
                page: Math.max(page - 1, 0),
                size: pageSize,
            }),
        enabled: Boolean(env?.id),
        // FOUND-33 NFR: heartbeat data refreshed ≤ 30s (classic list has no polling; we refresh on an interval).
        refetchInterval: 30_000,
        staleTime: 15_000,
        placeholderData: keepPreviousData,
    });

    const rows: GatewayInstanceRow[] = useMemo(() => (query.data?.content ?? []).map(mapInstanceListItem), [query.data?.content]);

    return {
        ...query,
        rows,
        totalCount: query.data?.totalElements ?? 0,
    };
}
