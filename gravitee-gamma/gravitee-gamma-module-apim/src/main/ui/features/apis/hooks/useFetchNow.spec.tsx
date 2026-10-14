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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useFetchNow } from './useFetchNow';
import { ApimApiError } from '../../../shared/api/apimClient';
import { notify } from '../../../shared/notify';
import { fetchApiDocumentationItem } from '../services/apiDocumentation';
import type { PortalNavigationPage } from '../types/apiDocumentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/apiDocumentation');
jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), warning: jest.fn(), error: jest.fn() },
}));

function queryClientWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
    return function QueryClientWrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useFetchNow', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useEnvironment).mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
    });

    it('tells every screen of the API that a fetch is running, so that none offers another meanwhile', async () => {
        let finishFetch: () => void = () => {};
        jest.mocked(fetchApiDocumentationItem).mockReturnValue(
            new Promise(resolve => {
                finishFetch = () => resolve({ summary: { succeeded: 1, failed: 0, results: [] } });
            }),
        );
        const { result } = renderHook(() => ({ dialog: useFetchNow('api-1'), tree: useFetchNow('api-1'), other: useFetchNow('api-2') }), {
            wrapper: queryClientWrapper(),
        });

        act(() => void result.current.dialog.fetchNow({ id: 'nav-1', title: 'Reference' }));

        await waitFor(() => expect(result.current.tree.isFetching).toBe(true));
        expect(result.current.other.isFetching).toBe(false);
        await act(async () => finishFetch());
        await waitFor(() => expect(result.current.tree.isFetching).toBe(false));
    });

    describe('telling the user how it went', () => {
        const PAGE: PortalNavigationPage = {
            id: 'petstore',
            organizationId: 'DEFAULT',
            environmentId: 'env-1',
            title: 'Petstore API',
            type: 'PAGE',
            area: 'TOP_NAVBAR',
            rootId: 'petstore',
            order: 0,
            published: false,
            visibility: 'PUBLIC',
            portalPageContentId: 'content-1',
            source: { type: 'github-fetcher', configuration: {} },
        };

        async function fetchNow(title: string) {
            const { result } = renderHook(() => useFetchNow('api-1'), { wrapper: queryClientWrapper() });
            await act(() => result.current.fetchNow({ id: 'nav-1', title }));
        }

        it('says the content of a page was fetched', async () => {
            jest.mocked(fetchApiDocumentationItem).mockResolvedValue({ item: PAGE });

            await fetchNow('Petstore API');

            expect(notify.success).toHaveBeenCalledWith("Content of 'Petstore API' fetched");
        });

        it('names the files of a folder that could not be fetched', async () => {
            jest.mocked(fetchApiDocumentationItem).mockResolvedValue({
                summary: { succeeded: 1, failed: 1, results: [{ title: 'errors.md', success: false, error: '404 Not Found' }] },
            });

            await fetchNow('Reference');

            expect(notify.warning).toHaveBeenCalledWith('1 of 2 pages fetched. Failed: errors.md (404 Not Found).');
        });

        it('says why a page could not be fetched', async () => {
            jest.mocked(fetchApiDocumentationItem).mockResolvedValue({
                item: { ...PAGE, source: { ...PAGE.source!, lastFetchError: '404 Not Found' } },
            });

            await fetchNow('Petstore API');

            expect(notify.error).toHaveBeenCalledWith("'Petstore API' could not be fetched: 404 Not Found");
        });

        it('reports why the server refused the fetch', async () => {
            const serverError = new ApimApiError(400, 'No sourced page below this folder');
            jest.mocked(fetchApiDocumentationItem).mockRejectedValue(serverError);

            await fetchNow('Reference');

            expect(notify.error).toHaveBeenCalledWith(serverError, "Failed to fetch 'Reference'");
        });
    });
});
