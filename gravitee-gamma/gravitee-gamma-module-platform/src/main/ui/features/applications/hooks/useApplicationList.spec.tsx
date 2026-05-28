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

import { useApplicationList } from './useApplicationList';
import { listApplications } from '../services/applicationList';
import { applicationListKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../services/applicationList', () => ({ listApplications: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockListApplications = jest.mocked(listApplications);

const MOCK_RESPONSE = {
    data: [],
    page: { current: 1, size: 0, per_page: 25, total_pages: 0, total_elements: 0 },
};

function createTestContext() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { queryClient, Wrapper };
}

describe('useApplicationList', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockListApplications.mockResolvedValue(MOCK_RESPONSE);
    });

    afterEach(() => jest.clearAllMocks());

    it('does not call the API when the environment is unavailable', () => {
        mockUseEnvironment.mockReturnValue(null);

        const { Wrapper } = createTestContext();
        renderHook(() => useApplicationList({ query: '', status: 'ACTIVE', page: 1, perPage: 25, order: 'name' }), {
            wrapper: Wrapper,
        });

        expect(mockListApplications).not.toHaveBeenCalled();
    });

    it('calls listApplications with env id, page, and perPage', async () => {
        const { Wrapper } = createTestContext();
        renderHook(() => useApplicationList({ query: 'billing', status: 'ARCHIVED', page: 2, perPage: 50, order: '-updated_at' }), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(mockListApplications).toHaveBeenCalledTimes(1));
        expect(mockListApplications).toHaveBeenCalledWith('env-1', {
            query: 'billing',
            page: 2,
            size: 50,
            status: 'ARCHIVED',
            order: '-updated_at',
        });
    });

    it('uses a query key scoped by env id, search, status, page, and perPage', async () => {
        const { queryClient, Wrapper } = createTestContext();
        renderHook(() => useApplicationList({ query: 'billing', status: 'ARCHIVED', page: 2, perPage: 50, order: '-updated_at' }), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(mockListApplications).toHaveBeenCalledTimes(1));

        const expectedKey = applicationListKeys.search('env-1', 'billing', 'ARCHIVED', 2, 50, '-updated_at');
        expect(queryClient.getQueryCache().find({ queryKey: expectedKey })).toBeDefined();
    });

    it('maps an empty query string to undefined in the API request', async () => {
        const { Wrapper } = createTestContext();
        renderHook(() => useApplicationList({ query: '', status: 'ACTIVE', page: 1, perPage: 25, order: 'name' }), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(mockListApplications).toHaveBeenCalledTimes(1));
        expect(mockListApplications).toHaveBeenCalledWith('env-1', {
            query: undefined,
            page: 1,
            size: 25,
            status: 'ACTIVE',
            order: 'name',
        });
    });
});
