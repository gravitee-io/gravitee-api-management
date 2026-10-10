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

import { previewIntegration } from '../services/integrationDetail';
import { integrationKeys } from '../utils/queryKeys';

// Discovery asks the agent to scan the provider, so it runs once per visit and is never retried
// (the module default retries a 5xx); gcTime 0 makes reopening the route run it again.
export function useIntegrationDiscovery(integrationId: string, enabled: boolean) {
    const env = useEnvironment();

    return useQuery({
        queryKey: integrationKeys.preview(env?.id ?? '', integrationId),
        queryFn: () => previewIntegration(env!.id, integrationId),
        enabled: Boolean(env && integrationId) && enabled,
        retry: false,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
        staleTime: Infinity,
        gcTime: 0,
    });
}
