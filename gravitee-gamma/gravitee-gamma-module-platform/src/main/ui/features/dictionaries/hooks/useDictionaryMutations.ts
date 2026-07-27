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
import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';

import {
    createEnvironmentDictionary,
    deleteEnvironmentDictionary,
    deployEnvironmentDictionary,
    startEnvironmentDictionary,
    stopEnvironmentDictionary,
    updateEnvironmentDictionary,
} from '../services/dictionaries';
import type { Dictionary, DictionaryListItem, NewDictionaryPayload, UpdateDictionaryPayload } from '../types/dictionary';
import { dictionaryKeys } from '../utils/queryKeys';

function toListItemFromDictionary(dictionary: Dictionary): DictionaryListItem {
    const providerType =
        dictionary.provider && typeof dictionary.provider === 'object' ? dictionary.provider.type : (dictionary.provider ?? undefined);

    return {
        id: dictionary.id,
        key: dictionary.key,
        name: dictionary.name,
        description: dictionary.description,
        type: dictionary.type,
        state: dictionary.state,
        provider: providerType,
        properties: dictionary.properties ? Object.keys(dictionary.properties).length : 0,
        created_at: dictionary.created_at,
        updated_at: dictionary.updated_at,
        deployed_at: dictionary.deployed_at,
    };
}

function updateDictionaryListCache(previous: DictionaryListItem[] | undefined, dictionary: Dictionary): DictionaryListItem[] | undefined {
    if (!previous) return previous;
    const nextItem = toListItemFromDictionary(dictionary);
    const index = previous.findIndex(item => item.id === dictionary.id);
    if (index === -1) {
        return [nextItem, ...previous];
    }
    return previous.map(item => {
        if (item.id === dictionary.id) {
            return { ...item, ...nextItem };
        }
        return item;
    });
}

/** Apply mutation response to detail + list caches so timestamps/state refresh immediately. */
function syncDictionaryCaches(queryClient: QueryClient, envId: string, dictionary: Dictionary) {
    queryClient.setQueryData(dictionaryKeys.detail(envId, dictionary.id), dictionary);
    queryClient.setQueryData<DictionaryListItem[]>(dictionaryKeys.list(envId), previous => updateDictionaryListCache(previous, dictionary));
}

function removeDictionaryFromCaches(queryClient: QueryClient, envId: string, dictionaryId: string) {
    queryClient.removeQueries({ queryKey: dictionaryKeys.detail(envId, dictionaryId) });
    queryClient.setQueryData<DictionaryListItem[]>(dictionaryKeys.list(envId), previous =>
        previous ? previous.filter(item => item.id !== dictionaryId) : previous,
    );
}

async function invalidateDictionaryCaches(queryClient: QueryClient) {
    await queryClient.invalidateQueries({ queryKey: dictionaryKeys.all });
}

function useDictionaryMutation<TVariables>(mutationFn: (envId: string, data: TVariables) => Promise<Dictionary>) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: TVariables) => mutationFn(env!.id, data),
        onSuccess: async (dictionary: Dictionary) => {
            if (env?.id) {
                syncDictionaryCaches(queryClient, env.id, dictionary);
            }
            await invalidateDictionaryCaches(queryClient);
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
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (dictionaryId: string) => deleteEnvironmentDictionary(env!.id, dictionaryId),
        onSuccess: async (_result, dictionaryId) => {
            if (env?.id) {
                removeDictionaryFromCaches(queryClient, env.id, dictionaryId);
            }
            await invalidateDictionaryCaches(queryClient);
        },
    });
}

export function useDeployDictionary() {
    return useDictionaryMutation<string>(deployEnvironmentDictionary);
}

export function useStartDictionary() {
    return useDictionaryMutation<string>(startEnvironmentDictionary);
}

export function useStopDictionary() {
    return useDictionaryMutation<string>(stopEnvironmentDictionary);
}
