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

import { useUpdateApiProductResources } from './useUpdateApiProductResources';
import { updateApiProductResources } from '../services/apiProduct';
import { apiProductKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/apiProduct', () => ({ updateApiProductResources: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUpdateApiProductResources = jest.mocked(updateApiProductResources);

const UPDATED = {
    id: 'product-1',
    name: 'Payments',
    version: '1.0',
    resources: [{ name: 'cache-1', type: 'cache', enabled: true, configuration: {} }],
};

function renderHookUnderTest() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries');
    const setQueryData = jest.spyOn(queryClient, 'setQueryData');
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const { result } = renderHook(() => useUpdateApiProductResources('product-1'), { wrapper });
    return { result, queryClient, invalidateQueries, setQueryData };
}

describe('useUpdateApiProductResources', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
        mockUpdateApiProductResources.mockResolvedValue(UPDATED);
    });

    it('invalidates product detail and list queries after a successful save without caching the PUT body', async () => {
        const { result, invalidateQueries, setQueryData } = renderHookUnderTest();

        await act(async () => {
            await result.current.mutateAsync(() => UPDATED.resources!);
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: apiProductKeys.detail('DEFAULT', 'product-1') });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: apiProductKeys.all });
        expect(setQueryData).not.toHaveBeenCalled();
    });
});
