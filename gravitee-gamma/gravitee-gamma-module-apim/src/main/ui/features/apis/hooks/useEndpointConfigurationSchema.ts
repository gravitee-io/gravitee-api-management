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

import { getEndpointConfigurationSchema } from '../services/endpointPlugins';
import { endpointPluginKeys } from '../utils/queryKeys';

/** Plugin schemas rarely change within a session — cache for a few minutes. */
const PLUGIN_STALE_MS = 5 * 60 * 1000;

export function useEndpointConfigurationSchema(endpointType: string | undefined) {
    return useQuery({
        queryKey: endpointPluginKeys.configurationSchema(endpointType ?? ''),
        queryFn: () => getEndpointConfigurationSchema(endpointType!),
        enabled: Boolean(endpointType),
        staleTime: PLUGIN_STALE_MS,
    });
}
