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
import type { Dictionary, NewDictionaryPayload, UpdateDictionaryPayload } from '../types/dictionary';
import { dictionaryKeys } from '../utils/queryKeys';

function useDictionaryMutation<TVariables, TResult = Dictionary>(mutationFn: (envId: string, data: TVariables) => Promise<TResult>) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: TVariables) => mutationFn(env!.id, data),
        onSuccess: async () => {
            // Prefix key invalidates both list and detail caches for all consumers.
            await queryClient.invalidateQueries({ queryKey: dictionaryKeys.all });
        },
    });
}

export function useCreateDictionary() {
    return useDictionaryMutation<NewDictionaryPayload, Dictionary>(createEnvironmentDictionary);
}

export function useUpdateDictionary() {
    return useDictionaryMutation<{ dictionaryId: string; data: UpdateDictionaryPayload }, Dictionary>((envId, { dictionaryId, data }) =>
        updateEnvironmentDictionary(envId, dictionaryId, data),
    );
}

export function useDeleteDictionary() {
    return useDictionaryMutation<string, void>(deleteEnvironmentDictionary);
}

export function useDeployDictionary() {
    return useDictionaryMutation<string, Dictionary>(deployEnvironmentDictionary);
}

export function useUndeployDictionary() {
    return useDictionaryMutation<string, Dictionary>(undeployEnvironmentDictionary);
}

export function useStartDictionary() {
    return useDictionaryMutation<string, Dictionary>(startEnvironmentDictionary);
}

export function useStopDictionary() {
    return useDictionaryMutation<string, Dictionary>(stopEnvironmentDictionary);
}
