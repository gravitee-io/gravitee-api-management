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
import {
    Badge,
    Button,
    Card,
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Separator,
    Skeleton,
    Switch,
} from '@gravitee/graphene-core';
import { PlusIcon, UserCogIcon, UsersIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useState } from 'react';
import { useParams } from 'react-router-dom';

import { AddMembersDialog } from '../../../components/user-permissions/AddMembersDialog';
import { DirectMembersTable } from '../../../components/user-permissions/DirectMembersTable';
import { GroupMembersSection } from '../../../components/user-permissions/GroupMembersSection';
import { ManageGroupsDialog } from '../../../components/user-permissions/ManageGroupsDialog';
import { getApiRole, type EditState } from '../../../components/user-permissions/memberHelpers';
import { RemoveMemberDialog } from '../../../components/user-permissions/RemoveMemberDialog';
import { TransferOwnershipDialog } from '../../../components/user-permissions/TransferOwnershipDialog';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiGroupMembers } from '../../../hooks/useApiGroupMembers';
import { useApiMembers } from '../../../hooks/useApiMembers';
import { useApiRoles } from '../../../hooks/useApiRoles';
import { useGroups } from '../../../hooks/useGroups';
import {
    addApiMember,
    deleteApiMember,
    transferApiOwnership,
    updateApiGroups,
    updateApiMember,
    updateApiNotifications,
} from '../../../services/members';
import type { Member, SearchableUser, TransferOwnershipPayload } from '../../../types/members.types';
import { apiDetailKeys, apiMemberKeys, groupKeys } from '../../../utils/queryKeys';

