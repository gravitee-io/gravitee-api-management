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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { createSharedPolicyGroup, deleteSharedPolicyGroup, updateSharedPolicyGroup } from '../services/sharedPolicyGroups';
import type { CreateSharedPolicyGroupPayload, SharedPolicyGroup, UpdateSharedPolicyGroupPayload } from '../types/sharedPolicyGroup';
import { sharedPolicyGroupKeys } from '../utils/queryKeys';

function useSharedPolicyGroupMutation<TData, TResult>(mutationFn: (envId: string, data: TData) => Promise<TResult>) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: TData) => {
            if (!env?.id) {
                return Promise.reject(new Error('No active environment'));
            }
            return mutationFn(env.id, data);
        },
        onSuccess: () => {
            if (env?.id) {
                void queryClient.invalidateQueries({ queryKey: sharedPolicyGroupKeys.all });
            }
        },
    });
}

export function useCreateSharedPolicyGroup() {
    return useSharedPolicyGroupMutation<CreateSharedPolicyGroupPayload, SharedPolicyGroup>(createSharedPolicyGroup);
}

export function useUpdateSharedPolicyGroup() {
    return useSharedPolicyGroupMutation<{ id: string; payload: UpdateSharedPolicyGroupPayload }, SharedPolicyGroup>(
        (envId, { id, payload }) => updateSharedPolicyGroup(envId, id, payload),
    );
}

export function useDeleteSharedPolicyGroup() {
    return useSharedPolicyGroupMutation<string, void>(deleteSharedPolicyGroup);
}
