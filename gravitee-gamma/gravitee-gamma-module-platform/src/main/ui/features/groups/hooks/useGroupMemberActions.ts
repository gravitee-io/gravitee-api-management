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
import { useAddGroupMembers, useDeleteGroupInvitation, useInviteGroupMember, useRemoveGroupMember } from './useGroupMutations';
import { notify } from '../../../shared/notify';
import type { GroupInvitation, GroupMember, GroupMembershipPayload } from '../types/group';

type MemberSheetState = 'closed' | 'search' | 'invite';
type MemberTab = 'members' | 'invitations';

export function useGroupMemberActions(groupId: string | undefined) {
    const [memberTab, setMemberTab] = useState<MemberTab>('members');
    const [memberSheet, setMemberSheet] = useState<MemberSheetState>('closed');
    const [editingMember, setEditingMember] = useState<GroupMember | null>(null);
    const [removingMember, setRemovingMember] = useState<GroupMember | null>(null);
    const [tooManyUsersEmail, setTooManyUsersEmail] = useState<string | null>(null);
    const [deletingInvitation, setDeletingInvitation] = useState<GroupInvitation | null>(null);

    const {
        data: invitations = [],
        isLoading: invitationsLoading,
        isError: invitationsError,
    } = useGroupInvitations(memberTab === 'invitations' ? groupId : undefined);

    const addMembersMutation = useAddGroupMembers();
    const inviteMemberMutation = useInviteGroupMember();
    const removeMemberMutation = useRemoveGroupMember();
    const deleteInvitationMutation = useDeleteGroupInvitation();

    function closeMemberSheet() {
        setMemberSheet('closed');
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
                    reference_id: groupId,
                    email: values.email,
                    api_role: values.apiRole || undefined,
                    application_role: values.applicationRole || undefined,
                },
            });
            if (result.ambiguous) {
                closeMemberSheet();
                setTooManyUsersEmail(values.email);
                return;
            }
            notify.success(`Invitation sent to ${values.email}`);
            closeMemberSheet();
        } catch (error) {
            notify.error(error, 'Failed to send invitation');
        }
    }

    function handleTooManyUsersContinue() {
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

    async function handleRemoveMember(transferMembership?: GroupMembershipPayload) {
        if (!groupId || !removingMember) return;

        // Transfer first so a failed transfer aborts before anything is removed.
        if (transferMembership) {
            try {
                await addMembersMutation.mutateAsync({ groupId, memberships: [transferMembership] });
            } catch (error) {
                notify.error(error, 'Primary ownership could not be transferred');
                // Keep the dialog open so the operator can retry or pick another successor.
                return;
            }
        }

        try {
            await removeMemberMutation.mutateAsync({ groupId, memberId: removingMember.id });
        } catch (error) {
            notify.error(error, 'Failed to remove member');
            return;
        }

        notify.success(`${removingMember.displayName} removed from the group`);
        setRemovingMember(null);
    }

    async function handleDeleteInvitation() {
        if (!groupId || !deletingInvitation) return;
        try {
            await deleteInvitationMutation.mutateAsync({ groupId, invitationId: deletingInvitation.id });
            notify.success('Successfully deleted the invitation.');
            setDeletingInvitation(null);
        } catch (error) {
            notify.error(error, 'Error occurred while deleting the invitation.');
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
        deletingInvitation,
        setDeletingInvitation,
        invitations,
        invitationsLoading,
        invitationsError,
        addMembersMutation,
        inviteMemberMutation,
        removeMemberMutation,
        deleteInvitationMutation,
        handleAddMembers,
        handleInviteMember,
        handleTooManyUsersContinue,
        handleEditMemberRoles,
        handleRemoveMember,
        handleDeleteInvitation,
    };
}
