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
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';

import { getAvailability, getAverageResponseTime, getResponseTimeOvertime } from '../../../../services/healthCheck';
import type { AvailabilityRow, HealthField, ResponseTimeTrendPoint } from '../../../../types/healthCheck';
import { toAvailabilityPct, toAvailabilityRows, toResponseTimeTrend } from '../../../../utils/healthCheckDashboard';
import { resolveHealthTimeRange, type Timeframe } from '../../../../utils/healthTimeframe';
import { apiHealthCheckKeys } from '../../../../utils/queryKeys';

const STALE_TIME = 30_000;

/** Independent state for a single headline metric (mirrors one console widget). */
export interface MetricState {
    /** Present only when the source query resolved with data. */
    value?: number;
    isLoading: boolean;
    isError: boolean;
}

export interface AvailabilityFieldData {
    rows: AvailabilityRow[];
    isLoading: boolean;
    isError: boolean;
}

export interface TrendData {
    points: ResponseTimeTrendPoint[];
    isLoading: boolean;
    isError: boolean;
}

export interface HealthCheckDashboardData {
    canRead: boolean;
    /** Global availability %, from the availability(endpoint) query only. */
    availability: MetricState;
    /** Global average response time (ms), from the avg-response-time(endpoint) query only. */
    responseTime: MetricState;
    trend: TrendData;
    endpoint: AvailabilityFieldData;
    gateway: AvailabilityFieldData;
    /** True if any underlying query failed — used only for a non-blocking toast. */
    anyError: boolean;
    refresh: () => void;
}

/**
 * Composes the read-path TanStack queries for the health-check dashboard.
 *
 * Each widget is wired to its own minimal source query — exactly like the classic
 * console V4 dashboard — so a single failing endpoint only degrades the widgets that
 * depend on it (e.g. global availability still renders when average-response-time fails).
 * Queries are shared/deduped by query key between the headline metrics and the per-field
 * tables.
 */
export function useHealthCheckDashboard(apiId: string | undefined, timeframe: Timeframe): HealthCheckDashboardData {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const id = apiId ?? '';
    const canRead = useHasPermission({ anyOf: ['api-health-r'] });
    const queryClient = useQueryClient();

    const { from, to } = useMemo(() => resolveHealthTimeRange(timeframe), [timeframe]);
    const enabled = Boolean(env && apiId && canRead);

    const trendQuery = useQuery({
        queryKey: apiHealthCheckKeys.trend(envId, id, timeframe),
        queryFn: () => getResponseTimeOvertime(envId, id, from, to),
        enabled,
        staleTime: STALE_TIME,
        retry: 0,
    });

    const endpointAvailability = useFieldAvailability(envId, id, 'endpoint', timeframe, from, to, enabled);
    const endpointResponseTime = useFieldResponseTime(envId, id, 'endpoint', timeframe, from, to, enabled);
    const gatewayAvailability = useFieldAvailability(envId, id, 'gateway', timeframe, from, to, enabled);
    const gatewayResponseTime = useFieldResponseTime(envId, id, 'gateway', timeframe, from, to, enabled);

    const trend = useMemo<TrendData>(
        () => ({ points: toResponseTimeTrend(trendQuery.data), isLoading: trendQuery.isLoading, isError: trendQuery.isError }),
        [trendQuery.data, trendQuery.isLoading, trendQuery.isError],
    );

    const endpoint = useMemo<AvailabilityFieldData>(
        () => ({
            rows: toAvailabilityRows(endpointAvailability.data, endpointResponseTime.data),
            isLoading: endpointAvailability.isLoading || endpointResponseTime.isLoading,
            isError: endpointAvailability.isError || endpointResponseTime.isError,
        }),
        [
            endpointAvailability.data,
            endpointAvailability.isLoading,
            endpointAvailability.isError,
            endpointResponseTime.data,
            endpointResponseTime.isLoading,
            endpointResponseTime.isError,
        ],
    );

    const gateway = useMemo<AvailabilityFieldData>(
        () => ({
            rows: toAvailabilityRows(gatewayAvailability.data, gatewayResponseTime.data),
            isLoading: gatewayAvailability.isLoading || gatewayResponseTime.isLoading,
            isError: gatewayAvailability.isError || gatewayResponseTime.isError,
        }),
        [
            gatewayAvailability.data,
            gatewayAvailability.isLoading,
            gatewayAvailability.isError,
            gatewayResponseTime.data,
            gatewayResponseTime.isLoading,
            gatewayResponseTime.isError,
        ],
    );

    const availability = useMemo<MetricState>(
        () => ({
            value: endpointAvailability.data ? toAvailabilityPct(endpointAvailability.data.global) : undefined,
            isLoading: endpointAvailability.isLoading,
            isError: endpointAvailability.isError,
        }),
        [endpointAvailability.data, endpointAvailability.isLoading, endpointAvailability.isError],
    );

    const responseTime = useMemo<MetricState>(
        () => ({
            value: endpointResponseTime.data ? Math.round(endpointResponseTime.data.global) : undefined,
            isLoading: endpointResponseTime.isLoading,
            isError: endpointResponseTime.isError,
        }),
        [endpointResponseTime.data, endpointResponseTime.isLoading, endpointResponseTime.isError],
    );

    const refresh = useCallback(() => {
        void queryClient.invalidateQueries({ queryKey: apiHealthCheckKeys.all });
    }, [queryClient]);

    return {
        canRead,
        availability,
        responseTime,
        trend,
        endpoint,
        gateway,
        anyError:
            trendQuery.isError ||
            endpointAvailability.isError ||
            endpointResponseTime.isError ||
            gatewayAvailability.isError ||
            gatewayResponseTime.isError,
        refresh,
    };
}

function useFieldAvailability(
    envId: string,
    apiId: string,
    field: HealthField,
    timeframe: Timeframe,
    from: number,
    to: number,
    enabled: boolean,
) {
    return useQuery({
        queryKey: apiHealthCheckKeys.availability(envId, apiId, field, timeframe),
        queryFn: () => getAvailability(envId, apiId, from, to, field),
        enabled,
        staleTime: STALE_TIME,
        retry: 0,
    });
}

function useFieldResponseTime(
    envId: string,
    apiId: string,
    field: HealthField,
    timeframe: Timeframe,
    from: number,
    to: number,
    enabled: boolean,
) {
    return useQuery({
        queryKey: apiHealthCheckKeys.avgResponseTime(envId, apiId, field, timeframe),
        queryFn: () => getAverageResponseTime(envId, apiId, from, to, field),
        enabled,
        staleTime: STALE_TIME,
        retry: 0,
    });
}
