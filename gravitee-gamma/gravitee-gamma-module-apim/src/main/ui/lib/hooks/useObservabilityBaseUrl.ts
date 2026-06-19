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
import { buildObservabilityBaseUrl } from '@gravitee/gamma-lib-observability';
import { useQuery } from '@tanstack/react-query';

import { useEnvironmentId } from './useEnvironmentId';
import { loadApimBootstrap } from '../../shared/api/apimClient';

const useDevProxy = process.env.NODE_ENV !== 'production';

export function useObservabilityBaseUrl() {
    const environmentId = useEnvironmentId();

    const query = useQuery({
        queryKey: ['observability-base-url', environmentId, useDevProxy] as const,
        queryFn: async () => {
            const { gammaBaseURL, organizationId } = await loadApimBootstrap();
            return buildObservabilityBaseUrl(gammaBaseURL, organizationId, environmentId, { useDevProxy });
        },
        staleTime: Infinity,
    });

    return {
        baseUrl: query.data,
        isLoading: query.isLoading,
        isError: query.isError,
        error: query.error,
    };
}
