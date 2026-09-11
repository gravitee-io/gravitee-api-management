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

import {
    createClientRegistrationProvider,
    deleteClientRegistrationProvider,
    updateClientRegistrationProvider,
} from '../services/clientRegistrationProviders';
import type { ClientRegistrationProviderWrite } from '../types/clientRegistrationProvider';
import { clientRegistrationProviderKeys } from '../utils/queryKeys';

export function useCreateClientRegistrationProvider() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (provider: ClientRegistrationProviderWrite) => createClientRegistrationProvider(env!.id, provider),
        onSuccess: async saved => {
            queryClient.setQueryData(clientRegistrationProviderKeys.detail(env!.id, saved.id), saved);
            await queryClient.invalidateQueries({ queryKey: clientRegistrationProviderKeys.list(env!.id) });
        },
    });
}

export function useUpdateClientRegistrationProvider() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ providerId, provider }: { providerId: string; provider: ClientRegistrationProviderWrite }) =>
            updateClientRegistrationProvider(env!.id, providerId, provider),
        onSuccess: async saved => {
            queryClient.setQueryData(clientRegistrationProviderKeys.detail(env!.id, saved.id), saved);
            await queryClient.invalidateQueries({ queryKey: clientRegistrationProviderKeys.list(env!.id) });
        },
    });
}

export function useDeleteClientRegistrationProvider() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (providerId: string) => deleteClientRegistrationProvider(env!.id, providerId),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: clientRegistrationProviderKeys.all });
        },
    });
}
