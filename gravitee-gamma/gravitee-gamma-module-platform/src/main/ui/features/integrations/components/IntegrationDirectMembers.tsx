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
import {
    Alert,
    AlertDescription,
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Empty,
    EmptyHeader,
    EmptyTitle,
    Skeleton,
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { IntegrationDirectMembersTable, type RoleEditing } from './IntegrationDirectMembersTable';
import { IntegrationRemoveMemberDialog } from './IntegrationRemoveMemberDialog';
import { notify } from '../../../shared/notify';
import type { SearchableUser } from '../../../shared/types/userSearch';
import { AddMembersSheet } from '../../shared/components';
import { useAddIntegrationMembers } from '../hooks/useAddIntegrationMembers';
import { useIntegrationMembers } from '../hooks/useIntegrationMembers';
import { useIntegrationDefaultRole, useIntegrationRoles } from '../hooks/useIntegrationRoles';
import { useRemoveIntegrationMember } from '../hooks/useRemoveIntegrationMember';
import { useUpdateIntegrationMemberRole } from '../hooks/useUpdateIntegrationMemberRole';
import type { IntegrationMember, IntegrationMemberEditState } from '../types/integrationMembers';
import { integrationMembersLoadErrorMessage } from '../utils/integrationMembersLoadErrorMessage';

export function IntegrationDirectMembers({
    integrationId,
    canCreateMembers,
    canUpdateMembers,
    canDeleteMembers,
}: Readonly<{ integrationId: string; canCreateMembers: boolean; canUpdateMembers: boolean; canDeleteMembers: boolean }>) {
    const { data: members, isLoading, isError, error } = useIntegrationMembers(integrationId);
    const [addMembersOpen, setAddMembersOpen] = useState(false);
    const roleEditing = useIntegrationMemberRoleEditing(integrationId, canUpdateMembers);
    const memberRemoval = useIntegrationMemberRemoval(integrationId);

    function renderContent() {
        if (isError) {
            return (
                <Alert variant="destructive">
                    <AlertDescription>{integrationMembersLoadErrorMessage(error)}</AlertDescription>
                </Alert>
            );
        }

        if (isLoading || !members) {
            return <Skeleton className="h-12 rounded-lg" />;
        }

        if (members.length === 0) {
            return (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No direct members</EmptyTitle>
                    </EmptyHeader>
                </Empty>
            );
        }

        return (
            <IntegrationDirectMembersTable
                members={members}
                roleEditing={roleEditing}
                onRemoveMember={canDeleteMembers ? memberRemoval.onRemoveMember : undefined}
            />
        );
    }

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between gap-4 pb-3">
                <CardTitle className="text-base">Direct Members</CardTitle>
                {canCreateMembers ? (
                    <Button type="button" size="sm" onClick={() => setAddMembersOpen(true)}>
                        <PlusIcon className="size-4" aria-hidden="true" />
                        Add members
                    </Button>
                ) : null}
            </CardHeader>
            <CardContent>{renderContent()}</CardContent>
            {canCreateMembers ? (
                <IntegrationAddMembersSheet
                    integrationId={integrationId}
                    open={addMembersOpen}
                    existingMembers={members ?? []}
                    onClose={() => setAddMembersOpen(false)}
                />
            ) : null}
            <IntegrationRemoveMemberDialog
                member={memberRemoval.removingMember}
                isRemoving={memberRemoval.isRemoving}
                onConfirm={memberRemoval.onConfirm}
                onCancel={memberRemoval.onCancel}
            />
        </Card>
    );
}

function useIntegrationMemberRoleEditing(integrationId: string, canUpdateMembers: boolean): RoleEditing | undefined {
    const { data: roles } = useIntegrationRoles({ enabled: canUpdateMembers });
    const { mutate: updateRole, isPending: isSaving } = useUpdateIntegrationMemberRole(integrationId);
    const [editState, setEditState] = useState<IntegrationMemberEditState>(null);

    return useMemo(() => {
        if (!canUpdateMembers) {
            return undefined;
        }
        const cancelEdit = () => setEditState(null);
        return {
            roles: roles ?? [],
            editState,
            isSaving,
            onStartEdit: setEditState,
            onRoleChange: (role: string) => setEditState(current => (current ? { ...current, role } : current)),
            onCancelEdit: cancelEdit,
            onSaveRole: () => {
                if (!editState) {
                    return;
                }
                updateRole(
                    { memberId: editState.memberId, roleName: editState.role },
                    {
                        onSuccess: () => notify.success('Changes successfully saved!'),
                        onError: updateError => notify.error(updateError, 'Failed to update member role.'),
                        onSettled: cancelEdit,
                    },
                );
            },
        };
    }, [canUpdateMembers, roles, editState, isSaving, updateRole]);
}

function useIntegrationMemberRemoval(integrationId: string) {
    const { mutate: removeMember, isPending: isRemoving } = useRemoveIntegrationMember(integrationId);
    const [removingMember, setRemovingMember] = useState<IntegrationMember | null>(null);
    const clearRemovingMember = () => setRemovingMember(null);

    function confirmRemoval() {
        if (!removingMember) {
            return;
        }
        const displayName = removingMember.displayName;
        removeMember(removingMember.id, {
            onSuccess: () => notify.success(`Member ${displayName} has been removed.`),
            onError: removeError => notify.error(removeError, 'Failed to remove member.'),
            onSettled: clearRemovingMember,
        });
    }

    return {
        removingMember,
        isRemoving,
        onRemoveMember: setRemovingMember,
        onConfirm: confirmRemoval,
        onCancel: clearRemovingMember,
    };
}

function IntegrationAddMembersSheet({
    integrationId,
    open,
    existingMembers,
    onClose,
}: Readonly<{ integrationId: string; open: boolean; existingMembers: ReadonlyArray<{ id?: string }>; onClose: () => void }>) {
    const { data: roles } = useIntegrationRoles();
    const { data: defaultRole } = useIntegrationDefaultRole();
    const addMembers = useAddIntegrationMembers(integrationId);

    function handleAdd(users: SearchableUser[], roleName: string): Promise<void> {
        return addMembers.mutateAsync(
            { users, roleName },
            {
                onSuccess: () => {
                    notify.success('Changes successfully saved!');
                    onClose();
                },
                onError: addError => notify.error(addError, 'Failed to add members.'),
            },
        );
    }

    return (
        <AddMembersSheet
            open={open}
            description="Search for users by name or email and add them to this integration."
            roles={roles ?? []}
            defaultRole={defaultRole}
            existingMembers={existingMembers}
            onClose={onClose}
            onAdd={handleAdd}
            isAdding={addMembers.isPending}
        />
    );
}
