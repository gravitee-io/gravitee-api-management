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
    Badge,
    Button,
    DateCell,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Skeleton,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon, MailIcon, PlusIcon, SearchIcon, TriangleAlertIcon, UsersRoundIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { GroupAddMembersSheet } from '../features/groups/components/GroupAddMembersSheet';
import { GroupEditMemberSheet } from '../features/groups/components/GroupEditMemberSheet';
import { GroupInviteMemberSheet } from '../features/groups/components/GroupInviteMemberSheet';
import { GroupMembershipTable } from '../features/groups/components/GroupMembershipTable';
import { GroupMembersTable } from '../features/groups/components/GroupMembersTable';
import { GroupRemoveMemberSheet } from '../features/groups/components/GroupRemoveMemberSheet';
import {
    useGroupApis,
    useGroupApplications,
    useGroupApiProducts,
    useGroupDetail,
    useGroupMembers,
} from '../features/groups/hooks/useGroupDetail';
import { useAddGroupMembers, useInviteGroupMember, useRemoveGroupMember } from '../features/groups/hooks/useGroupMutations';
import {
    useGroupApiProductRoles,
    useGroupApiRoles,
    useGroupApplicationRoles,
    useGroupClusterRoles,
    useGroupIntegrationRoles,
} from '../features/groups/hooks/useGroupRoles';
import type { GroupMember, GroupMembershipPayload } from '../features/groups/types/group';
import { hasEventRule } from '../features/groups/utils/groupPayload';
import { ENVIRONMENT_GROUP_UPDATE_PERMISSION } from '../features/groups/utils/groupPermissions';
import { notify } from '../shared/notify';

type MemberSheetState = 'closed' | 'search' | 'invite';

function SectionError({ message }: Readonly<{ message: string }>) {
    return (
        <div className="flex items-center gap-2 rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2.5 text-sm text-destructive">
            <TriangleAlertIcon className="size-4 shrink-0" aria-hidden />
            {message}
        </div>
    );
}

