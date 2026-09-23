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
    createDocumentationPage,
    deleteDocumentationPage,
    fetchDocumentationPage,
    getAllApiPages,
    getApiPage,
    getApiPages,
    getSpecGenState,
    listFetchers,
    listPortalDocumentationFolders,
    placeApiInPortalFolder,
    publishApiWithDefaultOverview,
    publishDocumentationPage,
    startSpecGen,
    syncPublishedPagesToPortalNavigation,
    unpublishApiFromPortalNavigation,
    unpublishDocumentationPage,
    unpublishPagesFromPortalNavigation,
    updateDocumentationPage,
} from '../services/documentation';
import type { ApiPortalPlacement, CreateDocumentationPayload, DocumentationPage, EditDocumentationPayload, PortalFolderOption } from '../types/documentation';
import { apiDocumentationKeys } from '../utils/queryKeys';

export function useApiDocumentationPages(apiId: string | undefined, parentId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && apiId);

    const query = useQuery({
        queryKey: apiDocumentationKeys.list(envId, apiId ?? '', parentId),
        queryFn: () => getApiPages(envId, apiId!, parentId),
        enabled,
    });

    const pages = (query.data?.pages ?? []).filter(page => !page.homepage);
    const breadcrumbs = query.data?.breadcrumb ?? [];

    return {
        pages,
        breadcrumbs,
        isLoading: query.isLoading,
        isError: query.isError,
        error: query.error,
        refetch: query.refetch,
    };
}

export function useApiDocumentationTree(apiId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && apiId);

    const query = useQuery({
        queryKey: apiDocumentationKeys.tree(envId, apiId ?? ''),
        queryFn: () => getAllApiPages(envId, apiId!),
        enabled,
    });

    const pages = (query.data?.pages ?? []).filter(page => !page.homepage);

    return {
        pages,
        isLoading: query.isLoading,
        isError: query.isError,
        error: query.error,
        refetch: query.refetch,
    };
}

export function usePortalDocumentationFolders(apiId: string | undefined, enabled: boolean) {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useQuery({
        queryKey: apiDocumentationKeys.portalFolders(envId),
        queryFn: () => listPortalDocumentationFolders(envId, apiId!),
        enabled: Boolean(env && apiId && enabled),
    });
}

export function usePlaceApiInPortalFolder(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ title, folder }: { title: string; folder: PortalFolderOption }) =>
            placeApiInPortalFolder(envId, apiId, title, folder),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.all });
        },
    });
}

export function useSyncDocumentationToPortal(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({
            title,
            folder,
            placement,
            pages,
            pageIds,
        }: {
            title: string;
            folder: PortalFolderOption;
            placement: ApiPortalPlacement | null;
            pages: DocumentationPage[];
            pageIds: string[];
        }) => syncPublishedPagesToPortalNavigation(envId, apiId, title, folder, placement, pages, pageIds),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.all });
        },
    });
}

export function usePublishApiWithDefaultOverview(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({
            title,
            folder,
            placement,
        }: {
            title: string;
            folder: PortalFolderOption;
            placement: ApiPortalPlacement | null;
        }) => publishApiWithDefaultOverview(envId, apiId, title, folder, placement),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.all });
        },
    });
}

export function useUnpublishDocumentationFromPortal(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({
            placement,
            pages,
            pageIds,
        }: {
            placement: ApiPortalPlacement | null;
            pages: DocumentationPage[];
            pageIds: string[];
        }) => unpublishPagesFromPortalNavigation(envId, apiId, placement, pages, pageIds),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.all });
        },
    });
}

export function useUnpublishApiFromPortal(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({
            placement,
            pages,
        }: {
            placement: ApiPortalPlacement | null;
            pages: DocumentationPage[];
        }) => unpublishApiFromPortalNavigation(envId, apiId, placement, pages),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.all });
        },
    });
}

export function useApiDocumentationPage(apiId: string | undefined, pageId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && apiId && pageId);

    return useQuery({
        queryKey: apiDocumentationKeys.detail(envId, apiId ?? '', pageId ?? ''),
        queryFn: () => getApiPage(envId, apiId!, pageId!),
        enabled,
    });
}

export function useDocumentationFetchers() {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useQuery({
        queryKey: apiDocumentationKeys.fetchers(envId),
        queryFn: () => listFetchers(envId),
        enabled: Boolean(env),
        staleTime: 5 * 60_000,
    });
}

export function useSpecGenState(apiId: string | undefined, enabled: boolean) {
    const env = useEnvironment();
    const envId = env?.id ?? '';

    return useQuery({
        queryKey: apiDocumentationKeys.specGen(envId, apiId ?? ''),
        queryFn: () => getSpecGenState(envId, apiId!),
        enabled: Boolean(env && apiId && enabled),
    });
}

function useInvalidateDocumentation(_apiId?: string) {
    const queryClient = useQueryClient();

    return () => {
        void queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.all });
    };
}

export function useCreateDocumentationPage(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const invalidate = useInvalidateDocumentation(apiId);

    return useMutation({
        mutationFn: (payload: CreateDocumentationPayload) => createDocumentationPage(envId, apiId, payload),
        onSuccess: invalidate,
    });
}

export function useUpdateDocumentationPage(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const invalidate = useInvalidateDocumentation(apiId);

    return useMutation({
        mutationFn: ({ pageId, payload }: { pageId: string; payload: EditDocumentationPayload }) =>
            updateDocumentationPage(envId, apiId, pageId, payload),
        onSuccess: invalidate,
    });
}

export function usePublishDocumentationPage(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const invalidate = useInvalidateDocumentation(apiId);

    return useMutation({
        mutationFn: (pageId: string) => publishDocumentationPage(envId, apiId, pageId),
        onSuccess: invalidate,
    });
}

export function useUnpublishDocumentationPage(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const invalidate = useInvalidateDocumentation(apiId);

    return useMutation({
        mutationFn: (pageId: string) => unpublishDocumentationPage(envId, apiId, pageId),
        onSuccess: invalidate,
    });
}

export function useFetchDocumentationPage(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const invalidate = useInvalidateDocumentation(apiId);

    return useMutation({
        mutationFn: (pageId: string) => fetchDocumentationPage(envId, apiId, pageId),
        onSuccess: invalidate,
    });
}

export function useDeleteDocumentationPage(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const invalidate = useInvalidateDocumentation(apiId);

    return useMutation({
        mutationFn: (pageId: string) => deleteDocumentationPage(envId, apiId, pageId),
        onSuccess: invalidate,
    });
}

export function useStartSpecGen(apiId: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: () => startSpecGen(envId, apiId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.specGen(envId, apiId) });
            queryClient.invalidateQueries({ queryKey: [...apiDocumentationKeys.all, 'list', envId, apiId] });
            queryClient.invalidateQueries({ queryKey: apiDocumentationKeys.tree(envId, apiId) });
        },
    });
}
