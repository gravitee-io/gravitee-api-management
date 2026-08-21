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
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import {
    type GroupMemberRemovalError,
    type GroupMemberUpdateError,
    useAddGroupMembers,
    useAssociateGroupToExisting,
    useDeleteGroup,
    useDeleteGroupInvitation,
    useInviteGroupMember,
    useRemoveGroupMemberWithOwnershipTransfer,
    useUpdateGroupMembersWithRollback,
} from './useGroupMutations';
import { organizationGroupKeys } from '../../../shared/utils/queryKeys';
import {
    addGroupMembers,
    associateGroupToExisting,
    deleteGroup,
    deleteGroupInvitation,
    inviteGroupMember,
    removeGroupMember,
} from '../services/groups';
import { groupKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));
jest.mock('../services/groups', () => ({
    addGroupMembers: jest.fn(),
    associateGroupToExisting: jest.fn(),
    createGroup: jest.fn(),
    deleteGroup: jest.fn(),
    deleteGroupInvitation: jest.fn(),
    inviteGroupMember: jest.fn(),
    removeGroupMember: jest.fn(),
    updateGroup: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockAddGroupMembers = jest.mocked(addGroupMembers);
const mockAssociateGroupToExisting = jest.mocked(associateGroupToExisting);
const mockInviteGroupMember = jest.mocked(inviteGroupMember);
const mockRemoveGroupMember = jest.mocked(removeGroupMember);
const mockDeleteGroup = jest.mocked(deleteGroup);
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
        mockAssociateGroupToExisting.mockResolvedValue({ id: 'group-1', name: 'Support Team' });
        mockInviteGroupMember.mockResolvedValue({ outcome: 'invitation-created' });
        mockRemoveGroupMember.mockResolvedValue(undefined);
        mockDeleteGroup.mockResolvedValue(undefined);
        mockDeleteGroupInvitation.mockResolvedValue(undefined);
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it('invalidates environment and organization group lists after deleting a group', async () => {
        const { result } = renderHook(() => useDeleteGroup(), { wrapper });

        await act(() => result.current.mutateAsync('group-1'));

        expect(invalidateQueries).toHaveBeenCalledTimes(2);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: groupKeys.all });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: organizationGroupKeys.all });
    });

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

    it('invalidates only invitations after creating a pending invitation', async () => {
        const { result } = renderHook(() => useInviteGroupMember(), { wrapper });

        await act(() =>
            result.current.mutateAsync({
                groupId: 'group-1',
                data: INVITATION_DATA,
            }),
        );

        expect(invalidateQueries).toHaveBeenCalledTimes(1);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: groupKeys.invitations('DEFAULT', 'group-1') });
    });

    it('invalidates only members when an existing user is added directly', async () => {
        mockInviteGroupMember.mockResolvedValue({ outcome: 'member-added' });
        const { result } = renderHook(() => useInviteGroupMember(), { wrapper });

        await act(() =>
            result.current.mutateAsync({
                groupId: 'group-1',
                data: INVITATION_DATA,
            }),
        );

        expect(invalidateQueries).toHaveBeenCalledTimes(1);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: groupKeys.members('DEFAULT', 'group-1') });
    });

    it('does not invalidate group data when the invitation only returns ambiguous user matches', async () => {
        mockInviteGroupMember.mockResolvedValue({ outcome: 'ambiguous' });
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

    it.each(['api', 'api_product', 'application'] as const)('invalidates only the %s memberships after bulk association', async type => {
        const { result } = renderHook(() => useAssociateGroupToExisting(), { wrapper });

        await act(() => result.current.mutateAsync({ groupId: 'group-1', type }));

        expect(mockAssociateGroupToExisting).toHaveBeenCalledWith('DEFAULT', 'group-1', type);
        expect(invalidateQueries).toHaveBeenCalledTimes(1);
        expect(invalidateQueries).toHaveBeenCalledWith({
            queryKey: groupKeys.memberships('DEFAULT', 'group-1', type),
        });
    });

    it('invalidates members once after transferring ownership and removing a member', async () => {
        const { result } = renderHook(() => useRemoveGroupMemberWithOwnershipTransfer(), { wrapper });

        await act(() =>
            result.current.mutateAsync({
                groupId: 'group-1',
                memberId: 'member-1',
                ownershipTransfer: {
                    apply: [
                        { id: 'member-1', roles: [{ scope: 'API', name: 'OWNER' }] },
                        { id: 'member-2', roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }] },
                    ],
                    rollback: [
                        { id: 'member-2', roles: [{ scope: 'API', name: 'OWNER' }] },
                        { id: 'member-1', roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }] },
                    ],
                },
            }),
        );

        expect(mockAddGroupMembers).toHaveBeenCalledTimes(1);
        expect(mockAddGroupMembers).toHaveBeenCalledWith('DEFAULT', 'group-1', [
            { id: 'member-1', roles: [{ scope: 'API', name: 'OWNER' }] },
            { id: 'member-2', roles: [{ scope: 'API', name: 'PRIMARY_OWNER' }] },
        ]);
        expect(mockRemoveGroupMember).toHaveBeenCalledWith('DEFAULT', 'group-1', 'member-1');
        expect(invalidateQueries).toHaveBeenCalledTimes(1);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: groupKeys.members('DEFAULT', 'group-1') });
    });

    it('restores both memberships when removal fails after ownership transfer', async () => {
        const removeError = new Error('remove failed');
        mockRemoveGroupMember.mockRejectedValue(removeError);
        const { result } = renderHook(() => useRemoveGroupMemberWithOwnershipTransfer(), { wrapper });
        const ownershipTransfer = {
            apply: [
                { id: 'member-1', roles: [{ scope: 'API' as const, name: 'OWNER' }] },
                { id: 'member-2', roles: [{ scope: 'API' as const, name: 'PRIMARY_OWNER' }] },
            ],
            rollback: [
                { id: 'member-2', roles: [{ scope: 'API' as const, name: 'OWNER' }] },
                { id: 'member-1', roles: [{ scope: 'API' as const, name: 'PRIMARY_OWNER' }] },
            ],
        };

        await expect(
            act(() =>
                result.current.mutateAsync({
                    groupId: 'group-1',
                    memberId: 'member-1',
                    ownershipTransfer,
                }),
            ),
        ).rejects.toMatchObject({
            phase: 'remove',
            operationError: removeError,
            rollbackSucceeded: true,
        } satisfies Partial<GroupMemberRemovalError>);

        await waitFor(() => expect(mockAddGroupMembers).toHaveBeenCalledTimes(2));
        expect(mockAddGroupMembers).toHaveBeenNthCalledWith(1, 'DEFAULT', 'group-1', ownershipTransfer.apply);
        expect(mockAddGroupMembers).toHaveBeenNthCalledWith(2, 'DEFAULT', 'group-1', ownershipTransfer.rollback);
        expect(invalidateQueries).toHaveBeenCalledTimes(1);
    });

    it('restores both memberships when the transfer request partially fails', async () => {
        const transferError = new Error('transfer failed');
        mockAddGroupMembers.mockRejectedValueOnce(transferError).mockResolvedValueOnce(undefined);
        const { result } = renderHook(() => useRemoveGroupMemberWithOwnershipTransfer(), { wrapper });
        const ownershipTransfer = {
            apply: [
                { id: 'member-1', roles: [{ scope: 'API' as const, name: 'OWNER' }] },
                { id: 'member-2', roles: [{ scope: 'API' as const, name: 'PRIMARY_OWNER' }] },
            ],
            rollback: [
                { id: 'member-2', roles: [{ scope: 'API' as const, name: 'OWNER' }] },
                { id: 'member-1', roles: [{ scope: 'API' as const, name: 'PRIMARY_OWNER' }] },
            ],
        };

        await expect(
            act(() =>
                result.current.mutateAsync({
                    groupId: 'group-1',
                    memberId: 'member-1',
                    ownershipTransfer,
                }),
            ),
        ).rejects.toMatchObject({
            phase: 'transfer',
            operationError: transferError,
            rollbackSucceeded: true,
        } satisfies Partial<GroupMemberRemovalError>);

        await waitFor(() => expect(mockAddGroupMembers).toHaveBeenCalledTimes(2));
        expect(mockAddGroupMembers).toHaveBeenNthCalledWith(1, 'DEFAULT', 'group-1', ownershipTransfer.apply);
        expect(mockAddGroupMembers).toHaveBeenNthCalledWith(2, 'DEFAULT', 'group-1', ownershipTransfer.rollback);
        expect(mockRemoveGroupMember).not.toHaveBeenCalled();
    });

    it('restores original memberships when an edit fails after a partial ownership update', async () => {
        const updateError = new Error('update failed');
        mockAddGroupMembers.mockRejectedValueOnce(updateError).mockResolvedValueOnce(undefined);
        const { result } = renderHook(() => useUpdateGroupMembersWithRollback(), { wrapper });
        const apply = [
            { id: 'member-1', roles: [{ scope: 'API' as const, name: 'OWNER' }] },
            { id: 'member-2', roles: [{ scope: 'API' as const, name: 'PRIMARY_OWNER' }] },
        ];
        const rollback = [
            { id: 'member-2', roles: [{ scope: 'API' as const, name: 'OWNER' }] },
            { id: 'member-1', roles: [{ scope: 'API' as const, name: 'PRIMARY_OWNER' }] },
        ];

        await expect(act(() => result.current.mutateAsync({ groupId: 'group-1', apply, rollback }))).rejects.toMatchObject({
            phase: 'update',
            operationError: updateError,
            rollbackSucceeded: true,
        } satisfies Partial<GroupMemberUpdateError>);

        await waitFor(() => expect(mockAddGroupMembers).toHaveBeenCalledTimes(2));
        expect(mockAddGroupMembers).toHaveBeenNthCalledWith(1, 'DEFAULT', 'group-1', apply);
        expect(mockAddGroupMembers).toHaveBeenNthCalledWith(2, 'DEFAULT', 'group-1', rollback);
        expect(invalidateQueries).toHaveBeenCalledTimes(1);
    });
});
