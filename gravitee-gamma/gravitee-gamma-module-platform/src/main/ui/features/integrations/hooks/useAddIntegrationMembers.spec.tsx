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

import { useAddIntegrationMembers } from './useAddIntegrationMembers';
import { addIntegrationMember } from '../services/integrationMembers';
import { integrationKeys } from '../utils/queryKeys';

jest.mock('../services/integrationMembers', () => ({ addIntegrationMember: jest.fn() }));

const mockAddIntegrationMember = jest.mocked(addIntegrationMember);

const CAROL = { id: 'user-carol', reference: 'ref-carol', displayName: 'Carol Diaz' };
const DAN_WITHOUT_ID = { id: null, reference: 'ref-dan', displayName: 'Dan External' };
const EVE = { id: 'user-eve', reference: 'ref-eve', displayName: 'Eve Stone' };

describe('useAddIntegrationMembers', () => {
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

    it('adds each selected user with the chosen role and refreshes the integration members', async () => {
        mockAddIntegrationMember.mockResolvedValue(undefined);
        const { result } = renderHook(() => useAddIntegrationMembers('int-1'), { wrapper });

        await act(() => result.current.mutateAsync({ users: [CAROL, DAN_WITHOUT_ID], roleName: 'USER' }));

        expect(mockAddIntegrationMember.mock.calls).toEqual([
            ['DEFAULT', 'int-1', { userId: 'user-carol', externalReference: 'ref-carol', roleName: 'USER' }],
            ['DEFAULT', 'int-1', { userId: undefined, externalReference: 'ref-dan', roleName: 'USER' }],
        ]);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: integrationKeys.members('DEFAULT', 'int-1') });
    });

    it('fails with a summary of every failed add and still refreshes the integration members when another add succeeded', async () => {
        mockAddIntegrationMember
            .mockResolvedValueOnce(undefined)
            .mockRejectedValueOnce(new Error('first failure'))
            .mockRejectedValueOnce(new Error('second failure'));
        const { result } = renderHook(() => useAddIntegrationMembers('int-1'), { wrapper });

        await act(async () => {
            await expect(
                result.current.mutateAsync({
                    users: [CAROL, DAN_WITHOUT_ID, EVE],
                    roleName: 'OWNER',
                }),
            ).rejects.toThrow('Added 1 of 3 members. Failed to add: Dan External (first failure), Eve Stone (second failure).');
        });

        expect(mockAddIntegrationMember).toHaveBeenCalledTimes(3);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: integrationKeys.members('DEFAULT', 'int-1') });
    });
});
