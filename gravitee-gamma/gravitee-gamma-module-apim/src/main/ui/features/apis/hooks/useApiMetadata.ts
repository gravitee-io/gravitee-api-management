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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { createApiMetadata, deleteApiMetadata, searchApiMetadata, updateApiMetadata } from '../services/apiMetadata';
import type { NewApiMetadataPayload, SearchApiMetadataParams, UpdateApiMetadataPayload } from '../types/metadata';
import { apiMetadataKeys } from '../utils/queryKeys';

export function useApiMetadata(apiId: string | undefined, params: SearchApiMetadataParams, enabled = true) {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useQuery({
        queryKey: apiMetadataKeys.list(envId, apiId ?? '', params),
        queryFn: () => searchApiMetadata(envId, apiId!, params),
        enabled: Boolean(env && apiId && enabled),
    });
}

function useApiMetadataMutation<TData>(apiId: string, mutationFn: (envId: string, apiId: string, data: TData) => Promise<unknown>) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: TData) => mutationFn(env!.id, apiId, data),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: apiMetadataKeys.all });
        },
    });
}

export function useCreateApiMetadata(apiId: string) {
    return useApiMetadataMutation<NewApiMetadataPayload>(apiId, createApiMetadata);
}

export function useUpdateApiMetadata(apiId: string) {
    return useApiMetadataMutation<UpdateApiMetadataPayload>(apiId, updateApiMetadata);
}

export function useDeleteApiMetadata(apiId: string) {
    return useApiMetadataMutation<string>(apiId, deleteApiMetadata);
}
