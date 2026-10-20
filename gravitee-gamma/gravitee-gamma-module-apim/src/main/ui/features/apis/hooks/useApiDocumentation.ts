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
import { type Query, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { type DocumentationItemChanges, toUpdatePayload } from '../components/detail/documentation/toUpdatePayload';
import {
    createApiDocumentationItem,
    createApiOverviewPage,
    deleteApiDocumentationItem,
    fetchApiDocumentationItem,
    getApiDocumentationPageContent,
    importApiDocumentation,
    listApiDocumentation,
    listApiPublishLocations,
    publishApiToPortal,
    saveApiDocumentationPageConfiguration,
    saveApiDocumentationPageContent,
    unpublishApiFromPortal,
    updateApiDocumentationItem,
} from '../services/apiDocumentation';
import type { ApiPortalNavigationItemsResponse, PortalPageContent, PortalPageOpenApiConfiguration } from '../types/apiDocumentation';
import { replaceablePageIds } from '../utils/documentationTree';
import { apiDocumentationKeys } from '../utils/queryKeys';

interface ChangeApiDocumentationItemVariables {
    navId: string;
    changes: DocumentationItemChanges;
    /** When a folder is published, publishes everything inside it too. Unpublishing a folder always unpublishes its contents. */
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

/**
 * Changes some fields of an item. The update replaces the whole item, so it is built from the item as the server has it
 * at that moment rather than as the screen last loaded it, which could undo a change made since.
 */
export function useChangeApiDocumentationItem(apiId: string) {
    const queryClient = useQueryClient();

    return useApiDocumentationMutation(
        apiId,
        async (envId, id, { navId, changes, propagatePublishToChildren }: ChangeApiDocumentationItemVariables) => {
            const documentation = await queryClient.fetchQuery({
                queryKey: apiDocumentationKeys.list(envId, id),
                queryFn: () => listApiDocumentation(envId, id),
                staleTime: 0,
            });
            const item = documentation.items.find(candidate => candidate.id === navId);
            if (!item) {
                throw new Error('This item no longer exists in the documentation of this API.');
            }
            return updateApiDocumentationItem(envId, id, navId, toUpdatePayload(item, changes), { propagatePublishToChildren });
        },
    );
}

export function useDeleteApiDocumentationItem(apiId: string) {
    return useApiDocumentationMutation(apiId, deleteApiDocumentationItem);
}

/** A fetch can replace the content of one page or of every page below a folder, so all of them are reloaded. */
export function useFetchApiDocumentationItem(apiId: string) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationKey: apiDocumentationKeys.fetch(env?.id ?? '', apiId),
        mutationFn: (navId: string) => fetchApiDocumentationItem(env!.id, apiId, navId),
        // A failed fetch is recorded on the source, so the documentation is reloaded either way. The content first: the
        // list says whether a page was ever fetched, and paired with the content from before, would show its placeholder
        // as fetched content.
        onSettled: async (_result, _error, navId) => {
            const documentation = queryClient.getQueryData<ApiPortalNavigationItemsResponse>(apiDocumentationKeys.list(env!.id, apiId));
            const replaceable = new Set(replaceablePageIds(documentation?.items ?? [], navId));
            // Only the content the fetch can replace: a page written by hand may hold a draft, which a reset would lose.
            const contents = {
                queryKey: apiDocumentationKeys.contents(env!.id, apiId),
                predicate: (query: Query) => replaceable.has(String(query.queryKey.at(-1))),
            };
            // A read started before the fetch brings the content from before, and a refresh would join it rather than
            // read again: it is cancelled first.
            await queryClient.cancelQueries(contents);
            try {
                await queryClient.invalidateQueries(contents, { throwOnError: true });
            } catch {
                // Kept, the content from before would show as fetched: it is dropped, and the editor says it failed to load.
                void queryClient.resetQueries(contents);
            }
            await queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.list(env!.id, apiId) });
        },
    });
}

export function useImportApiDocumentation(apiId: string) {
    return useApiDocumentationMutation(apiId, importApiDocumentation);
}

export function usePublishApiToPortal(apiId: string) {
    return useApiDocumentationMutation(apiId, publishApiToPortal);
}

export function useCreateApiOverviewPage(apiId: string) {
    return useApiDocumentationMutation(apiId, (envId, id, _variables: void) => createApiOverviewPage(envId, id));
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

export function useSaveApiDocumentationPageConfiguration(apiId: string) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ navId, configuration }: { navId: string; configuration: PortalPageOpenApiConfiguration }) =>
            saveApiDocumentationPageConfiguration(env!.id, apiId, navId, configuration),
        // Only the configuration is taken: the content the editor compares its draft with stays the one it loaded.
        onSuccess: (saved, { navId }) => {
            queryClient.setQueryData<PortalPageContent>(apiDocumentationKeys.content(env!.id, apiId, navId), current =>
                current ? { ...current, configuration: saved.configuration } : saved,
            );
        },
    });
}
