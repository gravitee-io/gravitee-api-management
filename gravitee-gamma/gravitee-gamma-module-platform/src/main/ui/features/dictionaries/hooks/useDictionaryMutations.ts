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
    createEnvironmentDictionary,
    deleteEnvironmentDictionary,
    deployEnvironmentDictionary,
    startEnvironmentDictionary,
    stopEnvironmentDictionary,
    undeployEnvironmentDictionary,
    updateEnvironmentDictionary,
} from '../services/dictionaries';
import type { NewDictionaryPayload, UpdateDictionaryPayload } from '../types/dictionary';
import { dictionaryKeys } from '../utils/queryKeys';

function useDictionaryMutation<TData>(mutationFn: (envId: string, data: TData) => Promise<unknown>) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: TData) => mutationFn(env!.id, data),
        onSuccess: () => {
            if (env?.id) {
                void queryClient.invalidateQueries({ queryKey: dictionaryKeys.list(env.id) });
            }
        },
    });
}

export function useCreateDictionary() {
    return useDictionaryMutation<NewDictionaryPayload>(createEnvironmentDictionary);
}

export function useUpdateDictionary() {
    return useDictionaryMutation<{ dictionaryId: string; data: UpdateDictionaryPayload }>((envId, { dictionaryId, data }) =>
        updateEnvironmentDictionary(envId, dictionaryId, data),
    );
}

export function useDeleteDictionary() {
    return useDictionaryMutation<string>(deleteEnvironmentDictionary);
}

export function useDeployDictionary() {
    return useDictionaryMutation<string>(deployEnvironmentDictionary);
}

export function useUndeployDictionary() {
    return useDictionaryMutation<string>(undeployEnvironmentDictionary);
}

export function useStartDictionary() {
    return useDictionaryMutation<string>(startEnvironmentDictionary);
}

export function useStopDictionary() {
    return useDictionaryMutation<string>(stopEnvironmentDictionary);
}