export function UserPermissionsPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const { api } = useApiDetailContext();

    const [editState, setEditState] = useState<EditState>(null);
    const [memberToRemove, setMemberToRemove] = useState<Member | null>(null);
    const [addMembersOpen, setAddMembersOpen] = useState(false);
    const [transferOpen, setTransferOpen] = useState(false);
    const [manageGroupsOpen, setManageGroupsOpen] = useState(false);

    const { data: membersData, isLoading: membersLoading } = useApiMembers(apiId);
    const { data: groupMembersMap, isLoading: groupsLoading } = useApiGroupMembers(apiId);
    const { data: rolesData } = useApiRoles();
    const { data: groupsData } = useGroups();

    const members = membersData?.data ?? [];
    const roleNames = (rolesData ?? []).filter(r => r.name !== 'PRIMARY_OWNER').map(r => r.name);
    const groupEntries = Object.entries(groupMembersMap ?? {}).filter(([, gm]) => gm.length > 0);
    const allGroups = groupsData?.data ?? [];
    const currentGroupIds = api?.groups ?? [];

    const addMutation = useMutation({
        mutationFn: ({ users, roleName }: { users: SearchableUser[]; roleName: string }) =>
            Promise.all(
                users.map(user =>
                    addApiMember(env!.id, apiId!, { userId: user.id ?? undefined, externalReference: user.reference, roleName }),
                ),
            ),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.list(env!.id, apiId!) });
            setAddMembersOpen(false);
        },
    });

    const updateMutation = useMutation({
        mutationFn: ({ memberId, roleName }: { memberId: string; roleName: string }) =>
            updateApiMember(env!.id, apiId!, memberId, roleName),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.list(env!.id, apiId!) });
            setEditState(null);
        },
    });

    const deleteMutation = useMutation({
        mutationFn: (memberId: string) => deleteApiMember(env!.id, apiId!, memberId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.list(env!.id, apiId!) });
            setMemberToRemove(null);
        },
    });

    const transferMutation = useMutation({
        mutationFn: (payload: TransferOwnershipPayload) => transferApiOwnership(env!.id, apiId!, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.list(env!.id, apiId!) });
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env!.id, apiId!) });
            setTransferOpen(false);
        },
    });

    const groupsMutation = useMutation({
        mutationFn: (groupIds: string[]) => updateApiGroups(env!.id, apiId!, groupIds),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.groups(env!.id, apiId!) });
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env!.id, apiId!) });
            queryClient.invalidateQueries({ queryKey: groupKeys.list(env!.id) });
            setManageGroupsOpen(false);
        },
    });

    const notificationMutation = useMutation({
        mutationFn: (disable: boolean) => updateApiNotifications(env!.id, apiId!, disable),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env!.id, apiId!) });
        },
    });

    const handleStartEdit = useCallback((member: Member) => setEditState({ memberId: member.id, role: getApiRole(member) }), []);
    const handleRoleChange = useCallback((role: string) => setEditState(prev => (prev ? { ...prev, role } : null)), []);
    const handleSaveRole = useCallback(() => {
        if (editState) updateMutation.mutate({ memberId: editState.memberId, roleName: editState.role });
    }, [editState, updateMutation]);
    const handleCancelEdit = useCallback(() => setEditState(null), []);
    const handleRemoveRequest = useCallback((member: Member) => setMemberToRemove(member), []);
    const handleRemoveConfirm = useCallback(() => {
        if (memberToRemove) deleteMutation.mutate(memberToRemove.id);
    }, [memberToRemove, deleteMutation]);
    const handleRemoveCancel = useCallback(() => setMemberToRemove(null), []);

    const notificationsEnabled = !(api?.disableMembershipNotifications ?? false);
    const handleNotificationToggle = useCallback((checked: boolean) => notificationMutation.mutate(!checked), [notificationMutation]);

    return (
        <div className="space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">User Permissions</h1>
                <p className="text-sm text-muted-foreground">Manage who can interact with your API through the API Management console.</p>
            </div>

            <Card className="px-4 py-3">
                <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-2">
                        <Switch
                            checked={notificationsEnabled}
                            onCheckedChange={handleNotificationToggle}
                            disabled={notificationMutation.isPending || !api}
                            aria-label="Toggle membership notifications"
                        />
                        <span className="text-sm">Notify members when they are added to the API</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <Button type="button" variant="outline" size="sm" onClick={() => setTransferOpen(true)}>
                            <UserCogIcon className="size-4" aria-hidden="true" />
                            Transfer ownership
                        </Button>
                        <Button type="button" variant="outline" size="sm" onClick={() => setManageGroupsOpen(true)}>
                            <UsersIcon className="size-4" aria-hidden="true" />
                            Manage groups
                        </Button>
                        <Button type="button" size="sm" onClick={() => setAddMembersOpen(true)}>
                            <PlusIcon className="size-4" aria-hidden="true" />
                            Add members
                        </Button>
                    </div>
                </div>
            </Card>

            <section className="space-y-3">
                <div className="space-y-1">
                    <div className="flex items-center gap-2">
                        <h2 className="text-base font-semibold">Direct Members</h2>
                        {!membersLoading ? (
                            <Badge variant="secondary" className="text-xs tabular-nums">
                                {members.length}
                            </Badge>
                        ) : null}
                    </div>
                    <p className="text-sm text-muted-foreground">
                        Users with direct access to this API. You can change their roles or remove them.
                    </p>
                </div>

                {membersLoading ? (
                    <div className="space-y-2">
                        {Array.from({ length: 3 }).map((_, i) => (
                            <Skeleton key={i} className="h-12 rounded-lg" />
                        ))}
                    </div>
                ) : members.length === 0 ? (
                    <Card>
                        <Empty>
                            <EmptyHeader>
                                <EmptyTitle>No direct members</EmptyTitle>
                                <EmptyDescription>Add members to grant direct access to this API.</EmptyDescription>
                            </EmptyHeader>
                            <EmptyContent />
                        </Empty>
                    </Card>
                ) : (
                    <DirectMembersTable
                        members={members}
                        roles={roleNames}
                        editState={editState}
                        onStartEdit={handleStartEdit}
                        onRoleChange={handleRoleChange}
                        onSaveRole={handleSaveRole}
                        onCancelEdit={handleCancelEdit}
                        onRemove={handleRemoveRequest}
                        isSaving={updateMutation.isPending}
                        isRemoving={deleteMutation.isPending}
                    />
                )}
            </section>

            <Separator />

            <section className="space-y-3">
                <div className="space-y-1">
                    <h2 className="text-base font-semibold">Group Inherited Members</h2>
                    <p className="text-sm text-muted-foreground">
                        These members have access through their group membership. Roles are managed at the group level.
                    </p>
                </div>

                {groupsLoading ? (
                    <div className="space-y-2">
                        {Array.from({ length: 2 }).map((_, i) => (
                            <Skeleton key={i} className="h-24 rounded-lg" />
                        ))}
                    </div>
                ) : groupEntries.length === 0 ? (
                    <Card>
                        <Empty>
                            <EmptyHeader>
                                <EmptyTitle>No group members</EmptyTitle>
                                <EmptyDescription>Add groups to this API to grant inherited access to their members.</EmptyDescription>
                            </EmptyHeader>
                            <EmptyContent />
                        </Empty>
                    </Card>
                ) : (
                    <div className="space-y-4">
                        {groupEntries.map(([groupName, groupMembers]) => (
                            <GroupMembersSection key={groupName} groupName={groupName} members={groupMembers} />
                        ))}
                    </div>
                )}
            </section>

            <RemoveMemberDialog
                member={memberToRemove}
                isRemoving={deleteMutation.isPending}
                onConfirm={handleRemoveConfirm}
                onCancel={handleRemoveCancel}
            />
            <AddMembersDialog
                open={addMembersOpen}
                roles={roleNames}
                existingMembers={members}
                onClose={() => setAddMembersOpen(false)}
                onAdd={(users, roleName) => addMutation.mutate({ users, roleName })}
                isAdding={addMutation.isPending}
            />
            <TransferOwnershipDialog
                open={transferOpen}
                members={members}
                roles={roleNames}
                onClose={() => setTransferOpen(false)}
                onTransfer={payload => transferMutation.mutate(payload)}
                isTransferring={transferMutation.isPending}
            />
            <ManageGroupsDialog
                open={manageGroupsOpen}
                allGroups={allGroups}
                currentGroupIds={currentGroupIds}
                onClose={() => setManageGroupsOpen(false)}
                onSave={groupIds => groupsMutation.mutate(groupIds)}
                isSaving={groupsMutation.isPending}
            />
        </div>
    );
}
