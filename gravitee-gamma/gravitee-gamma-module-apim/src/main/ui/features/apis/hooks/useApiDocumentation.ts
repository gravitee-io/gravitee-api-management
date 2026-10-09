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

import {
    createApiDocumentationItem,
    deleteApiDocumentationItem,
    getApiDocumentationPageContent,
    importApiDocumentation,
    listApiDocumentation,
    listApiPublishLocations,
    publishApiToPortal,
    saveApiDocumentationPageContent,
    unpublishApiFromPortal,
    updateApiDocumentationItem,
} from '../services/apiDocumentation';
import type { UpdateApiDocumentationItem } from '../types/apiDocumentation';
import { apiDocumentationKeys } from '../utils/queryKeys';

export interface UpdateApiDocumentationItemVariables {
    navId: string;
    payload: UpdateApiDocumentationItem;
    propagatePublishToChildren?: boolean;
}

export function useApiDocumentation(apiId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useQuery({
        queryKey: apiDocumentationKeys.list(envId, apiId ?? ''),
        queryFn: () => listApiDocumentation(envId, apiId!),
        enabled: Boolean(env && apiId),
    });
}

export function useApiPublishLocations(apiId: string | undefined, enabled = true) {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useQuery({
        queryKey: apiDocumentationKeys.publishLocations(envId, apiId ?? ''),
        queryFn: () => listApiPublishLocations(envId, apiId!),
        enabled: Boolean(env && apiId && enabled),
    });
}

export function useApiDocumentationPageContent(apiId: string, navId: string, enabled = true) {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useQuery({
        queryKey: apiDocumentationKeys.content(envId, apiId, navId),
        queryFn: () => getApiDocumentationPageContent(envId, apiId, navId),
        enabled: Boolean(env && enabled),
        // The editor copies the content into its draft once: a reload would move the saved content under the draft.
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
    });
}

// The list holds both the items and where the API is published, so every write refreshes it and nothing else.
function useApiDocumentationMutation<TVariables, TData>(
    apiId: string,
    mutationFn: (envId: string, apiId: string, variables: TVariables) => Promise<TData>,
) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (variables: TVariables) => mutationFn(env!.id, apiId, variables),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.list(env!.id, apiId) });
        },
    });
}

export function useCreateApiDocumentationItem(apiId: string) {
    return useApiDocumentationMutation(apiId, createApiDocumentationItem);
}

export function useUpdateApiDocumentationItem(apiId: string) {
    return useApiDocumentationMutation(
        apiId,
        (envId, id, { navId, payload, propagatePublishToChildren }: UpdateApiDocumentationItemVariables) =>
            updateApiDocumentationItem(envId, id, navId, payload, { propagatePublishToChildren }),
    );
}

export function useDeleteApiDocumentationItem(apiId: string) {
    return useApiDocumentationMutation(apiId, deleteApiDocumentationItem);
}

export function useImportApiDocumentation(apiId: string) {
    return useApiDocumentationMutation(apiId, importApiDocumentation);
}

export function usePublishApiToPortal(apiId: string) {
    return useApiDocumentationMutation(apiId, publishApiToPortal);
}

export function useUnpublishApiFromPortal(apiId: string) {
    return useApiDocumentationMutation(apiId, (envId, id, _variables: void) => unpublishApiFromPortal(envId, id));
}

export function useSaveApiDocumentationPageContent(apiId: string) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ navId, content }: { navId: string; content: string }) =>
            saveApiDocumentationPageContent(env!.id, apiId, navId, { content }),
        onSuccess: (saved, { navId }) => {
            queryClient.setQueryData(apiDocumentationKeys.content(env!.id, apiId, navId), saved);
        },
    });
}
