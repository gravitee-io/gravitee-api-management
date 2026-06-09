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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState } from 'react';

import { getHealthCheckLogs } from '../../../../services/healthCheck';
import type { HealthCheckLog } from '../../../../types/healthCheck';
import { resolveHealthTimeRange, type Timeframe } from '../../../../utils/healthTimeframe';
import { apiHealthCheckKeys } from '../../../../utils/queryKeys';

export const FAILED_LOGS_PAGE_SIZES = [10, 25, 50, 100];
const DEFAULT_PAGE_SIZE = 10;

export interface FailedHealthCheckLogsData {
    logs: HealthCheckLog[];
    totalCount: number;
    page: number;
    pageSize: number;
    isLoading: boolean;
    isError: boolean;
    setPage: (page: number) => void;
    setPageSize: (pageSize: number) => void;
}

/**
 * Owns the pagination state for the failed health-check logs table so its page
 * changes don't re-render the rest of the dashboard. Always queries failures
 * (`success=false`) and keeps previous data while paging for a stable table.
 */
export function useFailedHealthCheckLogs(apiId: string | undefined, timeframe: Timeframe): FailedHealthCheckLogsData {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const id = apiId ?? '';
    const canRead = useHasPermission({ anyOf: ['api-health-r'] });

    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    useEffect(() => {
        setPage(1);
    }, [timeframe]);

    const { from, to } = useMemo(() => resolveHealthTimeRange(timeframe), [timeframe]);
    const enabled = Boolean(env && apiId && canRead);

    const { data, isLoading, isError } = useQuery({
        queryKey: apiHealthCheckKeys.logs(envId, id, timeframe, page, pageSize),
        queryFn: () => getHealthCheckLogs(envId, id, { from, to, page, perPage: pageSize, success: false }),
        enabled,
        placeholderData: keepPreviousData,
        staleTime: 30_000,
    });

    const handlePageSizeChange = useCallback((size: number) => {
        setPageSize(size);
        setPage(1);
    }, []);

    return {
        logs: data?.data ?? [],
        totalCount: data?.pagination?.totalCount ?? 0,
        page,
        pageSize,
        isLoading,
        isError,
        setPage,
        setPageSize: handlePageSizeChange,
    };
}
