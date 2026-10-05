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

import { useDeleteFederatedApis } from './useDeleteFederatedApis';
import { deleteFederatedApis } from '../services/integrationDetail';
import { integrationKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));
jest.mock('../services/integrationDetail', () => ({
    deleteFederatedApis: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockDeleteFederatedApis = jest.mocked(deleteFederatedApis);

const FEDERATED_APIS_KEY = integrationKeys.federatedApis('env-1', 'int-1');
const OTHER_FEDERATED_APIS_KEY = integrationKeys.federatedApis('env-1', 'int-2');
const DETAIL_KEY = integrationKeys.detail('env-1', 'int-1');

describe('useDeleteFederatedApis', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockDeleteFederatedApis.mockResolvedValue({ deleted: 2, skipped: 1, errors: 0 });
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    function seedIntegrationQueries() {
        queryClient.setQueryData(FEDERATED_APIS_KEY, true);
        queryClient.setQueryData(OTHER_FEDERATED_APIS_KEY, true);
        queryClient.setQueryData(DETAIL_KEY, { id: 'int-1', name: 'Payments gateway', provider: 'aws-api-gateway' });
    }

    it('refreshes only the federated APIs check of the integration whose APIs were deleted', async () => {
        seedIntegrationQueries();
        const { result } = renderHook(() => useDeleteFederatedApis(), { wrapper });

        await act(() => result.current.mutateAsync('int-1'));

        expect(mockDeleteFederatedApis).toHaveBeenCalledWith('env-1', 'int-1');
        expect(queryClient.getQueryState(FEDERATED_APIS_KEY)?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(OTHER_FEDERATED_APIS_KEY)?.isInvalidated).toBe(false);
        expect(queryClient.getQueryState(DETAIL_KEY)?.isInvalidated).toBe(false);
    });

    it('leaves the federated APIs check untouched when the delete fails', async () => {
        seedIntegrationQueries();
        mockDeleteFederatedApis.mockRejectedValue(new Error('Boom'));
        const { result } = renderHook(() => useDeleteFederatedApis(), { wrapper });

        await act(() => expect(result.current.mutateAsync('int-1')).rejects.toThrow('Boom'));

        expect(queryClient.getQueryState(FEDERATED_APIS_KEY)?.isInvalidated).toBe(false);
    });
});
