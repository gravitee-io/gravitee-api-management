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
import { focusManager, onlineManager, QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import {
    useApiDocumentation,
    useApiDocumentationPageContent,
    useChangeApiDocumentationItem,
    useApiPublishLocations,
    useCreateApiDocumentationItem,
    useDeleteApiDocumentationItem,
    useImportApiDocumentation,
    usePublishApiToPortal,
    useSaveApiDocumentationPageContent,
    useUnpublishApiFromPortal,
    useUpdateApiDocumentationItem,
} from './useApiDocumentation';
import { ApimApiError } from '../../../shared/api/apimClient';
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
import type {
    CreateApiDocumentationItem,
    ImportPortalNavigationRequest,
    PortalNavigationPage,
    UpdateApiDocumentationItem,
} from '../types/apiDocumentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/apiDocumentation');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockListApiDocumentation = jest.mocked(listApiDocumentation);
const mockListApiPublishLocations = jest.mocked(listApiPublishLocations);
const mockCreateApiDocumentationItem = jest.mocked(createApiDocumentationItem);
const mockUpdateApiDocumentationItem = jest.mocked(updateApiDocumentationItem);
const mockDeleteApiDocumentationItem = jest.mocked(deleteApiDocumentationItem);
const mockImportApiDocumentation = jest.mocked(importApiDocumentation);
const mockPublishApiToPortal = jest.mocked(publishApiToPortal);
const mockUnpublishApiFromPortal = jest.mocked(unpublishApiFromPortal);
const mockSaveApiDocumentationPageContent = jest.mocked(saveApiDocumentationPageContent);
const mockGetApiDocumentationPageContent = jest.mocked(getApiDocumentationPageContent);

const API_DOCUMENTATION_LIST = { queryKey: ['api-documentation', 'env-1', 'api-1', 'list'] };

const CREATE_PAYLOAD: CreateApiDocumentationItem = { type: 'FOLDER', title: 'Guides', area: 'TOP_NAVBAR', visibility: 'PUBLIC' };
const UPDATE_PAYLOAD: UpdateApiDocumentationItem = { type: 'FOLDER', title: 'Guides', order: 0, published: true, visibility: 'PUBLIC' };
const IMPORT_REQUEST: ImportPortalNavigationRequest = {
    title: 'Docs',
    source: { type: 'github-fetcher', configuration: { repository: 'docs' } },
};

function renderWithQueryClient<T>(hook: () => T) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries');
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const { result } = renderHook(hook, { wrapper });
    return { result, invalidateQueries, queryClient };
}

