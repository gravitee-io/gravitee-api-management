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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    Badge,
    Button,
    DateCell,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Skeleton,
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from '@gravitee/graphene-core';
import {
    ArrowLeftIcon,
    InfoIcon,
    MailIcon,
    PencilIcon,
    PlusIcon,
    SearchIcon,
    Trash2Icon,
    UsersRoundIcon,
} from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { GroupAddMembersSheet } from '../features/groups/components/GroupAddMembersSheet';
import { GroupAssociationSection } from '../features/groups/components/GroupAssociationSection';
import { GroupDeleteSheet } from '../features/groups/components/GroupDeleteSheet';
import { GroupEditMemberSheet } from '../features/groups/components/GroupEditMemberSheet';
import { GroupInvitationsTable } from '../features/groups/components/GroupInvitationsTable';
import { GroupInviteMemberSheet } from '../features/groups/components/GroupInviteMemberSheet';
import { GroupMembersTable } from '../features/groups/components/GroupMembersTable';
import { GroupRemoveMemberSheet } from '../features/groups/components/GroupRemoveMemberSheet';
import { GroupSettingsSection } from '../features/groups/components/GroupSettingsSection';
import { GroupSheet, type GroupFormValues } from '../features/groups/components/GroupSheet';
import { GroupTooManyUsersDialog } from '../features/groups/components/GroupTooManyUsersDialog';
import { SectionError } from '../features/groups/components/SectionError';
import { useCurrentUserIsGroupAdmin } from '../features/groups/hooks/useCurrentUserGroupAdmin';
import {
    useEnvironmentSettings,
    useGroupApis,
    useGroupApplications,
    useGroupApiProducts,
    useGroupDetail,
    useGroupMembers,
} from '../features/groups/hooks/useGroupDetail';
import { useGroupMemberActions } from '../features/groups/hooks/useGroupMemberActions';
import { useDeleteGroup, useUpdateGroup } from '../features/groups/hooks/useGroupMutations';
import {
    useGroupApiProductRoles,
    useGroupApiRoles,
    useGroupApplicationRoles,
    useGroupClusterRoles,
    useGroupExplorerRoles,
    useGroupIntegrationRoles,
} from '../features/groups/hooks/useGroupRoles';
import { buildEventRules, buildRolesMap, hasEventRule, parseMaxInvitation } from '../features/groups/utils/groupPayload';
import {
    canInviteToGroup,
    ENVIRONMENT_GROUP_DELETE_PERMISSION,
    ENVIRONMENT_GROUP_UPDATE_PERMISSION,
} from '../features/groups/utils/groupPermissions';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { notify } from '../shared/notify';

