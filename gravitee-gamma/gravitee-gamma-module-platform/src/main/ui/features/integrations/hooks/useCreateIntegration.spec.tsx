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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useCreateIntegration } from './useCreateIntegration';
import { retryTransientRequest } from '../../../shared/api/queryRetry';
import { createIntegration } from '../services/integrationCreate';
import { integrationKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));
jest.mock('../services/integrationCreate', () => ({
    createIntegration: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockCreateIntegration = jest.mocked(createIntegration);

describe('useCreateIntegration', () => {
    let queryClient: QueryClient;
    let invalidateQueries: jest.SpiedFunction<QueryClient['invalidateQueries']>;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
        invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockCreateIntegration.mockResolvedValue({ id: 'int-42', name: 'My integration', provider: 'solace' });
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it('creates the integration in the current environment and refreshes every integrations query', async () => {
        const request = { name: 'My integration', provider: 'solace', description: 'Ingests the EU gateways' };
        const { result } = renderHook(() => useCreateIntegration(), { wrapper });

        const created = await act(() => result.current.mutateAsync(request));

        expect(mockCreateIntegration).toHaveBeenCalledWith('env-1', request);
        expect(created).toEqual({ id: 'int-42', name: 'My integration', provider: 'solace' });
        expect(invalidateQueries).toHaveBeenCalledTimes(1);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: integrationKeys.all });
    });

    it('surfaces a failed creation as the mutation error and leaves the integrations queries untouched', async () => {
        const failure = new Error('Validation error');
        mockCreateIntegration.mockRejectedValue(failure);
        const { result } = renderHook(() => useCreateIntegration(), { wrapper });

        await act(async () => {
            await expect(result.current.mutateAsync({ name: 'My integration', provider: 'solace' })).rejects.toBe(failure);
        });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toBe(failure);
        expect(invalidateQueries).not.toHaveBeenCalled();
    });

    it('does not repeat a creation that failed with a transient error', async () => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: retryTransientRequest, retryDelay: 0 } } });
        const failure = Object.assign(new Error('Service unavailable'), { status: 503 });
        mockCreateIntegration.mockRejectedValue(failure);
        const { result } = renderHook(() => useCreateIntegration(), { wrapper });

        await act(async () => {
            await expect(result.current.mutateAsync({ name: 'My integration', provider: 'solace' })).rejects.toBe(failure);
        });

        expect(mockCreateIntegration).toHaveBeenCalledTimes(1);
    });
});
