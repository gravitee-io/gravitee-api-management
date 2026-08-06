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
import { Badge, Button, DateCell, Skeleton } from '@gravitee/graphene-core';
import {
    ArrowLeftIcon,
    LockIcon,
    MailIcon,
    PencilIcon,
    SearchIcon,
    Trash2Icon,
    TriangleAlertIcon,
    UsersRoundIcon,
} from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { GroupDeleteSheet } from '../features/groups/components/GroupDeleteSheet';
import { GroupMembershipTable } from '../features/groups/components/GroupMembershipTable';
import { GroupMembersTable } from '../features/groups/components/GroupMembersTable';
import { GroupSheet, type GroupFormValues } from '../features/groups/components/GroupSheet';
import {
    useGroupApis,
    useGroupApplications,
    useGroupApiProducts,
    useGroupDetail,
    useGroupMembers,
} from '../features/groups/hooks/useGroupDetail';
import { useDeleteGroup, useUpdateGroup } from '../features/groups/hooks/useGroupMutations';
import { useGroupApiProductRoles, useGroupApiRoles, useGroupApplicationRoles } from '../features/groups/hooks/useGroupRoles';
import { buildEventRules, buildRolesMap, hasEventRule, parseMaxInvitation } from '../features/groups/utils/groupPayload';
import { ENVIRONMENT_GROUP_DELETE_PERMISSION, ENVIRONMENT_GROUP_UPDATE_PERMISSION } from '../features/groups/utils/groupPermissions';
import { notify } from '../shared/notify';

function SectionError({ message }: Readonly<{ message: string }>) {
    return (
        <div className="flex items-center gap-2 rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2.5 text-sm text-destructive">
            <TriangleAlertIcon className="size-4 shrink-0" aria-hidden />
            {message}
        </div>
    );
}

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
    const { data: apiRoles = [], isLoading: apiRolesLoading } = useGroupApiRoles();
    const { data: applicationRoles = [], isLoading: applicationRolesLoading } = useGroupApplicationRoles();
    const { data: apiProductRoles = [], isLoading: apiProductRolesLoading } = useGroupApiProductRoles();

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

                <section className="space-y-4 rounded-xl border bg-card p-5">
                    <div>
                        <h2 className="text-base font-semibold">Settings</h2>
                        <p className="text-sm text-muted-foreground">
                            Default roles, member limits, and invitation methods for this group.
                        </p>
                    </div>
                    <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-3">
                        <div>
                            <dt className="text-xs font-medium text-muted-foreground">Default API role</dt>
                            <dd className="flex items-center gap-1.5 text-sm">
                                {group.roles?.API ?? '—'}
                                {group.lock_api_role && <LockIcon className="size-3.5 text-muted-foreground" aria-label="Locked" />}
                            </dd>
                        </div>
                        <div>
                            <dt className="text-xs font-medium text-muted-foreground">Default API product role</dt>
                            <dd className="flex items-center gap-1.5 text-sm">
                                {group.roles?.API_PRODUCT ?? '—'}
                                {group.lock_api_product_role && <LockIcon className="size-3.5 text-muted-foreground" aria-label="Locked" />}
                            </dd>
                        </div>
                        <div>
                            <dt className="text-xs font-medium text-muted-foreground">Default application role</dt>
                            <dd className="flex items-center gap-1.5 text-sm">
                                {group.roles?.APPLICATION ?? '—'}
                                {group.lock_application_role && <LockIcon className="size-3.5 text-muted-foreground" aria-label="Locked" />}
                            </dd>
                        </div>
                        <div>
                            <dt className="text-xs font-medium text-muted-foreground">Max members</dt>
                            <dd className="text-sm">{typeof group.max_invitation === 'number' ? group.max_invitation : 'Unlimited'}</dd>
                        </div>
                        <div className="col-span-2 sm:col-span-2">
                            <dt className="text-xs font-medium text-muted-foreground">Invitation methods</dt>
                            <dd className="flex flex-wrap items-center gap-1.5 text-sm">
                                {group.system_invitation && (
                                    <Badge variant="default" className="gap-1 text-xs font-normal">
                                        <SearchIcon className="size-3" aria-hidden />
                                        User search
                                    </Badge>
                                )}
                                {group.email_invitation && (
                                    <Badge variant="default" className="gap-1 text-xs font-normal">
                                        <MailIcon className="size-3" aria-hidden />
                                        Email invitation
                                    </Badge>
                                )}
                                {!group.system_invitation && !group.email_invitation && <span className="text-muted-foreground">None</span>}
                            </dd>
                        </div>
                    </dl>
                </section>

                <section className="space-y-4 rounded-xl border bg-card p-5">
                    <div>
                        <h2 className="text-base font-semibold">Members</h2>
                        <p className="text-sm text-muted-foreground">Direct members of this group and their scoped roles.</p>
                    </div>
                    {membersError ? (
                        <SectionError message="Failed to load members. Please refresh and try again." />
                    ) : (
                        <GroupMembersTable members={members} loading={membersLoading} />
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
        </>
    );
}
