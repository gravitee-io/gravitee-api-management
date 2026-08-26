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
jest.mock('../services/roles', () => ({
    createRoleMembership: jest.fn(),
    deleteRoleMembership: jest.fn(),
    listRoleMemberships: jest.fn(),
}));

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useAddRoleMembers, useDeleteRoleMember, useRoleMemberships } from './useRoleMemberships';
import { createRoleMembership, deleteRoleMembership, listRoleMemberships } from '../services/roles';
import { roleKeys } from '../utils/queryKeys';

const mockListRoleMemberships = jest.mocked(listRoleMemberships);
const mockCreateRoleMembership = jest.mocked(createRoleMembership);
const mockDeleteRoleMembership = jest.mocked(deleteRoleMembership);

describe('useRoleMemberships', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
        mockListRoleMemberships.mockReset();
        mockCreateRoleMembership.mockReset();
        mockDeleteRoleMembership.mockReset();
    });

    afterEach(() => {
        queryClient.clear();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it('lists memberships for a role', async () => {
        mockListRoleMemberships.mockResolvedValue([{ id: 'user-1', displayName: 'Jane Doe' }]);

        const { result } = renderHook(() => useRoleMemberships('ORGANIZATION', 'ADMIN'), { wrapper });

        await waitFor(() => expect(result.current.data).toEqual([{ id: 'user-1', displayName: 'Jane Doe' }]));
        expect(mockListRoleMemberships).toHaveBeenCalledWith('ORGANIZATION', 'ADMIN');
    });

    it('stays disabled without a role name', () => {
        const { result } = renderHook(() => useRoleMemberships('ORGANIZATION', undefined), { wrapper });

        expect(result.current.fetchStatus).toBe('idle');
        expect(mockListRoleMemberships).not.toHaveBeenCalled();
    });

    it('adds every selected user in parallel and invalidates the memberships list', async () => {
        mockCreateRoleMembership.mockResolvedValue(undefined);
        const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
        const { result } = renderHook(() => useAddRoleMembers(), { wrapper });

        await act(() =>
            result.current.mutateAsync({
                scope: 'ORGANIZATION',
                roleName: 'ADMIN',
                users: [
                    { id: 'user-1', reference: 'USER' },
                    { id: 'user-2', reference: 'USER' },
                ],
            }),
        );

        expect(mockCreateRoleMembership).toHaveBeenCalledTimes(2);
        expect(mockCreateRoleMembership).toHaveBeenCalledWith('ORGANIZATION', 'ADMIN', { id: 'user-1', reference: 'USER' });
        expect(mockCreateRoleMembership).toHaveBeenCalledWith('ORGANIZATION', 'ADMIN', { id: 'user-2', reference: 'USER' });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roleKeys.memberships('ORGANIZATION', 'ADMIN') });
    });

    it('still adds and invalidates the members that succeeded when one of several fails', async () => {
        mockCreateRoleMembership.mockImplementation(async (_scope, _roleName, membership) => {
            if (membership.reference === 'user-2') throw new Error('boom');
        });
        const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
        const { result } = renderHook(() => useAddRoleMembers(), { wrapper });

        const outcome = await act(() =>
            result.current.mutateAsync({
                scope: 'ORGANIZATION',
                roleName: 'ADMIN',
                users: [
                    { id: 'user-1', reference: 'user-1' },
                    { id: 'user-2', reference: 'user-2' },
                ],
            }),
        );

        expect(outcome).toEqual({ succeededCount: 1, failedCount: 1 });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roleKeys.memberships('ORGANIZATION', 'ADMIN') });
    });

    it('does not invalidate when every add fails', async () => {
        mockCreateRoleMembership.mockRejectedValue(new Error('boom'));
        const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
        const { result } = renderHook(() => useAddRoleMembers(), { wrapper });

        const outcome = await act(() =>
            result.current.mutateAsync({ scope: 'ORGANIZATION', roleName: 'ADMIN', users: [{ id: 'user-1', reference: 'user-1' }] }),
        );

        expect(outcome).toEqual({ succeededCount: 0, failedCount: 1 });
        expect(invalidateQueries).not.toHaveBeenCalled();
    });

    it('deletes a member and invalidates the memberships list', async () => {
        mockDeleteRoleMembership.mockResolvedValue(undefined);
        const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
        const { result } = renderHook(() => useDeleteRoleMember(), { wrapper });

        await act(() => result.current.mutateAsync({ scope: 'ORGANIZATION', roleName: 'ADMIN', userId: 'user-1' }));

        expect(mockDeleteRoleMembership).toHaveBeenCalledWith('ORGANIZATION', 'ADMIN', 'user-1');
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roleKeys.memberships('ORGANIZATION', 'ADMIN') });
    });
});
