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

import { useEnvironmentId } from './useEnvironmentId';
import { loadApimBootstrap } from '../../shared/api/apimClient';

/**
 * The Management API v2 environment root the targets library talks to
 * (`{managementBaseURL}/v2/environments/{envId}`): performance targets and the
 * analytics definition are management resources, not Gamma-scoped ones, so this
 * is not the observability base URL.
 */
export function useTargetsBaseUrl(): string | undefined {
    const environmentId = useEnvironmentId();

    const { data } = useQuery({
        queryKey: ['targets-base-url', environmentId] as const,
        queryFn: async () => {
            const { managementBaseURL } = await loadApimBootstrap();
            return `${managementBaseURL}/v2/environments/${encodeURIComponent(environmentId)}`;
        },
        staleTime: Infinity,
    });

    return data;
}
