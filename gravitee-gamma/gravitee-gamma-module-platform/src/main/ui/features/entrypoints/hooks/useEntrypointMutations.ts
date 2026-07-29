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

import { createEntrypoint, deleteEntrypoint, updateEntrypoint } from '../services/entrypoints';
import type { NewEntrypointPayload, UpdateEntrypointPayload } from '../types/entrypoint';
import { entrypointKeys } from '../utils/queryKeys';

function useEntrypointMutation<TData>(mutationFn: (data: TData) => Promise<unknown>) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn,
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: entrypointKeys.list() });
        },
    });
}

export function useCreateEntrypoint() {
    return useEntrypointMutation<NewEntrypointPayload>(createEntrypoint);
}

export function useUpdateEntrypoint() {
    return useEntrypointMutation<UpdateEntrypointPayload>(updateEntrypoint);
}

export function useDeleteEntrypoint() {
    return useEntrypointMutation<string>(deleteEntrypoint);
}
