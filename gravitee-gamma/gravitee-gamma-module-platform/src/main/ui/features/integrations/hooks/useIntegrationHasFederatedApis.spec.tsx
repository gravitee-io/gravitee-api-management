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
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useIntegrationHasFederatedApis } from './useIntegrationHasFederatedApis';
import { hasFederatedApis } from '../services/integrationDetail';
import { integrationKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));
jest.mock('../services/integrationDetail', () => ({
    hasFederatedApis: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockHasFederatedApis = jest.mocked(hasFederatedApis);

describe('useIntegrationHasFederatedApis', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it.each([true, false])(
        'resolves to %s from the federated-APIs check and caches it under the integration federated-APIs key',
        async hasApis => {
            mockHasFederatedApis.mockResolvedValue(hasApis);

            const { result } = renderHook(() => useIntegrationHasFederatedApis('int-1'), { wrapper });

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(result.current.data).toBe(hasApis);
            expect(mockHasFederatedApis).toHaveBeenCalledWith('env-1', 'int-1');
            expect(queryClient.getQueryData(integrationKeys.federatedApis('env-1', 'int-1'))).toBe(hasApis);
        },
    );

    it('surfaces a failed federated-APIs check as the query error', async () => {
        const failure = new Error('Internal error');
        mockHasFederatedApis.mockRejectedValue(failure);

        const { result } = renderHook(() => useIntegrationHasFederatedApis('int-1'), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toBe(failure);
    });

    it.each([
        { name: 'the id is empty', integrationId: '', environment: { id: 'env-1' } },
        { name: 'no environment is selected', integrationId: 'int-1', environment: undefined },
    ])('stays idle and does not check for federated APIs when $name', ({ integrationId, environment }) => {
        mockUseEnvironment.mockReturnValue(environment as ReturnType<typeof useEnvironment>);

        const { result } = renderHook(() => useIntegrationHasFederatedApis(integrationId), { wrapper });

        expect(result.current.fetchStatus).toBe('idle');
        expect(mockHasFederatedApis).not.toHaveBeenCalled();
    });
});
