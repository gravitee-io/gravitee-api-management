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
import { ArrowLeftIcon, PencilIcon, Trash2Icon, UsersRoundIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { GroupAssociationSection } from '../features/groups/components/GroupAssociationSection';
import { GroupDeleteSheet } from '../features/groups/components/GroupDeleteSheet';
import { GroupMembersTable } from '../features/groups/components/GroupMembersTable';
import { GroupSettingsSection } from '../features/groups/components/GroupSettingsSection';
import { GroupSheet, type GroupFormValues } from '../features/groups/components/GroupSheet';
import { SectionError } from '../features/groups/components/SectionError';
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
    const { data: apiRoles = [], isLoading: apiRolesLoading } = useGroupApiRoles({ enabled: canEdit && editOpen });
    const { data: applicationRoles = [], isLoading: applicationRolesLoading } = useGroupApplicationRoles({
        enabled: canEdit && editOpen,
    });
    const { data: apiProductRoles = [], isLoading: apiProductRolesLoading } = useGroupApiProductRoles({ enabled: canEdit && editOpen });

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

                <GroupSettingsSection group={group} />

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
        </>
    );
}