// Classic Console's group.component.ts has no delete action on this screen at all — a group can only be
// deleted from the list (groups.component.ts). Editing group settings (name, default roles, invitation
// rules) also only happens from the list's Edit action, not this page — the detail page is read-only
// plus member management.
export function GroupDetailPage() {
    const { groupId } = useParams<{ groupId: string }>();
    const navigate = useNavigate();
    const canManageMembers = useHasPermission({ anyOf: [ENVIRONMENT_GROUP_UPDATE_PERMISSION] });

    const [memberSheet, setMemberSheet] = useState<MemberSheetState>('closed');
    const [editingMember, setEditingMember] = useState<GroupMember | null>(null);
    const [removingMember, setRemovingMember] = useState<GroupMember | null>(null);

    const { data: group, isLoading, isError } = useGroupDetail(groupId);
    const { data: members = [], isLoading: membersLoading, isError: membersError } = useGroupMembers(groupId);
    const { data: apis = [], isLoading: apisLoading, isError: apisError } = useGroupApis(groupId);
    const { data: applications = [], isLoading: applicationsLoading, isError: applicationsError } = useGroupApplications(groupId);
    const { data: apiProducts = [], isLoading: apiProductsLoading, isError: apiProductsError } = useGroupApiProducts(groupId);
    const { data: apiRoles = [] } = useGroupApiRoles();
    const { data: applicationRoles = [] } = useGroupApplicationRoles();
    const { data: apiProductRoles = [] } = useGroupApiProductRoles();
    const { data: integrationRoles = [] } = useGroupIntegrationRoles();
    const { data: clusterRoles = [] } = useGroupClusterRoles();

    const addMembersMutation = useAddGroupMembers();
    const inviteMemberMutation = useInviteGroupMember();
    const removeMemberMutation = useRemoveGroupMember();

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
            await inviteMemberMutation.mutateAsync({
                groupId,
                data: {
                    reference_id: groupId,
                    email: values.email,
                    api_role: values.apiRole || undefined,
                    application_role: values.applicationRole || undefined,
                },
            });
            notify.success(`Invitation sent to ${values.email}`);
            closeMemberSheet();
        } catch (error) {
            notify.error(error, 'Failed to send invitation');
        }
    }

    async function handleEditMemberRoles(payload: GroupMembershipPayload) {
        if (!groupId) return;
        try {
            await addMembersMutation.mutateAsync({ groupId, memberships: [payload] });
            notify.success('Member roles updated successfully');
            setEditingMember(null);
        } catch (error) {
            notify.error(error, 'Failed to update member roles');
        }
    }

    async function handleRemoveMember() {
        if (!groupId || !removingMember) return;
        try {
            await removeMemberMutation.mutateAsync({ groupId, memberId: removingMember.id });
            notify.success(`${removingMember.displayName} removed from the group`);
            setRemovingMember(null);
        } catch (error) {
            notify.error(error, 'Failed to remove member');
        }
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
                <Button type="button" variant="ghost" className="gap-1.5 px-0" onClick={() => navigate('..')}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to groups
                </Button>
                <p className="text-sm text-muted-foreground">Group not found or failed to load.</p>
            </div>
        );
    }

    return (
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
                </div>
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <h2 className="text-base font-semibold">Settings</h2>
                <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-3">
                    <div>
                        <dt className="text-sm text-muted-foreground">Default API role</dt>
                        <dd className="text-sm font-medium">{group.roles?.API ?? 'Not set'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Default API product role</dt>
                        <dd className="text-sm font-medium">{group.roles?.API_PRODUCT ?? 'Not set'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Default application role</dt>
                        <dd className="text-sm font-medium">{group.roles?.APPLICATION ?? 'Not set'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Lock API role</dt>
                        <dd className="text-sm font-medium">{group.lock_api_role ? 'Yes' : 'No'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Lock API product role</dt>
                        <dd className="text-sm font-medium">{group.lock_api_product_role ? 'Yes' : 'No'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Lock application role</dt>
                        <dd className="text-sm font-medium">{group.lock_application_role ? 'Yes' : 'No'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Maximum members</dt>
                        <dd className="text-sm font-medium">{group.max_invitation ?? 'Unlimited'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Invitation via search</dt>
                        <dd className="text-sm font-medium">{group.system_invitation ? 'Allowed' : 'Not allowed'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Invitation via email</dt>
                        <dd className="text-sm font-medium">{group.email_invitation ? 'Allowed' : 'Not allowed'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Notify on new members</dt>
                        <dd className="text-sm font-medium">{group.disable_membership_notifications ? 'No' : 'Yes'}</dd>
                    </div>
                </dl>
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <h2 className="text-base font-semibold">Members</h2>
                        <p className="text-sm text-muted-foreground">Direct members of this group and their scoped roles.</p>
                    </div>
                    {canManageMembers && (
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button type="button" className="shrink-0 gap-1.5">
                                    <PlusIcon className="size-4" aria-hidden />
                                    Add members
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                <DropdownMenuItem onSelect={() => setMemberSheet('search')}>
                                    <SearchIcon className="size-4 mr-2" aria-hidden />
                                    User search
                                </DropdownMenuItem>
                                <DropdownMenuItem onSelect={() => setMemberSheet('invite')}>
                                    <MailIcon className="size-4 mr-2" aria-hidden />
                                    Email invitation
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    )}
                </div>
                {membersError ? (
                    <SectionError message="Failed to load members. Please refresh and try again." />
                ) : (
                    <GroupMembersTable
                        members={members}
                        loading={membersLoading}
                        canManageMembers={canManageMembers}
                        onEditRoles={setEditingMember}
                        onRemove={setRemovingMember}
                    />
                )}
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <h2 className="text-base font-semibold">APIs</h2>
                {apisError ? (
                    <SectionError message="Failed to load associated APIs. Please refresh and try again." />
                ) : (
                    <GroupMembershipTable
                        items={apis}
                        loading={apisLoading}
                        ariaLabel="APIs"
                        searchPlaceholder="Search APIs…"
                        emptyTitle="No dependent APIs to display"
                        emptyDescription="APIs associated with this group will appear here."
                    />
                )}
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <h2 className="text-base font-semibold">API Products</h2>
                {apiProductsError ? (
                    <SectionError message="Failed to load associated API Products. Please refresh and try again." />
                ) : (
                    <GroupMembershipTable
                        items={apiProducts}
                        loading={apiProductsLoading}
                        ariaLabel="API Products"
                        searchPlaceholder="Search API Products…"
                        emptyTitle="No dependent API Products to display"
                        emptyDescription="API Products associated with this group will appear here."
                    />
                )}
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <h2 className="text-base font-semibold">Applications</h2>
                {applicationsError ? (
                    <SectionError message="Failed to load associated applications. Please refresh and try again." />
                ) : (
                    <GroupMembershipTable
                        items={applications}
                        loading={applicationsLoading}
                        ariaLabel="Applications"
                        searchPlaceholder="Search Applications…"
                        emptyTitle="No dependent applications to display"
                        emptyDescription="Applications associated with this group will appear here."
                        showVersionColumn={false}
                    />
                )}
            </section>

            <GroupAddMembersSheet
                open={memberSheet === 'search'}
                groupName={group.name}
                members={members}
                apiRoles={apiRoles}
                applicationRoles={applicationRoles}
                apiProductRoles={apiProductRoles}
                integrationRoles={integrationRoles}
                clusterRoles={clusterRoles}
                onClose={closeMemberSheet}
                onSubmit={handleAddMembers}
                isSaving={addMembersMutation.isPending}
            />

            <GroupInviteMemberSheet
                open={memberSheet === 'invite'}
                groupName={group.name}
                apiRoles={apiRoles}
                applicationRoles={applicationRoles}
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
                groupAllowsGroupAdmin={Boolean(group.system_invitation)}
                onClose={() => setEditingMember(null)}
                onSubmit={handleEditMemberRoles}
                isSaving={addMembersMutation.isPending}
            />

            <GroupRemoveMemberSheet
                open={removingMember !== null}
                member={removingMember ?? undefined}
                groupName={group.name}
                onClose={() => setRemovingMember(null)}
                onConfirm={handleRemoveMember}
                isRemoving={removeMemberMutation.isPending}
            />
        </div>
    );
}
