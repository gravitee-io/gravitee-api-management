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
import { useQuery } from '@tanstack/react-query';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { getApiAvailability, getApiAvailabilityAverage } from '../services/environmentHealthApis';
import { availabilityFromMetric, type AvailabilityView } from '../utils/availability';
import type { HealthTimeRange, Timeframe } from '../utils/healthTimeframe';
import { environmentHealthKeys } from '../utils/queryKeys';

/**
 * Mirrors Classic's per-row pair: `health?type=availability` for the percentage, and
 * `health/average?type=AVAILABILITY&from&to&interval` for the window. Classic reuses the cached health
 * payload when the timeframe changes and re-requests only the average, so the keys are split the same way.
 */
export function useEnvironmentHealthAvailability({
    apiId,
    timeframe,
    range,
    enabled,
    reloadToken = 0,
}: {
    apiId: string;
    timeframe: Timeframe;
    range: HealthTimeRange;
    enabled: boolean;
    reloadToken?: number;
}) {
    const env = useEnvironment();
    const isEnabled = Boolean(env) && enabled;

    const health = useQuery({
        queryKey: environmentHealthKeys.availability(env?.id ?? '', apiId, reloadToken),
        queryFn: ({ signal }) => getApiAvailability(env!.id, apiId, signal),
        enabled: isEnabled,
        // Refresh and timeframe changes move the key, so nothing else needs to refetch. Without this a
        // remounted cell re-requested what it already had.
        staleTime: Infinity,
    });

    const average = useQuery({
        queryKey: environmentHealthKeys.availabilityAverage(env?.id ?? '', apiId, timeframe, reloadToken),
        queryFn: ({ signal }) => getApiAvailabilityAverage(env!.id, apiId, range, signal),
        enabled: isEnabled,
        staleTime: Infinity,
    });

    const availability: AvailabilityView | undefined =
        health.data || average.data ? availabilityFromMetric(health.data, average.data, timeframe) : undefined;

    return {
        availability,
        isLoading: health.isLoading || average.isLoading,
        isError: health.isError || average.isError,
    };
}
