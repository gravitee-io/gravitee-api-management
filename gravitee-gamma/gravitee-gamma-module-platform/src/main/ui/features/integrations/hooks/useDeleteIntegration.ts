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

import { deleteIntegration } from '../services/integrationDetail';
import { integrationKeys } from '../utils/queryKeys';

export function useDeleteIntegration() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (integrationId: string) => deleteIntegration(env!.id, integrationId),
        retry: false,
        onSuccess: (_data, integrationId) => {
            // Invalidating integrationKeys.all would refetch the deleted integration while its page is still mounted.
            queryClient.removeQueries({ queryKey: integrationKeys.detail(env!.id, integrationId) });
            queryClient.removeQueries({ queryKey: integrationKeys.permissions(env!.id, integrationId) });
            queryClient.removeQueries({ queryKey: integrationKeys.federatedApis(env!.id, integrationId) });
            return queryClient.invalidateQueries({ queryKey: [...integrationKeys.all, 'list'] });
        },
    });
}
