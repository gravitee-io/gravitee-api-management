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

import { useUpdateIntegration } from './useUpdateIntegration';
import { retryTransientRequest } from '../../../shared/api/queryRetry';
import { updateIntegration } from '../services/integrationUpdate';
import { integrationKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));
jest.mock('../services/integrationUpdate', () => ({
    updateIntegration: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUpdateIntegration = jest.mocked(updateIntegration);

const request = { name: 'New name', description: 'New description', groups: ['Platform Team'] };

describe('useUpdateIntegration', () => {
    let queryClient: QueryClient;
    let invalidateQueries: jest.SpiedFunction<QueryClient['invalidateQueries']>;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
        invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockUpdateIntegration.mockResolvedValue({ id: 'int-1', name: 'New name', description: 'New description', provider: 'solace' });
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it('updates the integration and refreshes only the integrations lists and its detail', async () => {
        const { result } = renderHook(() => useUpdateIntegration(), { wrapper });

        const updated = await act(() => result.current.mutateAsync({ integrationId: 'int-1', request }));

        expect(mockUpdateIntegration).toHaveBeenCalledWith('env-1', 'int-1', request);
        expect(updated).toEqual({ id: 'int-1', name: 'New name', description: 'New description', provider: 'solace' });
        expect(invalidateQueries).toHaveBeenCalledTimes(2);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: [...integrationKeys.all, 'list'] });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: integrationKeys.detail('env-1', 'int-1') });
        expect(invalidateQueries).not.toHaveBeenCalledWith({ queryKey: integrationKeys.all });
        expect(invalidateQueries).not.toHaveBeenCalledWith({ queryKey: integrationKeys.permissions('env-1', 'int-1') });
        expect(invalidateQueries).not.toHaveBeenCalledWith({ queryKey: integrationKeys.federatedApis('env-1', 'int-1') });
    });

    it('resolves the update without waiting for the integrations queries to refetch', async () => {
        invalidateQueries.mockReturnValue(new Promise<void>(() => {}));
        const { result } = renderHook(() => useUpdateIntegration(), { wrapper });

        let outcome: unknown;
        await act(async () => {
            outcome = await Promise.race([
                result.current.mutateAsync({ integrationId: 'int-1', request }),
                new Promise(resolve => setTimeout(() => resolve('blocked'), 50)),
            ]);
        });

        expect(outcome).toEqual({ id: 'int-1', name: 'New name', description: 'New description', provider: 'solace' });
    });

    it('surfaces a failed update as the mutation error and leaves the integrations queries untouched', async () => {
        const failure = new Error('Update rejected');
        mockUpdateIntegration.mockRejectedValue(failure);
        const { result } = renderHook(() => useUpdateIntegration(), { wrapper });

        await act(async () => {
            await expect(result.current.mutateAsync({ integrationId: 'int-1', request })).rejects.toBe(failure);
        });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toBe(failure);
        expect(invalidateQueries).not.toHaveBeenCalled();
    });

    it('does not repeat an update that failed with a transient error', async () => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: retryTransientRequest, retryDelay: 0 } } });
        const failure = Object.assign(new Error('Service unavailable'), { status: 503 });
        mockUpdateIntegration.mockRejectedValue(failure);
        const { result } = renderHook(() => useUpdateIntegration(), { wrapper });

        await act(async () => {
            await expect(result.current.mutateAsync({ integrationId: 'int-1', request })).rejects.toBe(failure);
        });

        expect(mockUpdateIntegration).toHaveBeenCalledTimes(1);
    });
});
