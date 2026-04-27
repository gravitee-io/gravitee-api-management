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

import { useApimRuntime } from '../context/apimRuntimeContext';
import { fetchPolicySchema } from '../services/policySchemas';
import { proxyCreationKeys } from './queryKeys';

export function usePolicySchema(policyId: string, enabled = true) {
    const runtime = useApimRuntime();
    return useQuery({
        queryKey: proxyCreationKeys.policySchema(runtime, policyId),
        queryFn: () => fetchPolicySchema(runtime, policyId),
        enabled: enabled && Boolean(runtime.managementBaseURL) && Boolean(policyId),
        staleTime: 15 * 60_000,
    });
}

