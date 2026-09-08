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

import { getApiAvailability } from '../services/environmentHealthApis';
import { availabilityFromMetric, type AvailabilityView } from '../utils/availability';
import { environmentHealthKeys } from '../utils/queryKeys';

export function useEnvironmentHealthAvailability({
    apiId,
    from,
    to,
    enabled,
    reloadToken = 0,
}: {
    apiId: string;
    from: number;
    to: number;
    enabled: boolean;
    reloadToken?: number;
}) {
    const env = useEnvironment();
    const result = useQuery({
        queryKey: environmentHealthKeys.availability(env?.id ?? '', apiId, from, to, reloadToken),
        queryFn: ({ signal }) => getApiAvailability(env!.id, apiId, from, to, signal),
        enabled: Boolean(env) && enabled,
    });

    const availability: AvailabilityView | undefined = result.data ? availabilityFromMetric(result.data) : undefined;

    return {
        availability,
        isLoading: result.isLoading,
        isError: result.isError,
    };
}
