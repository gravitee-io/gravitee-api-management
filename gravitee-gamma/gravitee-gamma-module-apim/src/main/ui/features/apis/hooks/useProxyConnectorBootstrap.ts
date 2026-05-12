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

import { useApimRuntime } from '../../../core/context/apimRuntimeContext';
import { resolveProxyConnectorBootstrap } from '../../../services/connectors';
import { proxyCreationKeys } from '../../../utils/queryKeys';

export function useProxyConnectorBootstrap(enabled = true) {
    const runtime = useApimRuntime();
    return useQuery({
        queryKey: proxyCreationKeys.bootstrap(runtime),
        queryFn: () => resolveProxyConnectorBootstrap(runtime),
        enabled: enabled && Boolean(runtime.managementBaseURL),
        staleTime: 5 * 60_000,
    });
}
