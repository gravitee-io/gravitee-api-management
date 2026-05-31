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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    Badge,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Skeleton,
    Switch,
} from '@gravitee/graphene-core';
import { PlusIcon, UserCogIcon, UsersIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { AddMembersDialog } from '../features/applications/components/user-permissions/AddMembersDialog';
import { DirectMembersTable } from '../features/applications/components/user-permissions/DirectMembersTable';
import { EditRoleDialog } from '../features/applications/components/user-permissions/EditRoleDialog';
import { GroupMembersSection } from '../features/applications/components/user-permissions/GroupMembersSection';
import { ManageGroupsDialog } from '../features/applications/components/user-permissions/ManageGroupsDialog';
import { formatAddMembersResultMessage, getApplicationRole } from '../features/applications/components/user-permissions/memberHelpers';
import { RemoveMemberDialog } from '../features/applications/components/user-permissions/RemoveMemberDialog';
import { TransferOwnershipDialog } from '../features/applications/components/user-permissions/TransferOwnershipDialog';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';
import { useApplicationGroupMembers } from '../features/applications/hooks/useApplicationGroupMembers';
import { useApplicationMemberPermissions } from '../features/applications/hooks/useApplicationMemberPermissions';
import {
    useApplicationAssociatedGroups,
    useApplicationMembers,
    useApplicationRoles,
    useEnvironmentGroups,
} from '../features/applications/hooks/useApplicationMembers';
import {
    addApplicationMember,
    deleteApplicationMember,
    transferApplicationOwnership,
    updateApplicationGroups,
    updateApplicationMembershipNotifications,
    updateApplicationMember,
} from '../features/applications/services/applicationMembers';
import type {
    ApplicationTransferOwnershipPayload,
    ApplicationUiMember,
    SearchableUser,
} from '../features/applications/types/applicationMembers.types';
import { toApplicationMemberEntity } from '../features/applications/utils/applicationMemberMapper';
import { applicationDetailKeys, applicationMemberKeys } from '../features/applications/utils/queryKeys';
import { notify } from '../shared/notify';

class AddMembersMutationError extends Error {
    public readonly succeededCount: number;

    constructor(message: string, succeededCount: number) {
        super(message);
        this.name = 'AddMembersMutationError';
        this.succeededCount = succeededCount;
    }
}

export function ApplicationUserPermissionsPage() {
    const { applicationId } = useParams<{ applicationId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const { application, permissionsReady } = useApplicationDetailContext();
    const { canCreate, canUpdate, canDelete } = useApplicationMemberPermissions();
    const canUpdateDefinitionPermission = useHasPermission({ anyOf: ['application-definition-u'] });
    const canUpdateDefinition = permissionsReady && canUpdateDefinitionPermission;

    const isReadOnly = application?.origin === 'KUBERNETES';

    const [memberToEdit, setMemberToEdit] = useState<ApplicationUiMember | null>(null);
    const [memberToRemove, setMemberToRemove] = useState<ApplicationUiMember | null>(null);
    const [addMembersOpen, setAddMembersOpen] = useState(false);
    const [transferOpen, setTransferOpen] = useState(false);
    const [manageGroupsOpen, setManageGroupsOpen] = useState(false);

    const { data: members = [], isLoading: membersLoading, isError: membersError } = useApplicationMembers(applicationId);
    const { data: rolesData } = useApplicationRoles();
    const { data: groupsData } = useEnvironmentGroups();

    const roleNames = useMemo(() => (rolesData ?? []).filter(r => r.name !== 'PRIMARY_OWNER').map(r => r.name), [rolesData]);
    const allGroups = useMemo(() => groupsData?.data ?? [], [groupsData]);
    const currentGroupIds = useMemo(() => application?.groups ?? [], [application]);
    const { data: associatedGroups = [], isLoading: associatedGroupsLoading } = useApplicationAssociatedGroups(currentGroupIds);

    const currentGroups = useMemo(() => {
        if (associatedGroups.length > 0) {
            return associatedGroups;
        }
        const fromCatalog = allGroups.filter(g => currentGroupIds.includes(g.id));
        if (fromCatalog.length > 0) {
            return fromCatalog;
        }
        return currentGroupIds.map(id => ({ id, name: id }));
    }, [associatedGroups, allGroups, currentGroupIds]);

    const { views: groupMemberViews, isLoading: groupMembersLoading } = useApplicationGroupMembers(applicationId, currentGroups);

    const addMutation = useMutation({
        mutationFn: async ({ users, roleName }: { users: SearchableUser[]; roleName: string }) => {
            const results = await Promise.allSettled(
                users.map(user =>
                    addApplicationMember(env!.id, applicationId!, {
                        id: user.id ?? user.reference,
                        role: roleName,
                    }),
                ),
            );

            let succeededCount = 0;
            const failed: { user: SearchableUser; reason: string }[] = [];

            results.forEach((result, index) => {
                if (result.status === 'fulfilled') {
                    succeededCount += 1;
                } else {
                    const reason = result.reason instanceof Error ? result.reason.message : String(result.reason);
                    failed.push({ user: users[index]!, reason });
                }
            });

            if (failed.length > 0) {
                throw new AddMembersMutationError(formatAddMembersResultMessage(users.length, succeededCount, failed), succeededCount);
            }
        },
        onSuccess: () => {
            if (env?.id && applicationId) {
                void queryClient.invalidateQueries({
                    queryKey: applicationMemberKeys.list(env.id, applicationId),
                });
            }
        },
        onError: error => {
            notify.error(error);
            if (error instanceof AddMembersMutationError && error.succeededCount > 0 && env?.id && applicationId) {
                // Partial success: refresh the list even though the mutation is considered failed.
                void queryClient.invalidateQueries({
                    queryKey: applicationMemberKeys.list(env.id, applicationId),
                });
            }
        },
    });

    const updateMutation = useMutation({
        mutationFn: ({ member, roleName }: { member: ApplicationUiMember; roleName: string }) =>
            updateApplicationMember(env!.id, applicationId!, toApplicationMemberEntity(member, roleName)),
        onSuccess: () => {
            if (env?.id && applicationId) {
                void queryClient.invalidateQueries({
                    queryKey: applicationMemberKeys.list(env.id, applicationId),
                });
            }
            setMemberToEdit(null);
        },
    });

    const deleteMutation = useMutation({
        mutationFn: (memberId: string) => deleteApplicationMember(env!.id, applicationId!, memberId),
        onSuccess: () => {
            if (env?.id && applicationId) {
                void queryClient.invalidateQueries({ queryKey: applicationMemberKeys.list(env.id, applicationId) });
            }
            setMemberToRemove(null);
        },
    });

    const transferMutation = useMutation({
        mutationFn: (payload: ApplicationTransferOwnershipPayload) => transferApplicationOwnership(env!.id, applicationId!, payload),
        onSuccess: () => {
            if (env?.id && applicationId) {
                void queryClient.invalidateQueries({ queryKey: applicationMemberKeys.list(env.id, applicationId) });
                void queryClient.invalidateQueries({ queryKey: applicationDetailKeys.detail(env.id, applicationId) });
            }
            setTransferOpen(false);
        },
    });

    const groupsMutation = useMutation({
        mutationFn: (groupIds: string[]) => {
            if (!application) throw new Error('Application is not loaded');
            return updateApplicationGroups(env!.id, application, groupIds);
        },
        onSuccess: () => {
            if (env?.id && applicationId) {
                void queryClient.invalidateQueries({ queryKey: applicationDetailKeys.detail(env.id, applicationId) });
            }
            void queryClient.invalidateQueries({ queryKey: applicationMemberKeys.all });
            setManageGroupsOpen(false);
        },
    });

    const notificationMutation = useMutation({
        mutationFn: (disable: boolean) => {
            if (!application) throw new Error('Application is not loaded');
            return updateApplicationMembershipNotifications(env!.id, application, disable);
        },
        onSuccess: () => {
            if (env?.id && applicationId) {
                void queryClient.invalidateQueries({ queryKey: applicationDetailKeys.detail(env.id, applicationId) });
            }
        },
    });

    const handleEditRole = useCallback(
        (member: ApplicationUiMember) => {
            if (!canUpdate || isReadOnly) return;
            setMemberToEdit(member);
        },
        [canUpdate, isReadOnly],
    );
    const handleSaveEditRole = useCallback(
        (roleName: string) => {
            if (!memberToEdit || !canUpdate || isReadOnly) return;
            updateMutation.mutate(
                { member: memberToEdit, roleName },
                {
                    onSuccess: () => notify.success('Changes successfully saved!'),
                    onError: error => notify.error(error),
                },
            );
        },
        [canUpdate, isReadOnly, memberToEdit, updateMutation],
    );
    const handleCloseEditRole = useCallback(() => setMemberToEdit(null), []);
    const handleRemoveRequest = useCallback(
        (member: ApplicationUiMember) => {
            if (!canDelete || isReadOnly) return;
            setMemberToRemove(member);
        },
        [canDelete, isReadOnly],
    );
    const handleRemoveConfirm = useCallback(() => {
        if (!memberToRemove) return;
        const displayName = memberToRemove.displayName;
        deleteMutation.mutate(memberToRemove.id, {
            onSuccess: () => notify.success(`"${displayName}" has been deleted`),
            onError: error => notify.error(error),
        });
    }, [memberToRemove, deleteMutation]);
    const handleRemoveCancel = useCallback(() => setMemberToRemove(null), []);

    const handleAddMembers = useCallback(
        async (users: SearchableUser[], roleName: string) => {
            try {
                await addMutation.mutateAsync({ users, roleName });
                notify.success('Changes successfully saved!');
                setAddMembersOpen(false);
            } catch {
                // Error is surfaced via notify in addMutation.onError
            }
        },
        [addMutation],
    );

    const notificationsEnabled = !(application?.disable_membership_notifications ?? false);
    const handleNotificationToggle = useCallback(
        (checked: boolean) =>
            notificationMutation.mutate(!checked, {
                onSuccess: () => notify.success('Changes successfully saved!'),
                onError: error => notify.error(error),
            }),
        [notificationMutation],
    );

    const memberActionsDisabled = isReadOnly || !canUpdate;
    const definitionActionsDisabled = isReadOnly || !canUpdateDefinition;

    return (
        <div className="space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">User Permissions</h1>
                <p className="text-sm text-muted-foreground">Manage who can access and administer this application in the console.</p>
            </div>

            <Card className="px-4 py-3">
                <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-2">
                        <Switch
                            checked={notificationsEnabled}
                            onCheckedChange={handleNotificationToggle}
                            disabled={definitionActionsDisabled || notificationMutation.isPending || !application}
                            aria-label="Toggle membership notifications"
                        />
                        <span className="text-sm">Notify members when they are added to the application</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setTransferOpen(true)}
                            disabled={memberActionsDisabled}
                        >
                            <UserCogIcon className="size-4" aria-hidden="true" />
                            Transfer ownership
                        </Button>
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setManageGroupsOpen(true)}
                            disabled={definitionActionsDisabled}
                        >
                            <UsersIcon className="size-4" aria-hidden="true" />
                            Manage groups
                        </Button>
                        <Button type="button" size="sm" onClick={() => setAddMembersOpen(true)} disabled={isReadOnly || !canCreate}>
                            <PlusIcon className="size-4" aria-hidden="true" />
                            Add members
                        </Button>
                    </div>
                </div>
            </Card>

            <Card>
                <CardHeader className="pb-3">
                    <div className="flex items-center gap-2">
                        <CardTitle className="text-base">Direct Members</CardTitle>
                        {!membersLoading ? (
                            <Badge variant="secondary" className="text-xs tabular-nums">
                                {members.length}
                            </Badge>
                        ) : null}
                    </div>
                    <CardDescription className="text-sm">
                        Users with direct access to this application. You can change their roles or remove them.
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-3">
                    {membersLoading ? (
                        <div className="space-y-2">
                            {Array.from({ length: 3 }).map((_, i) => (
                                <Skeleton key={i} className="h-12 rounded-lg" />
                            ))}
                        </div>
                    ) : membersError ? (
                        <Alert variant="destructive">
                            <AlertDescription>Failed to load application members. Please try again.</AlertDescription>
                        </Alert>
                    ) : members.length === 0 ? (
                        <Empty>
                            <EmptyHeader>
                                <EmptyTitle>No direct members</EmptyTitle>
                                <EmptyDescription>Add members to grant direct access to this application.</EmptyDescription>
                            </EmptyHeader>
                            <EmptyContent />
                        </Empty>
                    ) : (
                        <DirectMembersTable
                            members={members}
                            onEditRole={handleEditRole}
                            onRemove={handleRemoveRequest}
                            isRemoving={deleteMutation.isPending}
                            getRoleName={getApplicationRole}
                            canManageMembers={!isReadOnly && (canUpdate || canDelete)}
                            canEditRole={canUpdate}
                            canRemoveMember={canDelete}
                        />
                    )}
                </CardContent>
            </Card>

            {currentGroupIds.length > 0 ? (
                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="text-base">Group Inherited Members</CardTitle>
                        <CardDescription className="text-sm">
                            These members have access through their group membership. Their roles are managed at the group level.
                        </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-3">
                        {associatedGroupsLoading ? (
                            <div className="space-y-2">
                                {Array.from({ length: Math.min(currentGroupIds.length, 2) }).map((_, i) => (
                                    <Skeleton key={`group-skeleton-${i}`} className="h-24 rounded-lg" />
                                ))}
                            </div>
                        ) : (
                            <div className="space-y-4">
                                {groupMemberViews.map(({ group, members, isLoading: groupLoading, isError }) => (
                                    <div key={group.id} className="space-y-2">
                                        {isError ? (
                                            <Alert variant="destructive">
                                                <AlertDescription>Failed to load members for group {group.name}.</AlertDescription>
                                            </Alert>
                                        ) : null}
                                        <GroupMembersSection
                                            groupName={group.name}
                                            members={members}
                                            isLoading={groupLoading || (groupMembersLoading && members.length === 0)}
                                        />
                                    </div>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>
            ) : null}

            <EditRoleDialog
                member={memberToEdit}
                roles={roleNames}
                onClose={handleCloseEditRole}
                onSave={handleSaveEditRole}
                isSaving={updateMutation.isPending}
            />
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
                onAdd={(users, roleName) => void handleAddMembers(users, roleName)}
                isAdding={addMutation.isPending}
            />
            <TransferOwnershipDialog
                open={transferOpen}
                members={members}
                roles={roleNames}
                onClose={() => setTransferOpen(false)}
                onTransfer={payload =>
                    transferMutation.mutate(payload, {
                        onSuccess: () => notify.success('Transfer ownership done.'),
                        onError: error => notify.error(error),
                    })
                }
                isTransferring={transferMutation.isPending}
            />
            <ManageGroupsDialog
                open={manageGroupsOpen}
                allGroups={allGroups}
                currentGroupIds={currentGroupIds}
                onClose={() => setManageGroupsOpen(false)}
                onSave={groupIds =>
                    groupsMutation.mutate(groupIds, {
                        onSuccess: () => notify.success('Changes successfully saved!'),
                        onError: error => notify.error(error),
                    })
                }
                isSaving={groupsMutation.isPending}
            />
        </div>
    );
}
