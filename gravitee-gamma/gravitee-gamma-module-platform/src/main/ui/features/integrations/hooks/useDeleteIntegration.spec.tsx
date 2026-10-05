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
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useDeleteIntegration } from './useDeleteIntegration';
import { deleteIntegration } from '../services/integrationDetail';
import { integrationKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));
jest.mock('../services/integrationDetail', () => ({
    deleteIntegration: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockDeleteIntegration = jest.mocked(deleteIntegration);

const DELETED_DETAIL_KEY = integrationKeys.detail('env-1', 'int-1');
const DELETED_PERMISSIONS_KEY = integrationKeys.permissions('env-1', 'int-1');
const DELETED_FEDERATED_APIS_KEY = integrationKeys.federatedApis('env-1', 'int-1');
const LIST_KEY = integrationKeys.list('env-1', 1, 10);
const OTHER_DETAIL_KEY = integrationKeys.detail('env-1', 'int-2');

describe('useDeleteIntegration', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockDeleteIntegration.mockResolvedValue(undefined);
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    function seedIntegrationQueries() {
        queryClient.setQueryData(DELETED_DETAIL_KEY, { id: 'int-1', name: 'Payments gateway', provider: 'aws-api-gateway' });
        queryClient.setQueryData(DELETED_PERMISSIONS_KEY, ['integration-definition-d']);
        queryClient.setQueryData(DELETED_FEDERATED_APIS_KEY, false);
        queryClient.setQueryData(LIST_KEY, {
            data: [],
            pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
        });
        queryClient.setQueryData(OTHER_DETAIL_KEY, { id: 'int-2', name: 'Orders gateway', provider: 'solace' });
    }

    it('drops the deleted integration from the cache and refreshes only the integrations lists', async () => {
        seedIntegrationQueries();
        const { result } = renderHook(() => useDeleteIntegration(), { wrapper });

        await act(() => result.current.mutateAsync('int-1'));

        expect(mockDeleteIntegration).toHaveBeenCalledWith('env-1', 'int-1');
        expect(queryClient.getQueryState(DELETED_DETAIL_KEY)).toBeUndefined();
        expect(queryClient.getQueryState(DELETED_PERMISSIONS_KEY)).toBeUndefined();
        expect(queryClient.getQueryState(DELETED_FEDERATED_APIS_KEY)).toBeUndefined();
        expect(queryClient.getQueryState(LIST_KEY)?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(OTHER_DETAIL_KEY)?.isInvalidated).toBe(false);
    });

    it('keeps the integration cache when the delete fails', async () => {
        seedIntegrationQueries();
        mockDeleteIntegration.mockRejectedValue(new Error('Boom'));
        const { result } = renderHook(() => useDeleteIntegration(), { wrapper });

        await act(() => expect(result.current.mutateAsync('int-1')).rejects.toThrow('Boom'));

        expect(queryClient.getQueryData(DELETED_DETAIL_KEY)).toEqual({
            id: 'int-1',
            name: 'Payments gateway',
            provider: 'aws-api-gateway',
        });
        expect(queryClient.getQueryData(DELETED_PERMISSIONS_KEY)).toEqual(['integration-definition-d']);
        expect(queryClient.getQueryData(DELETED_FEDERATED_APIS_KEY)).toBe(false);
        expect(queryClient.getQueryState(LIST_KEY)?.isInvalidated).toBe(false);
    });
});
