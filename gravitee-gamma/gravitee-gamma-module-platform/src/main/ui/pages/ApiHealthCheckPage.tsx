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
import {
    Alert,
    AlertDescription,
    Button,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { RefreshCwIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useState, type Dispatch, type SetStateAction } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';

import { ApiHealthCheckEmptyLanding } from '../features/api-health-check/components/ApiHealthCheckEmptyLanding';
import { HealthCheckApisTable } from '../features/api-health-check/components/HealthCheckApisTable';
import { HealthCheckReport } from '../features/api-health-check/components/HealthCheckReport';
import { useEnvironmentHealthApis } from '../features/api-health-check/hooks/useEnvironmentHealthApis';
import { useEnvironmentHealthReport } from '../features/api-health-check/hooks/useEnvironmentHealthReport';
import { healthCheckDashboardPath } from '../features/api-health-check/utils/healthCheckDashboardPath';
import { HEALTH_CHECK_FILTER_QUERY } from '../features/api-health-check/utils/healthCheckQuery';
import { DEFAULT_TIMEFRAME, resolveHealthTimeRange, TIMEFRAMES, type Timeframe } from '../features/api-health-check/utils/healthTimeframe';
import { orderToSort, sortToOrder, type TableSortingState } from '../features/applications/utils/tableSort';

const DEFAULT_PAGE = 1;
const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_ORDER: TableSortingState = [];
const SEARCH_DEBOUNCE_MS = 200;

function parsePositiveInt(value: string | null, fallback: number): number {
    const parsed = Number(value);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export function ApiHealthCheckPage() {
    const location = useLocation();
    const [searchParams, setSearchParams] = useSearchParams();

    const [timeframe, setTimeframe] = useState<Timeframe>(DEFAULT_TIMEFRAME);
    const [searchInput, setSearchInput] = useState(() => searchParams.get('q') ?? '');
    const [query, setQuery] = useState(() => searchParams.get('q') ?? '');
    const [page, setPage] = useState(() => parsePositiveInt(searchParams.get('page'), DEFAULT_PAGE));
    const [pageSize, setPageSize] = useState(() => parsePositiveInt(searchParams.get('size'), DEFAULT_PAGE_SIZE));
    const [sorting, setSorting] = useState<TableSortingState>(() => orderToSort(searchParams.get('order') ?? undefined, DEFAULT_ORDER));
    const [now, setNow] = useState(() => Date.now());

    const order = useMemo(() => sortToOrder(sorting), [sorting]);
    const range = useMemo(() => resolveHealthTimeRange(timeframe, undefined, now), [now, timeframe]);

    useEffect(() => {
        if (searchInput === query) {
            return;
        }
        const handle = window.setTimeout(() => {
            setQuery(searchInput);
            setPage(DEFAULT_PAGE);
        }, SEARCH_DEBOUNCE_MS);
        return () => window.clearTimeout(handle);
    }, [query, searchInput]);

    useEffect(() => {
        const next = new URLSearchParams();
        if (query) {
            next.set('q', query);
        }
        if (page !== DEFAULT_PAGE) {
            next.set('page', String(page));
        }
        if (pageSize !== DEFAULT_PAGE_SIZE) {
            next.set('size', String(pageSize));
        }
        if (order) {
            next.set('order', order);
        }
        setSearchParams(next, { replace: true });
    }, [order, page, pageSize, query, setSearchParams]);

    const list = useEnvironmentHealthApis({
        query,
        page,
        perPage: pageSize,
        sortBy: order,
        reloadToken: now,
    });
    const report = useEnvironmentHealthReport({ from: range.from, to: range.to, reloadToken: now });

    const handleSortingChange = useCallback<Dispatch<SetStateAction<TableSortingState>>>(updater => {
        setSorting(previous => (typeof updater === 'function' ? updater(previous) : updater));
        setPage(DEFAULT_PAGE);
    }, []);

    const handleSearchChange = (value: string) => {
        setSearchInput(value);
    };

    const handleFilterToHealthCheck = () => {
        setSearchInput(HEALTH_CHECK_FILTER_QUERY);
        setQuery(HEALTH_CHECK_FILTER_QUERY);
        setPage(DEFAULT_PAGE);
    };

    const dashboardHref = useCallback((apiId: string) => healthCheckDashboardPath(location.pathname, apiId), [location.pathname]);

    const isEmptyEnvironment =
        !list.isLoading && !list.isFetching && !list.isError && list.totalCount === 0 && query.trim() === '' && searchInput.trim() === '';

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-semibold tracking-tight">API Health Check</h1>
                <p className="text-muted-foreground text-sm">
                    Each API is monitored by a periodic HTTP request to the health check endpoint. The API backend receives the request and
                    responds, and the health check service determines if the response is as expected.
                </p>
            </div>

            {list.isError ? (
                <Alert variant="destructive">
                    <TriangleAlertIcon className="size-4" aria-hidden />
                    <AlertDescription className="flex flex-wrap items-center gap-3">
                        Failed to load APIs.
                        <Button size="sm" variant="outline" onClick={() => list.refetch()}>
                            Try again
                        </Button>
                    </AlertDescription>
                </Alert>
            ) : isEmptyEnvironment ? (
                <ApiHealthCheckEmptyLanding />
            ) : (
                <>
                    <div className="flex flex-wrap items-end gap-3">
                        <div className="space-y-2">
                            <Label htmlFor="health-check-timeframe">Timeframe</Label>
                            <Select value={timeframe} onValueChange={value => setTimeframe(value as Timeframe)}>
                                <SelectTrigger id="health-check-timeframe" className="w-44">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {TIMEFRAMES.map(option => (
                                        <SelectItem key={option.id} value={option.id}>
                                            {option.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        <Button type="button" variant="outline" size="sm" onClick={() => setNow(Date.now())} aria-label="Refresh">
                            <RefreshCwIcon className="size-4" aria-hidden />
                            Refresh
                        </Button>
                        <Button type="button" variant="outline" size="sm" onClick={handleFilterToHealthCheck}>
                            Filter to APIs with Health Check enabled
                        </Button>
                    </div>

                    <HealthCheckReport report={report.report} isLoading={report.isLoading} isError={report.isError} />

                    <HealthCheckApisTable
                        apis={list.apis}
                        totalCount={list.totalCount}
                        loading={list.isLoading}
                        query={searchInput}
                        page={page}
                        pageSize={pageSize}
                        sorting={sorting}
                        from={range.from}
                        to={range.to}
                        reloadToken={now}
                        dashboardHref={dashboardHref}
                        onSearchChange={handleSearchChange}
                        onPageChange={setPage}
                        onPageSizeChange={setPageSize}
                        onSortingChange={handleSortingChange}
                    />
                </>
            )}
        </div>
    );
}
