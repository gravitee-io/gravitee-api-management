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

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { updateIntegration } from '../services/integrationUpdate';
import type { UpdateIntegrationRequest } from '../types/integration';
import { integrationKeys } from '../utils/queryKeys';

interface UpdateIntegrationVariables {
    integrationId: string;
    request: UpdateIntegrationRequest;
}

export function useUpdateIntegration() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ integrationId, request }: UpdateIntegrationVariables) => updateIntegration(env!.id, integrationId, request),
        retry: false,
        onSuccess: (_data, { integrationId }) => {
            void queryClient.invalidateQueries({ queryKey: [...integrationKeys.all, 'list'] });
            void queryClient.invalidateQueries({ queryKey: integrationKeys.detail(env!.id, integrationId) });
        },
    });
}
