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

import { useRemoveIntegrationMember } from './useRemoveIntegrationMember';
import { removeIntegrationMember } from '../services/integrationMembers';
import { integrationKeys } from '../utils/queryKeys';

jest.mock('../services/integrationMembers', () => ({ removeIntegrationMember: jest.fn() }));

const mockRemoveIntegrationMember = jest.mocked(removeIntegrationMember);

describe('useRemoveIntegrationMember', () => {
    let queryClient: QueryClient;
    let invalidateQueries: jest.SpiedFunction<QueryClient['invalidateQueries']>;

    beforeEach(() => {
        jest.clearAllMocks();
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
        invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it('removes the member from the integration of the environment and refreshes the integration members', async () => {
        mockRemoveIntegrationMember.mockResolvedValue(undefined);
        const { result } = renderHook(() => useRemoveIntegrationMember('int-1'), { wrapper });

        await act(() => result.current.mutateAsync('user-bob'));

        expect(mockRemoveIntegrationMember).toHaveBeenCalledWith('DEFAULT', 'int-1', 'user-bob');
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: integrationKeys.members('DEFAULT', 'int-1') });
    });

    it('reports a failed removal as its error and leaves the integration members untouched', async () => {
        const failure = new Error('Member could not be removed');
        mockRemoveIntegrationMember.mockRejectedValue(failure);
        const { result } = renderHook(() => useRemoveIntegrationMember('int-1'), { wrapper });

        act(() => result.current.mutate('user-bob'));

        await waitFor(() => expect(result.current.error).toBe(failure));
        expect(invalidateQueries).not.toHaveBeenCalled();
    });
});
