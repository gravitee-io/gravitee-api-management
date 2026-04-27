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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import type { ApiCreationState } from '../../../../domain/apiCreation/models';
import { useApimRuntime } from '../context/apimRuntimeContext';
import { getApimErrorMessage, runCreateProxyWorkflow } from '../createProxyWorkflow';
import type { ProxyConnectorBootstrap } from '../dto/types';
import { proxyCreationKeys } from './queryKeys';

export function useCreateProxyMutation() {
    const runtime = useApimRuntime();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: async (input: { data: ApiCreationState; bootstrap: ProxyConnectorBootstrap; askForReview?: boolean }) =>
            runCreateProxyWorkflow(runtime, input.data, input.bootstrap, { askForReview: input.askForReview }),
        onSuccess: () => {
            void qc.invalidateQueries({ queryKey: proxyCreationKeys.all });
        },
    });
}

export { getApimErrorMessage };
