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

import { createCredential, deleteCredential, rotateCredential, updateCredential } from '../services/credentials';
import type { CreateCredentialPayload, Credential, RotateCredentialPayload, UpdateCredentialPayload } from '../types/credential';
import { credentialKeys } from '../utils/queryKeys';

function useCredentialMutation<TData, TResult>(mutationFn: (envId: string, data: TData) => Promise<TResult>) {
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
        onSettled: () => queryClient.invalidateQueries({ queryKey: credentialKeys.all }),
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
    return useCredentialMutation<string, void>(deleteCredential);
}
