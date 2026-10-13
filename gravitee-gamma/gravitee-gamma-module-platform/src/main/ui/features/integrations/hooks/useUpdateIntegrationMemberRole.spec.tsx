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

import { useUpdateIntegrationMemberRole } from './useUpdateIntegrationMemberRole';
import { updateIntegrationMemberRole } from '../services/integrationMembers';
import { integrationKeys } from '../utils/queryKeys';

jest.mock('../services/integrationMembers', () => ({ updateIntegrationMemberRole: jest.fn() }));

const mockUpdateIntegrationMemberRole = jest.mocked(updateIntegrationMemberRole);

describe('useUpdateIntegrationMemberRole', () => {
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

    it("updates the member's role in the integration of the environment and refreshes the integration members", async () => {
        mockUpdateIntegrationMemberRole.mockResolvedValue(undefined);
        const { result } = renderHook(() => useUpdateIntegrationMemberRole('int-1'), { wrapper });

        await act(() => result.current.mutateAsync({ memberId: 'user-bob', roleName: 'OWNER' }));

        expect(mockUpdateIntegrationMemberRole).toHaveBeenCalledWith('DEFAULT', 'int-1', 'user-bob', 'OWNER');
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: integrationKeys.members('DEFAULT', 'int-1') });
    });

    it('reports a failed update as its error and leaves the integration members untouched', async () => {
        const failure = new Error('Role could not be updated');
        mockUpdateIntegrationMemberRole.mockRejectedValue(failure);
        const { result } = renderHook(() => useUpdateIntegrationMemberRole('int-1'), { wrapper });

        act(() => result.current.mutate({ memberId: 'user-bob', roleName: 'OWNER' }));

        await waitFor(() => expect(result.current.error).toBe(failure));
        expect(invalidateQueries).not.toHaveBeenCalled();
    });
});
