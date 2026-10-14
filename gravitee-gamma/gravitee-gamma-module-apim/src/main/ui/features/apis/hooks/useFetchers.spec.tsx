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
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useFetchers } from './useFetchers';
import { listFetchers } from '../services/fetchers';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/fetchers');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockListFetchers = jest.mocked(listFetchers);

const HTTP_FETCHER = { id: 'http-fetcher', name: 'HTTP', schema: { type: 'object' as const, properties: {} } };

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

function renderUseFetchers(options?: { forImport?: boolean }) {
    return renderHook(() => useFetchers(options), { wrapper: createWrapper() });
}

describe('useFetchers', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockListFetchers.mockResolvedValue([HTTP_FETCHER]);
    });

    it('loads the fetchers installed in the current environment', async () => {
        const { result } = renderUseFetchers();

        await waitFor(() => expect(result.current.data).toEqual([HTTP_FETCHER]));
        expect(mockListFetchers).toHaveBeenCalledWith('env-1', { forImport: false });
    });

    it('keeps the fetchers for an import apart from the full list', async () => {
        mockListFetchers.mockImplementation(async (_envId, options) => (options?.forImport ? [] : [HTTP_FETCHER]));

        const { result } = renderHook(() => ({ all: useFetchers(), forImport: useFetchers({ forImport: true }) }), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.forImport.isSuccess && result.current.all.isSuccess).toBe(true));
        expect(result.current.all.data).toEqual([HTTP_FETCHER]);
        expect(result.current.forImport.data).toEqual([]);
    });

    it('loads only the fetchers that can list a folder for an import', async () => {
        const { result } = renderUseFetchers({ forImport: true });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(mockListFetchers).toHaveBeenCalledWith('env-1', { forImport: true });
    });

    it('does not load without an environment', () => {
        mockUseEnvironment.mockReturnValue(undefined as unknown as ReturnType<typeof useEnvironment>);

        renderUseFetchers();

        expect(mockListFetchers).not.toHaveBeenCalled();
    });
});