describe('useApiDocumentation hooks', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockListApiDocumentation.mockResolvedValue({ items: [], publications: [] });
        mockListApiPublishLocations.mockResolvedValue({ data: [] });
    });

    describe('useApiDocumentation', () => {
        it('loads the documentation of the API in the current environment', async () => {
            const { result } = renderWithQueryClient(() => useApiDocumentation('api-1'));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(mockListApiDocumentation).toHaveBeenCalledWith('env-1', 'api-1');
            expect(result.current.data).toEqual({ items: [], publications: [] });
        });

        it('does not load without an API id', () => {
            const { result } = renderWithQueryClient(() => useApiDocumentation(undefined));

            expect(result.current.fetchStatus).toBe('idle');
            expect(mockListApiDocumentation).not.toHaveBeenCalled();
        });

        it('does not load without an environment', () => {
            mockUseEnvironment.mockReturnValue(undefined as unknown as ReturnType<typeof useEnvironment>);
            const { result } = renderWithQueryClient(() => useApiDocumentation('api-1'));

            expect(result.current.fetchStatus).toBe('idle');
            expect(mockListApiDocumentation).not.toHaveBeenCalled();
        });
    });

    describe('useApiDocumentationPageContent', () => {
        it('loads the content of the page in the current environment', async () => {
            const content = { id: 'content-1', type: 'GRAVITEE_MARKDOWN' as const, content: '# Hello' };
            mockGetApiDocumentationPageContent.mockResolvedValue(content);
            const { result } = renderWithQueryClient(() => useApiDocumentationPageContent('api-1', 'nav-1'));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(mockGetApiDocumentationPageContent).toHaveBeenCalledWith('env-1', 'api-1', 'nav-1');
            expect(result.current.data).toEqual(content);
        });

        it('is not loaded again when the window regains focus or the browser comes back online', async () => {
            mockGetApiDocumentationPageContent.mockResolvedValue({ id: 'content-1', type: 'GRAVITEE_MARKDOWN', content: '# Hello' });
            const { result } = renderWithQueryClient(() => useApiDocumentationPageContent('api-1', 'nav-1'));
            await waitFor(() => expect(result.current.isSuccess).toBe(true));

            try {
                await act(async () => {
                    focusManager.setFocused(false);
                    focusManager.setFocused(true);
                    onlineManager.setOnline(false);
                    onlineManager.setOnline(true);
                });
            } finally {
                focusManager.setFocused(undefined);
                onlineManager.setOnline(true);
            }

            expect(mockGetApiDocumentationPageContent).toHaveBeenCalledTimes(1);
        });

        it('does not load until it is enabled', () => {
            const { result } = renderWithQueryClient(() => useApiDocumentationPageContent('api-1', 'nav-1', false));

            expect(result.current.fetchStatus).toBe('idle');
            expect(mockGetApiDocumentationPageContent).not.toHaveBeenCalled();
        });
    });

    describe('useApiPublishLocations', () => {
        it('loads the sections the API can be published to', async () => {
            const { result } = renderWithQueryClient(() => useApiPublishLocations('api-1'));

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(mockListApiPublishLocations).toHaveBeenCalledWith('env-1', 'api-1');
        });

        it('does not load until it is enabled', () => {
            const { result } = renderWithQueryClient(() => useApiPublishLocations('api-1', false));

            expect(result.current.fetchStatus).toBe('idle');
            expect(mockListApiPublishLocations).not.toHaveBeenCalled();
        });
    });

    describe.each([
        {
            name: 'useCreateApiDocumentationItem',
            render: () => renderWithQueryClient(() => useCreateApiDocumentationItem('api-1')),
            variables: CREATE_PAYLOAD,
            service: mockCreateApiDocumentationItem,
            expectedCall: ['env-1', 'api-1', CREATE_PAYLOAD],
        },
        {
            name: 'useUpdateApiDocumentationItem',
            render: () => renderWithQueryClient(() => useUpdateApiDocumentationItem('api-1')),
            variables: { navId: 'nav-1', payload: UPDATE_PAYLOAD, propagatePublishToChildren: true },
            service: mockUpdateApiDocumentationItem,
            expectedCall: ['env-1', 'api-1', 'nav-1', UPDATE_PAYLOAD, { propagatePublishToChildren: true }],
        },
        {
            name: 'useDeleteApiDocumentationItem',
            render: () => renderWithQueryClient(() => useDeleteApiDocumentationItem('api-1')),
            variables: 'nav-1',
            service: mockDeleteApiDocumentationItem,
            expectedCall: ['env-1', 'api-1', 'nav-1'],
        },
        {
            name: 'useImportApiDocumentation',
            render: () => renderWithQueryClient(() => useImportApiDocumentation('api-1')),
            variables: IMPORT_REQUEST,
            service: mockImportApiDocumentation,
            expectedCall: ['env-1', 'api-1', IMPORT_REQUEST],
        },
        {
            name: 'usePublishApiToPortal',
            render: () => renderWithQueryClient(() => usePublishApiToPortal('api-1')),
            variables: { sectionId: 'section-1' },
            service: mockPublishApiToPortal,
            expectedCall: ['env-1', 'api-1', { sectionId: 'section-1' }],
        },
        {
            name: 'useUnpublishApiFromPortal',
            render: () => renderWithQueryClient(() => useUnpublishApiFromPortal('api-1')),
            variables: undefined,
            service: mockUnpublishApiFromPortal,
            expectedCall: ['env-1', 'api-1'],
        },
    ])('$name', ({ render, variables, service, expectedCall }) => {
        // The hooks differ only in their variables, so the shared cases drive them through a common signature.
        type AnyMutation = { mutateAsync: (variables: unknown) => Promise<unknown>; error: unknown };

        it('calls the service in the current environment', async () => {
            service.mockResolvedValue(undefined as never);
            const { result } = render();

            await act(() => (result.current as AnyMutation).mutateAsync(variables));

            expect(service).toHaveBeenCalledWith(...expectedCall);
        });

        it("refreshes only this API's documentation once it succeeds", async () => {
            service.mockResolvedValue(undefined as never);
            const { result, invalidateQueries } = render();

            await act(() => (result.current as AnyMutation).mutateAsync(variables));

            expect(invalidateQueries).toHaveBeenCalledTimes(1);
            expect(invalidateQueries).toHaveBeenCalledWith(API_DOCUMENTATION_LIST);
        });

        it('exposes the server error unchanged and refreshes nothing when it fails', async () => {
            const serverError = new ApimApiError(400, 'The item is fetched from an external source and cannot be deleted');
            service.mockRejectedValue(serverError);
            const { result, invalidateQueries } = render();

            await act(async () => {
                await expect((result.current as AnyMutation).mutateAsync(variables)).rejects.toBe(serverError);
            });

            await waitFor(() => expect((result.current as AnyMutation).error).toBe(serverError));
            expect(invalidateQueries).not.toHaveBeenCalled();
        });
    });

    describe('useSaveApiDocumentationPageContent', () => {
        it('saves the content of the page in the current environment', async () => {
            mockSaveApiDocumentationPageContent.mockResolvedValue({ id: 'content-1', type: 'GRAVITEE_MARKDOWN', content: '# Hello' });
            const { result } = renderWithQueryClient(() => useSaveApiDocumentationPageContent('api-1'));

            await act(() => result.current.mutateAsync({ navId: 'nav-1', content: '# Hello' }));

            expect(mockSaveApiDocumentationPageContent).toHaveBeenCalledWith('env-1', 'api-1', 'nav-1', { content: '# Hello' });
        });

        it('leaves the documentation list alone, since it does not carry content', async () => {
            mockSaveApiDocumentationPageContent.mockResolvedValue({ id: 'content-1', type: 'GRAVITEE_MARKDOWN', content: '# Hello' });
            const { result, invalidateQueries } = renderWithQueryClient(() => useSaveApiDocumentationPageContent('api-1'));

            await act(() => result.current.mutateAsync({ navId: 'nav-1', content: '# Hello' }));

            expect(invalidateQueries).not.toHaveBeenCalled();
        });

        it('keeps the saved content as the content of the page, without loading it again', async () => {
            const saved = { id: 'content-1', type: 'GRAVITEE_MARKDOWN' as const, content: '# Hello again' };
            mockSaveApiDocumentationPageContent.mockResolvedValue(saved);
            const { result, queryClient } = renderWithQueryClient(() => useSaveApiDocumentationPageContent('api-1'));

            await act(() => result.current.mutateAsync({ navId: 'nav-1', content: '# Hello again' }));

            expect(queryClient.getQueryData(['api-documentation', 'env-1', 'api-1', 'content', 'nav-1'])).toEqual(saved);
        });
    });

    describe('useChangeApiDocumentationItem', () => {
        const PAGE: PortalNavigationPage = {
            id: 'nav-1',
            organizationId: 'DEFAULT',
            environmentId: 'env-1',
            title: 'Petstore',
            type: 'PAGE',
            area: 'TOP_NAVBAR',
            parentId: 'guides',
            rootId: 'guides',
            order: 3,
            published: true,
            visibility: 'PUBLIC',
            portalPageContentId: 'content-1',
        };
        const SOURCE = { type: 'github-fetcher', configuration: { repository: 'docs' } };

        it('builds the update from the item as the server has it now, changing only what was asked', async () => {
            // The list loaded earlier predates the page being linked to a source and moved.
            const { result, queryClient } = renderWithQueryClient(() => useChangeApiDocumentationItem('api-1'));
            queryClient.setQueryData(API_DOCUMENTATION_LIST.queryKey, { items: [PAGE], publications: [] });
            mockListApiDocumentation.mockResolvedValue({ items: [{ ...PAGE, order: 0, source: SOURCE }], publications: [] });
            mockUpdateApiDocumentationItem.mockResolvedValue(PAGE);

            await act(() => result.current.mutateAsync({ navId: 'nav-1', changes: { title: 'Pet store' } }));

            expect(mockUpdateApiDocumentationItem).toHaveBeenCalledWith('env-1', 'api-1', 'nav-1', {
                type: 'PAGE',
                title: 'Pet store',
                order: 0,
                published: true,
                visibility: 'PUBLIC',
                parentId: 'guides',
                source: SOURCE,
            });
        });

        it("refreshes this API's documentation once it succeeds", async () => {
            mockListApiDocumentation.mockResolvedValue({ items: [PAGE], publications: [] });
            mockUpdateApiDocumentationItem.mockResolvedValue(PAGE);
            const { result, invalidateQueries } = renderWithQueryClient(() => useChangeApiDocumentationItem('api-1'));

            await act(() => result.current.mutateAsync({ navId: 'nav-1', changes: { visibility: 'PRIVATE' } }));

            expect(invalidateQueries).toHaveBeenCalledWith(API_DOCUMENTATION_LIST);
        });

        it('updates nothing when the item no longer exists', async () => {
            mockListApiDocumentation.mockResolvedValue({ items: [], publications: [] });
            const { result } = renderWithQueryClient(() => useChangeApiDocumentationItem('api-1'));

            await act(async () => {
                await expect(result.current.mutateAsync({ navId: 'nav-1', changes: { title: 'Pet store' } })).rejects.toThrow(
                    'This item no longer exists in the documentation of this API.',
                );
            });

            expect(mockUpdateApiDocumentationItem).not.toHaveBeenCalled();
        });

        it('exposes the server error unchanged when the update is refused', async () => {
            const serverError = new ApimApiError(400, 'A sibling item already uses the title "Pet store"');
            mockListApiDocumentation.mockResolvedValue({ items: [PAGE], publications: [] });
            mockUpdateApiDocumentationItem.mockRejectedValue(serverError);
            const { result } = renderWithQueryClient(() => useChangeApiDocumentationItem('api-1'));

            await act(async () => {
                await expect(result.current.mutateAsync({ navId: 'nav-1', changes: { title: 'Pet store' } })).rejects.toBe(serverError);
            });
        });
    });
});
