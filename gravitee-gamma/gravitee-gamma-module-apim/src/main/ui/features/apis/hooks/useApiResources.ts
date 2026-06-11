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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { getResourceSchema, listResourcePlugins, updateApiResources } from '../services/resources';
import type { ApiResource } from '../types/resource';
import { apiDetailKeys, resourcePluginKeys } from '../utils/queryKeys';

/** Plugin catalog rarely changes within a session — cache for a few minutes. */
const PLUGIN_STALE_MS = 5 * 60 * 1000;

export function useResourcePlugins() {
    return useQuery({
        queryKey: resourcePluginKeys.list(),
        queryFn: listResourcePlugins,
        staleTime: PLUGIN_STALE_MS,
    });
}

export function useResourceSchema(resourceId: string | undefined) {
    return useQuery({
        queryKey: resourcePluginKeys.schema(resourceId ?? ''),
        queryFn: () => getResourceSchema(resourceId!),
        enabled: Boolean(resourceId),
        staleTime: PLUGIN_STALE_MS,
    });
}

export function useUpdateApiResources(apiId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (resources: ApiResource[]) => {
            if (!env?.id || !apiId) {
                return Promise.reject(new Error('Environment or API is not ready yet'));
            }
            return updateApiResources(env.id, apiId, resources);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
        },
    });
}
