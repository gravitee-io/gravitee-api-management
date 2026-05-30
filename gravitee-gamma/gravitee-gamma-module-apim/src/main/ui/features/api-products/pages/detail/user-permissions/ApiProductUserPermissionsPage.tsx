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
import { useCallback, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { AddMembers } from '../../../../apis/components/user-permissions/AddMembers';
import { DirectMembersTable } from '../../../../apis/components/user-permissions/DirectMembersTable';
import { GroupMembersSection } from '../../../../apis/components/user-permissions/GroupMembersSection';
import { ManageGroups } from '../../../../apis/components/user-permissions/ManageGroups';
import { type EditState, getApiProductRole } from '../../../../apis/components/user-permissions/memberHelpers';
import { RemoveMemberDialog } from '../../../../apis/components/user-permissions/RemoveMemberDialog';
import { TransferOwnership } from '../../../../apis/components/user-permissions/TransferOwnership';
import { useGroups } from '../../../../apis/hooks/useGroups';
import type { Member, SearchableUser, TransferOwnershipPayload } from '../../../../apis/types/members.types';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';
import { useApiProductGroupMembers } from '../../../hooks/useApiProductGroupMembers';
import { useApiProductMembers, useApiProductRoles } from '../../../hooks/useApiProductMembers';
import { useUpdateApiProduct } from '../../../hooks/useUpdateApiProduct';
import {
    addApiProductMember,
    deleteApiProductMember,
    transferApiProductOwnership,
    updateApiProductMember,
} from '../../../services/apiProductMembers';
import { apiProductKeys } from '../../../utils/queryKeys';

export function ApiProductUserPermissionsPage() {
    const { productId } = useParams<{ productId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const { product } = useApiProductDetailContext();

    const [editState, setEditState] = useState<EditState>(null);
    const [memberToRemove, setMemberToRemove] = useState<Member | null>(null);
    const [addMembersOpen, setAddMembersOpen] = useState(false);
    const [transferOpen, setTransferOpen] = useState(false);
    const [manageGroupsOpen, setManageGroupsOpen] = useState(false);

    const { data: membersData, isLoading: membersLoading } = useApiProductMembers(productId);
    const { data: rolesData } = useApiProductRoles();
    const { data: groupsData } = useGroups();
    const { mutate: updateProduct, isPending: isProductUpdating } = useUpdateApiProduct(productId ?? '');

    const members = membersData?.data ?? [];
    const roleNames = useMemo(() => (rolesData ?? []).filter(r => r.name !== 'PRIMARY_OWNER').map(r => r.name), [rolesData]);
    const allGroups = useMemo(() => groupsData?.data ?? [], [groupsData]);
    const currentGroupIds = useMemo(() => product?.groups ?? [], [product]);
    const currentGroups = useMemo(() => allGroups.filter(g => currentGroupIds.includes(g.id)), [allGroups, currentGroupIds]);

    const { data: groupMembersMap, isLoading: groupMembersLoading } = useApiProductGroupMembers(productId, currentGroups);
    const groupEntries = useMemo(() => Object.entries(groupMembersMap).filter(([, gm]) => gm.length > 0), [groupMembersMap]);

    const addMutation = useMutation({
        mutationFn: ({ users, roleName }: { users: SearchableUser[]; roleName: string }) =>
            Promise.all(
                users.map(user =>
                    addApiProductMember(env!.id, productId!, {
                        userId: user.id ?? undefined,
                        externalReference: user.reference,
                        roleName,
                    }),
                ),
            ),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiProductKeys.members(env!.id, productId!) });
            setAddMembersOpen(false);
        },
    });

    const updateMutation = useMutation({
        mutationFn: ({ memberId, roleName }: { memberId: string; roleName: string }) =>
            updateApiProductMember(env!.id, productId!, memberId, roleName),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiProductKeys.members(env!.id, productId!) });
            setEditState(null);
        },
    });

    const deleteMutation = useMutation({
        mutationFn: (memberId: string) => deleteApiProductMember(env!.id, productId!, memberId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiProductKeys.members(env!.id, productId!) });
            setMemberToRemove(null);
        },
    });

    const transferMutation = useMutation({
        mutationFn: (payload: TransferOwnershipPayload) => transferApiProductOwnership(env!.id, productId!, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiProductKeys.members(env!.id, productId!) });
            queryClient.invalidateQueries({ queryKey: apiProductKeys.detail(env!.id, productId!) });
            setTransferOpen(false);
        },
    });

    function handleSaveGroups(groupIds: string[]) {
        if (!product) return;
        updateProduct(
            {
                name: product.name,
                version: product.version,
                description: product.description,
                apiIds: product.apiIds ?? [],
                groups: groupIds,
                disableMembershipNotifications: product.disableMembershipNotifications,
            },
            { onSuccess: () => setManageGroupsOpen(false) },
        );
    }

    const notificationsEnabled = !(product?.disableMembershipNotifications ?? false);

    const handleNotificationToggle = useCallback(
        (checked: boolean) => {
            if (!product) return;
            updateProduct({
                name: product.name,
                version: product.version,
                description: product.description,
                apiIds: product.apiIds ?? [],
                groups: product.groups,
                disableMembershipNotifications: !checked,
            });
        },
        [product, updateProduct],
    );

    const handleStartEdit = useCallback((member: Member) => setEditState({ memberId: member.id, role: getApiProductRole(member) }), []);
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

    return (
        <div className="space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">User Permissions</h1>
                <p className="text-sm text-muted-foreground">Manage who can access and manage this API product.</p>
            </div>

            <Card className="px-4 py-3">
                <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-2">
                        <Switch
                            checked={notificationsEnabled}
                            onCheckedChange={handleNotificationToggle}
                            disabled={isProductUpdating || !product}
                            aria-label="Toggle membership notifications"
                        />
                        <span className="text-sm">Notify members when they are added to the product</span>
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
                        Users with direct access to this product. You can change their roles or remove them.
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
                                <EmptyDescription>Add members to grant direct access to this product.</EmptyDescription>
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
                        getRoleName={getApiProductRole}
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

                {groupMembersLoading ? (
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
                                <EmptyDescription>Add groups to this product to grant inherited access to their members.</EmptyDescription>
                            </EmptyHeader>
                            <EmptyContent />
                        </Empty>
                    </Card>
                ) : (
                    <div className="space-y-4">
                        {groupEntries.map(([groupName, groupMembers]) => (
                            <GroupMembersSection key={groupName} groupName={groupName} members={groupMembers} roleScope="API_PRODUCT" />
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
            <AddMembers
                open={addMembersOpen}
                roles={roleNames}
                existingMembers={members}
                onClose={() => setAddMembersOpen(false)}
                onAdd={(users, roleName) => addMutation.mutate({ users, roleName })}
                isAdding={addMutation.isPending}
            />
            <TransferOwnership
                open={transferOpen}
                members={members}
                roles={roleNames}
                onClose={() => setTransferOpen(false)}
                onTransfer={payload => transferMutation.mutate(payload)}
                isTransferring={transferMutation.isPending}
            />
            <ManageGroups
                open={manageGroupsOpen}
                allGroups={allGroups}
                currentGroupIds={currentGroupIds}
                onClose={() => setManageGroupsOpen(false)}
                onSave={handleSaveGroups}
                isSaving={isProductUpdating}
            />
        </div>
    );
}