export function GroupDetailPage() {
    const { groupId } = useParams<{ groupId: string }>();
    const navigate = useNavigate();
    const canEdit = useHasPermission({ anyOf: [ENVIRONMENT_GROUP_UPDATE_PERMISSION] });
    const canDelete = useHasPermission({ anyOf: [ENVIRONMENT_GROUP_DELETE_PERMISSION] });

    const [editOpen, setEditOpen] = useState(false);
    const [deleteOpen, setDeleteOpen] = useState(false);

    const { data: group, isLoading, isError } = useGroupDetail(groupId);
    const { data: members = [], isLoading: membersLoading, isError: membersError } = useGroupMembers(groupId);
    const { data: apis = [], isLoading: apisLoading, isError: apisError } = useGroupApis(groupId);
    const { data: applications = [], isLoading: applicationsLoading, isError: applicationsError } = useGroupApplications(groupId);
    const { data: apiProducts = [], isLoading: apiProductsLoading, isError: apiProductsError } = useGroupApiProducts(groupId);

    const {
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
    } = useGroupMemberActions(groupId);

    // Edit Group only needs API / application / API product catalogs. Search add-members needs all six;
    // edit-member also needs integration / cluster. Invite only uses API / application.
    const addMemberRolesNeeded = memberSheet === 'search';
    const extraMemberRolesNeeded = addMemberRolesNeeded || editingMember !== null;
    const defaultGroupRolesNeeded = editOpen || memberSheet !== 'closed' || editingMember !== null;
    const { data: apiRoles = [], isLoading: apiRolesLoading } = useGroupApiRoles({ enabled: defaultGroupRolesNeeded });
    const { data: applicationRoles = [], isLoading: applicationRolesLoading } = useGroupApplicationRoles({
        enabled: defaultGroupRolesNeeded,
    });
    const { data: apiProductRoles = [], isLoading: apiProductRolesLoading } = useGroupApiProductRoles({
        enabled: defaultGroupRolesNeeded,
    });
    const { data: integrationRoles = [] } = useGroupIntegrationRoles({ enabled: extraMemberRolesNeeded });
    const { data: clusterRoles = [] } = useGroupClusterRoles({ enabled: extraMemberRolesNeeded });
    const { data: explorerRoles = [] } = useGroupExplorerRoles({ enabled: addMemberRolesNeeded });
    const isCurrentUserGroupAdmin = useCurrentUserIsGroupAdmin(members);

    const maxInvitationsLimitReached = typeof group?.max_invitation === 'number' && group.max_invitation <= members.length;
    const canAddMembers = group !== undefined && (canEdit || canInviteToGroup(group)) && !maxInvitationsLimitReached;
    const canManageMemberActions = canEdit || isCurrentUserGroupAdmin;
    // Portal settings only matter for add-members PRIMARY_OWNER gating; skip when the user cannot open the sheet.
    const { data: environmentSettings } = useEnvironmentSettings({ enabled: canAddMembers });

    const updateMutation = useUpdateGroup();
    const deleteMutation = useDeleteGroup();

    async function handleUpdate(values: GroupFormValues) {
        if (!group) return;
        try {
            await updateMutation.mutateAsync({
                groupId: group.id,
                data: {
                    name: values.name,
                    lock_api_role: values.lockApiRole,
                    lock_api_product_role: values.lockApiProductRole,
                    lock_application_role: values.lockApplicationRole,
                    event_rules: buildEventRules({
                        apiCreate: values.defaultGroupForNewApis,
                        applicationCreate: values.defaultGroupForNewApplications,
                        apiProductCreate: values.defaultGroupForNewApiProducts,
                    }),
                    roles: buildRolesMap(group.roles, values.apiRole, values.applicationRole, values.apiProductRole),
                    max_invitation: parseMaxInvitation(values.maxInvitation),
                    system_invitation: values.systemInvitation,
                    email_invitation: values.emailInvitation,
                    disable_membership_notifications: !values.notifyOnMemberAdded,
                },
            });
            notify.success('Group updated successfully');
            setEditOpen(false);
        } catch (error) {
            notify.error(error, 'Failed to update group');
        }
    }

    async function handleDelete() {
        if (!group) return;
        try {
            await deleteMutation.mutateAsync(group.id);
            notify.success('Group deleted successfully');
            setDeleteOpen(false);
            navigate('..');
        } catch (error) {
            notify.error(error, 'Failed to delete group');
        }
    }

    function handleDeleteInvitationDialogOpenChange(isOpen: boolean) {
        if (isOpen || deleteInvitationMutation.isPending) return;
        setDeletingInvitation(null);
    }

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-8 w-32" />
                <Skeleton className="h-32 w-full rounded-xl" />
                <Skeleton className="h-56 w-full rounded-xl" />
            </div>
        );
    }

    if (isError || !group) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0" asChild>
                    <Link to="..">
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to groups
                    </Link>
                </Button>
                <p className="text-sm text-muted-foreground">Group not found or failed to load.</p>
            </div>
        );
    }

    return (
        <>
            <div className="space-y-6">
                <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                    <Link to="..">
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to groups
                    </Link>
                </Button>

                <section className="rounded-xl border bg-card p-5">
                    <div className="flex items-start justify-between gap-4">
                        <div className="flex min-w-0 items-start gap-3">
                            <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-orange-100 text-orange-700">
                                <UsersRoundIcon className="size-5" aria-hidden />
                            </div>
                            <div className="min-w-0 space-y-1">
                                <div className="flex flex-wrap items-center gap-2">
                                    <h1 className="text-xl font-semibold tracking-tight">{group.name}</h1>
                                    {hasEventRule(group, 'API_CREATE') && (
                                        <Badge variant="default" className="text-xs font-normal">
                                            Auto APIs
                                        </Badge>
                                    )}
                                    {hasEventRule(group, 'API_PRODUCT_CREATE') && (
                                        <Badge variant="default" className="text-xs font-normal">
                                            Auto API Products
                                        </Badge>
                                    )}
                                    {hasEventRule(group, 'APPLICATION_CREATE') && (
                                        <Badge variant="default" className="text-xs font-normal">
                                            Auto Applications
                                        </Badge>
                                    )}
                                </div>
                                {group.created_at && (
                                    <p className="text-sm text-muted-foreground">
                                        Created <DateCell value={new Date(group.created_at)} format="absolute" />
                                        {group.updated_at && group.updated_at !== group.created_at && (
                                            <>
                                                {' '}
                                                · Updated <DateCell value={new Date(group.updated_at)} format="absolute" />
                                            </>
                                        )}
                                    </p>
                                )}
                            </div>
                        </div>
                        {(canEdit || canDelete) && (
                            <div className="flex shrink-0 items-center gap-2">
                                {canEdit && (
                                    <Button type="button" variant="outline" size="sm" className="gap-1.5" onClick={() => setEditOpen(true)}>
                                        <PencilIcon className="size-4" aria-hidden />
                                        Edit group
                                    </Button>
                                )}
                                {canDelete && (
                                    <Button
                                        type="button"
                                        variant="destructive"
                                        size="sm"
                                        className="gap-1.5"
                                        onClick={() => setDeleteOpen(true)}
                                    >
                                        <Trash2Icon className="size-4" aria-hidden />
                                        Delete
                                    </Button>
                                )}
                            </div>
                        )}
                    </div>
                </section>

                <GroupSettingsSection group={group} />

                <section className="space-y-4 rounded-xl border bg-card p-5">
                    <div className="flex items-start justify-between gap-4">
                        <div>
                            <h2 className="text-base font-semibold">Members</h2>
                            <p className="text-sm text-muted-foreground">Direct members of this group and their scoped roles.</p>
                        </div>
                        {canAddMembers && (
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button type="button" className="shrink-0 gap-1.5">
                                        <PlusIcon className="size-4" aria-hidden />
                                        Add members
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem onSelect={() => setMemberSheet('search')} disabled={!group.system_invitation}>
                                        <SearchIcon className="size-4 mr-2" aria-hidden />
                                        User search
                                    </DropdownMenuItem>
                                    <DropdownMenuItem onSelect={() => setMemberSheet('invite')} disabled={!group.email_invitation}>
                                        <MailIcon className="size-4 mr-2" aria-hidden />
                                        Email invitation
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        )}
                    </div>
                    {maxInvitationsLimitReached && (
                        <Alert variant="default">
                            <InfoIcon className="size-4" aria-hidden />
                            <AlertDescription>
                                The number of members in this group has reached maximum allowed. Adding users via search and email
                                invitation have been disabled.
                            </AlertDescription>
                        </Alert>
                    )}
                    <Tabs value={memberTab} onValueChange={value => setMemberTab(value as 'members' | 'invitations')}>
                        <TabsList variant="line">
                            <TabsTrigger value="members">Members</TabsTrigger>
                            <TabsTrigger value="invitations">Invitations</TabsTrigger>
                        </TabsList>
                        <TabsContent value="members">
                            {membersError ? (
                                <SectionError message="Failed to load members. Please refresh and try again." />
                            ) : (
                                <GroupMembersTable
                                    members={members}
                                    loading={membersLoading}
                                    canManageMembers={canManageMemberActions}
                                    canAddMembers={canAddMembers}
                                    onEditRoles={setEditingMember}
                                    onRemove={setRemovingMember}
                                />
                            )}
                        </TabsContent>
                        <TabsContent value="invitations">
                            {invitationsError ? (
                                <SectionError message="Failed to load invitations. Please refresh and try again." />
                            ) : (
                                <GroupInvitationsTable
                                    invitations={invitations}
                                    loading={invitationsLoading}
                                    canManageMembers={canManageMemberActions}
                                    onDelete={setDeletingInvitation}
                                />
                            )}
                        </TabsContent>
                    </Tabs>
                </section>

                <GroupAssociationSection
                    title="APIs"
                    error={apisError}
                    errorMessage="Failed to load associated APIs. Please refresh and try again."
                    items={apis}
                    loading={apisLoading}
                    ariaLabel="APIs"
                    searchPlaceholder="Search APIs…"
                    emptyTitle="No dependent APIs to display"
                />

                <GroupAssociationSection
                    title="API Products"
                    error={apiProductsError}
                    errorMessage="Failed to load associated API Products. Please refresh and try again."
                    items={apiProducts}
                    loading={apiProductsLoading}
                    ariaLabel="API Products"
                    searchPlaceholder="Search API Products…"
                    emptyTitle="No dependent API Products to display"
                />

                <GroupAssociationSection
                    title="Applications"
                    error={applicationsError}
                    errorMessage="Failed to load associated applications. Please refresh and try again."
                    items={applications}
                    loading={applicationsLoading}
                    ariaLabel="Applications"
                    searchPlaceholder="Search Applications…"
                    emptyTitle="No dependent applications to display"
                    showVersionColumn={false}
                />
            </div>

            <GroupSheet
                open={editOpen}
                mode="edit"
                group={group}
                apiRoles={apiRoles}
                applicationRoles={applicationRoles}
                apiProductRoles={apiProductRoles}
                rolesLoading={apiRolesLoading || applicationRolesLoading || apiProductRolesLoading}
                onClose={() => setEditOpen(false)}
                onSubmit={handleUpdate}
                isSaving={updateMutation.isPending}
            />

            <GroupDeleteSheet
                open={deleteOpen}
                group={group}
                onClose={() => setDeleteOpen(false)}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />

            <GroupAddMembersSheet
                open={memberSheet === 'search'}
                groupName={group.name}
                groupRoles={group.roles}
                members={members}
                apiRoles={apiRoles}
                applicationRoles={applicationRoles}
                apiProductRoles={apiProductRoles}
                integrationRoles={integrationRoles}
                clusterRoles={clusterRoles}
                explorerRoles={explorerRoles}
                lockApiRole={Boolean(group.lock_api_role)}
                lockApiProductRole={Boolean(group.lock_api_product_role)}
                lockApplicationRole={Boolean(group.lock_application_role)}
                canOverrideLocks={canEdit}
                maxInvitation={group.max_invitation ?? null}
                apiPrimaryOwnerMode={environmentSettings?.api?.primaryOwnerMode}
                apiProductPrimaryOwnerMode={environmentSettings?.apiProduct?.primaryOwnerMode}
                onClose={closeMemberSheet}
                onSubmit={handleAddMembers}
                isSaving={addMembersMutation.isPending}
            />

            <GroupInviteMemberSheet
                open={memberSheet === 'invite'}
                groupName={group.name}
                groupRoles={group.roles}
                members={members}
                apiRoles={apiRoles}
                applicationRoles={applicationRoles}
                lockApiRole={Boolean(group.lock_api_role)}
                lockApplicationRole={Boolean(group.lock_application_role)}
                canOverrideLocks={canEdit}
                onClose={closeMemberSheet}
                onSubmit={handleInviteMember}
                isSaving={inviteMemberMutation.isPending}
            />

            <GroupEditMemberSheet
                open={editingMember !== null}
                groupName={group.name}
                member={editingMember ?? undefined}
                members={members}
                apiRoles={apiRoles}
                applicationRoles={applicationRoles}
                apiProductRoles={apiProductRoles}
                integrationRoles={integrationRoles}
                clusterRoles={clusterRoles}
                lockApiRole={Boolean(group.lock_api_role)}
                lockApiProductRole={Boolean(group.lock_api_product_role)}
                lockApplicationRole={Boolean(group.lock_application_role)}
                canOverrideLocks={canEdit}
                groupAllowsGroupAdmin={Boolean(group.system_invitation)}
                onClose={() => setEditingMember(null)}
                onSubmit={handleEditMemberRoles}
                isSaving={addMembersMutation.isPending}
            />

            <GroupRemoveMemberSheet
                open={removingMember !== null}
                member={removingMember ?? undefined}
                members={members}
                groupName={group.name}
                onClose={() => setRemovingMember(null)}
                onConfirm={handleRemoveMember}
                isRemoving={removeMemberMutation.isPending}
            />

            <GroupTooManyUsersDialog
                open={tooManyUsersEmail !== null}
                email={tooManyUsersEmail}
                onClose={() => setTooManyUsersEmail(null)}
                onContinue={handleTooManyUsersContinue}
            />

            <ConfirmDialog
                open={deletingInvitation !== null}
                onOpenChange={handleDeleteInvitationDialogOpenChange}
                title="Delete Invitation"
                description={`You are trying to delete an invitation sent to ${deletingInvitation?.email}. Do you want to continue?`}
                confirmLabel="Continue"
                pendingLabel="Deleting…"
                isPending={deleteInvitationMutation.isPending}
                onConfirm={handleDeleteInvitation}
            />
        </>
    );
}
