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
import { type QueryClient, useMutation, useQueryClient } from '@tanstack/react-query';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { createCredential, deleteCredential, rotateCredential, updateCredential } from '../services/credentials';
import type { CreateCredentialPayload, Credential, RotateCredentialPayload, UpdateCredentialPayload } from '../types/credential';
import { credentialKeys } from '../utils/queryKeys';

type Refresh<TData> = (queryClient: QueryClient, envId: string, data: TData) => Promise<void>;

const refreshAll: Refresh<unknown> = queryClient => queryClient.invalidateQueries({ queryKey: credentialKeys.all });

function useCredentialMutation<TData, TResult>(
    mutationFn: (envId: string, data: TData) => Promise<TResult>,
    refreshAfterSuccess: Refresh<TData> = refreshAll,
) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: TData) => {
            if (!env?.id) {
                return Promise.reject(new Error('No active environment'));
            }
            return mutationFn(env.id, data);
        },
        // A 5xx can come after the write landed: a repeat would create a duplicate or rotate twice.
        retry: false,
        onSuccess: (_result, data) => refreshAfterSuccess(queryClient, env?.id ?? '', data),
        onError: () => refreshAll(queryClient, env?.id ?? '', undefined),
    });
}

export function useCreateCredential() {
    return useCredentialMutation<CreateCredentialPayload, Credential>(createCredential);
}

export function useUpdateCredential() {
    return useCredentialMutation<{ id: string; payload: UpdateCredentialPayload }, Credential>((envId, { id, payload }) =>
        updateCredential(envId, id, payload),
    );
}

export function useRotateCredential() {
    return useCredentialMutation<{ id: string; payload: RotateCredentialPayload }, Credential>((envId, { id, payload }) =>
        rotateCredential(envId, id, payload),
    );
}

export function useDeleteCredential() {
    // Reloading the deleted credential would only fetch a 404 while the page navigates away.
    return useCredentialMutation<string, void>(deleteCredential, (queryClient, envId, id) => {
        queryClient.removeQueries({ queryKey: credentialKeys.detail(envId, id) });
        queryClient.removeQueries({ queryKey: credentialKeys.consumers(envId, id) });
        return queryClient.invalidateQueries({ queryKey: credentialKeys.lists() });
    });
}
