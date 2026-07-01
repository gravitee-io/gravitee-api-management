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

import { createEnvironmentMetadata, deleteEnvironmentMetadata, updateEnvironmentMetadata } from '../services/metadata';
import type { NewMetadataPayload, UpdateMetadataPayload } from '../types/metadata';
import { metadataKeys } from '../utils/queryKeys';

export function useCreateMetadata() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: NewMetadataPayload) => createEnvironmentMetadata(env!.id, data),
        onSuccess: () => {
            if (env?.id) {
                void queryClient.invalidateQueries({ queryKey: metadataKeys.list(env.id) });
            }
        },
    });
}

export function useUpdateMetadata() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: UpdateMetadataPayload) => updateEnvironmentMetadata(env!.id, data),
        onSuccess: () => {
            if (env?.id) {
                void queryClient.invalidateQueries({ queryKey: metadataKeys.list(env.id) });
            }
        },
    });
}

export function useDeleteMetadata() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (key: string) => deleteEnvironmentMetadata(env!.id, key),
        onSuccess: () => {
            if (env?.id) {
                void queryClient.invalidateQueries({ queryKey: metadataKeys.list(env.id) });
            }
        },
    });
}
