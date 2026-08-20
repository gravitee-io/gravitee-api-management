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

import { useState } from 'react';

import { useGroupInvitations } from './useGroupDetail';
import {
    GroupMemberRemovalError,
    useAddGroupMembers,
    useDeleteGroupInvitation,
    useInviteGroupMember,
    useRemoveGroupMemberWithOwnershipTransfer,
} from './useGroupMutations';
import { notify } from '../../../shared/notify';
import type { GroupInvitation, GroupMember, GroupMembershipPayload } from '../types/group';
import { requiresPrimaryOwnerSuccessor, type RemovalOwnershipTransfer } from '../utils/primaryOwnership';

type MemberSheetState = 'closed' | 'search' | 'invite';
type MemberTab = 'members' | 'invitations';

export function useGroupMemberActions(groupId: string | undefined) {
    const [memberTab, setMemberTab] = useState<MemberTab>('members');
    const [memberSheet, setMemberSheet] = useState<MemberSheetState>('closed');
    const [editingMember, setEditingMember] = useState<GroupMember | null>(null);
    const [removingMember, setRemovingMember] = useState<GroupMember | null>(null);
    const [tooManyUsersEmail, setTooManyUsersEmail] = useState<string | null>(null);
    const [searchSeed, setSearchSeed] = useState<string | null>(null);
    const [deletingInvitation, setDeletingInvitation] = useState<GroupInvitation | null>(null);

    const addMembersMutation = useAddGroupMembers();
    const inviteMemberMutation = useInviteGroupMember();
    const removeMemberMutation = useRemoveGroupMemberWithOwnershipTransfer();
    const deleteInvitationMutation = useDeleteGroupInvitation();
    const {
        data: invitations = [],
        isLoading: invitationsLoading,
        isError: invitationsError,
    } = useGroupInvitations(memberTab === 'invitations' ? groupId : undefined);

    function closeMemberSheet() {
        setMemberSheet('closed');
        setSearchSeed(null);
    }

    async function handleAddMembers(memberships: GroupMembershipPayload[]) {
        if (!groupId) return;
        try {
            await addMembersMutation.mutateAsync({ groupId, memberships });
            notify.success(memberships.length > 1 ? `${memberships.length} members added successfully` : 'Member added successfully');
            closeMemberSheet();
        } catch (error) {
            notify.error(error, 'Failed to add members');
        }
    }

    async function handleInviteMember(values: { email: string; apiRole: string; applicationRole: string }) {
        if (!groupId) return;
        try {
            const result = await inviteMemberMutation.mutateAsync({
                groupId,
                data: {
                    reference_type: 'GROUP',
                    reference_id: groupId,
                    email: values.email,
                    api_role: values.apiRole,
                    application_role: values.applicationRole,
                },
            });
            closeMemberSheet();
            switch (result.outcome) {
                case 'ambiguous':
                    setTooManyUsersEmail(values.email);
                    return;
                case 'member-added':
                    notify.success('Member added successfully');
                    setMemberTab('members');
                    return;
                case 'invitation-created':
                    notify.success('Successfully invited user to the group.');
                    setMemberTab('invitations');
                    return;
            }
        } catch (error) {
            notify.error(error, 'Failed to invite member');
        }
    }

    function handleTooManyUsersContinue() {
        setSearchSeed(tooManyUsersEmail);
        setTooManyUsersEmail(null);
        setMemberSheet('search');
    }

    async function handleEditMemberRoles(memberships: GroupMembershipPayload[]) {
        if (!groupId) return;
        try {
            await addMembersMutation.mutateAsync({ groupId, memberships });
            notify.success(
                memberships.length > 1 ? 'Member roles updated and primary ownership transferred' : 'Member roles updated successfully',
            );
            setEditingMember(null);
        } catch (error) {
            notify.error(error, 'Failed to update member roles');
        }
    }

    async function handleRemoveMember(ownershipTransfer?: RemovalOwnershipTransfer) {
        if (!groupId || !removingMember) return;

        if (requiresPrimaryOwnerSuccessor(removingMember) && !ownershipTransfer) {
            notify.warning('Primary ownership must be transferred before removing this member');
            return;
        }

        try {
            await removeMemberMutation.mutateAsync({ groupId, memberId: removingMember.id, ownershipTransfer });
        } catch (error) {
            if (!(error instanceof GroupMemberRemovalError)) {
                notify.error(error, 'Failed to remove member');
                return;
            }
            if (error.phase === 'transfer') {
                notify.error(error.operationError, 'Primary ownership could not be transferred');
                return;
            }
            if (error.rollbackSucceeded) {
                notify.error(error.operationError, 'The member could not be removed. Primary ownership was restored.');
                return;
            }
            if (error.phase === 'rollback') {
                notify.error(
                    error.rollbackError ?? error.operationError,
                    'The member could not be removed and primary ownership could not be restored. Refresh the member list before retrying.',
                );
                return;
            }
            notify.error(error.operationError, 'Failed to remove member');
            return;
        }

        notify.success(`${removingMember.displayName} removed from the group`);
        setRemovingMember(null);
    }

    async function handleDeleteInvitation() {
        if (!groupId || !deletingInvitation) return;
        try {
            await deleteInvitationMutation.mutateAsync({ groupId, invitationId: deletingInvitation.id });
            notify.success('Invitation deleted successfully');
            setDeletingInvitation(null);
        } catch (error) {
            notify.error(error, 'Failed to delete invitation');
        }
    }

    return {
        memberTab,
        setMemberTab,
        memberSheet,
        setMemberSheet,
        closeMemberSheet,
        editingMember,
        setEditingMember,
        removingMember,
        setRemovingMember,
        tooManyUsersEmail,
        setTooManyUsersEmail,
        searchSeed,
        deletingInvitation,
        setDeletingInvitation,
        invitations,
        invitationsLoading,
        invitationsError,
        deleteInvitationMutation,
        handleAddMembers,
        handleInviteMember,
        handleTooManyUsersContinue,
        handleEditMemberRoles,
        handleRemoveMember,
        handleDeleteInvitation,
    };
}
