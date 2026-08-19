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
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useAddGroupMembers, useDeleteGroupInvitation, useInviteGroupMember } from './useGroupMutations';
import { addGroupMembers, deleteGroupInvitation, inviteGroupMember } from '../services/groups';
import { groupKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));
jest.mock('../services/groups', () => ({
    addGroupMembers: jest.fn(),
    createGroup: jest.fn(),
    deleteGroup: jest.fn(),
    deleteGroupInvitation: jest.fn(),
    inviteGroupMember: jest.fn(),
    updateGroup: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockAddGroupMembers = jest.mocked(addGroupMembers);
const mockInviteGroupMember = jest.mocked(inviteGroupMember);
const mockDeleteGroupInvitation = jest.mocked(deleteGroupInvitation);
const INVITATION_DATA = {
    reference_type: 'GROUP',
    reference_id: 'group-1',
    email: 'user@example.com',
    api_role: 'USER',
    application_role: 'USER',
} as const;

describe('group mutation invalidation', () => {
    let queryClient: QueryClient;
    let invalidateQueries: jest.SpiedFunction<QueryClient['invalidateQueries']>;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
        invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        mockAddGroupMembers.mockResolvedValue(undefined);
        mockInviteGroupMember.mockResolvedValue({ ambiguous: false });
        mockDeleteGroupInvitation.mockResolvedValue(undefined);
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it('invalidates only members after adding members', async () => {
        const { result } = renderHook(() => useAddGroupMembers(), { wrapper });

        await act(() =>
            result.current.mutateAsync({
                groupId: 'group-1',
                memberships: [{ id: 'user-1', roles: [{ scope: 'API', name: 'USER' }] }],
            }),
        );

        expect(invalidateQueries).toHaveBeenCalledTimes(1);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: groupKeys.members('DEFAULT', 'group-1') });
    });

    it('invalidates members and invitations after inviting a member', async () => {
        const { result } = renderHook(() => useInviteGroupMember(), { wrapper });

        await act(() =>
            result.current.mutateAsync({
                groupId: 'group-1',
                data: INVITATION_DATA,
            }),
        );

        expect(invalidateQueries).toHaveBeenCalledTimes(2);
        expect(invalidateQueries).toHaveBeenNthCalledWith(1, { queryKey: groupKeys.members('DEFAULT', 'group-1') });
        expect(invalidateQueries).toHaveBeenNthCalledWith(2, { queryKey: groupKeys.invitations('DEFAULT', 'group-1') });
    });

    it('does not invalidate group data when the invitation only returns ambiguous user matches', async () => {
        mockInviteGroupMember.mockResolvedValue({ ambiguous: true });
        const { result } = renderHook(() => useInviteGroupMember(), { wrapper });

        await act(() =>
            result.current.mutateAsync({
                groupId: 'group-1',
                data: INVITATION_DATA,
            }),
        );

        expect(invalidateQueries).not.toHaveBeenCalled();
    });

    it('invalidates only invitations after deleting an invitation', async () => {
        const { result } = renderHook(() => useDeleteGroupInvitation(), { wrapper });

        await act(() => result.current.mutateAsync({ groupId: 'group-1', invitationId: 'invitation-1' }));

        expect(invalidateQueries).toHaveBeenCalledTimes(1);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: groupKeys.invitations('DEFAULT', 'group-1') });
    });
});
