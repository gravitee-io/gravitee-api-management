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
    CardContent,
    CardHeader,
    CardTitle,
    Checkbox,
    cn,
    DataTablePagination,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyMedia,
    EmptyTitle,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { CheckIcon, PlusIcon, Trash2Icon, UsersIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useId, useMemo, useState } from 'react';

import { AddUserGroupSheet } from './AddUserGroupSheet';
import { ClientSideTableSearchField } from './ClientSideTableSearchField';
import { GroupMembershipRoleSelect } from './GroupMembershipRoleSelect';
import { UserInheritedPermissionsSection } from './UserInheritedPermissionsSection';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import { useClientSideTableState } from '../hooks/useClientSideTableState';
import { useGroupMembershipRoleCatalog, useOrganizationUserGroups } from '../hooks/useOrganizationUser';
import { useAddUserToGroup, useRemoveUserFromGroup, useUpdateUserGroupMembership } from '../hooks/useUserMutations';
import type {
    AddUserGroupMembershipPayload,
    GroupMembershipRoleCatalogScope,
    GroupMembershipRoleScope,
    OrganizationEnvironment,
    OrganizationRole,
    OrganizationUserGroup,
} from '../types/user';
import { formatGroupScopeRole, GROUP_MEMBERSHIP_TABLE_COLUMNS, groupMembershipStatusLabel, isGroupAdmin } from '../utils/userGroupDisplay';
import {
    hasAtLeastOneGroupMembershipRole,
    isApiRolePrimaryOwnerLocked,
    mergeGroupMembershipPayload,
    organizationUserGroupToMembershipPayload,
} from '../utils/userGroupMembership';

const GROUP_MEMBERSHIP_SEARCH_IGNORE_KEYS = ['roles', 'isApiPrimaryOwner', 'environmentId', 'environmentName'] as const;

interface UserGroupMembershipsCardProps {
    readonly userId: string;
    readonly userDisplayName?: string;
    readonly environments: OrganizationEnvironment[];
    readonly environmentsLoading?: boolean;
    readonly rolesEditable?: boolean;
    readonly canAddToGroup?: boolean;
    readonly canRemoveFromGroup?: boolean;
}

function useRoleCatalog(scope: GroupMembershipRoleCatalogScope) {
    const { data = [] } = useGroupMembershipRoleCatalog(scope);
    return data;
}

function GroupAdminCell({ roles }: Readonly<{ roles: OrganizationUserGroup['roles'] }>) {
    if (!isGroupAdmin(roles)) {
        return <span className="text-muted-foreground">—</span>;
    }
    return (
        <span className="inline-flex items-center text-success" aria-label="Group admin">
            <CheckIcon className="size-4" aria-hidden />
        </span>
    );
}

function GroupRoleCell({
    roles,
    scope,
}: Readonly<{ roles: OrganizationUserGroup['roles']; scope: Exclude<GroupMembershipRoleScope, 'GROUP'> }>) {
    const role = formatGroupScopeRole(roles, scope);
    if (!role) {
        return <span className="text-muted-foreground">—</span>;
    }
    return (
        <Badge variant="outline" className="font-normal">
            {role}
        </Badge>
    );
}

function GroupNameCell({ group }: Readonly<{ group: OrganizationUserGroup }>) {
    const statusLabel = groupMembershipStatusLabel(group);

    return (
        <div className="flex flex-wrap items-center gap-2">
            <span>{group.name ?? group.id}</span>
            {statusLabel ? (
                <Badge variant="outline" className="text-xs font-normal">
                    {statusLabel}
                </Badge>
            ) : null}
        </div>
    );
}

function GroupMembershipDeleteCell({
    group,
    saving,
    apiRoleLocked,
    onRemove,
}: Readonly<{
    group: OrganizationUserGroup;
    saving: boolean;
    apiRoleLocked: boolean;
    onRemove: () => void;
}>) {
    const deleteButton = (
        <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-8 text-destructive hover:text-destructive"
            aria-label={`Remove from ${group.name ?? group.id}`}
            disabled={saving || apiRoleLocked}
            onClick={onRemove}
        >
            <Trash2Icon className="size-4" aria-hidden />
        </Button>
    );

    if (apiRoleLocked) {
        return (
            <TableCell className="px-2 py-3 align-middle">
                <TooltipProvider delayDuration={200}>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="inline-flex">{deleteButton}</span>
                        </TooltipTrigger>
                        <TooltipContent>Cannot remove a user who is API Primary Owner in this group</TooltipContent>
                    </Tooltip>
                </TooltipProvider>
            </TableCell>
        );
    }

    return <TableCell className="px-2 py-3 align-middle">{deleteButton}</TableCell>;
}

function GroupMembershipRow({
    group,
    rolesEditable,
    canRemoveFromGroup,
    saving,
    roleCatalogs,
    onRoleChange,
    onRemove,
}: Readonly<{
    group: OrganizationUserGroup;
    rolesEditable: boolean;
    canRemoveFromGroup: boolean;
    saving: boolean;
    roleCatalogs: Record<Exclude<GroupMembershipRoleScope, 'GROUP'>, OrganizationRole[]>;
    onRoleChange: (patch: Partial<Omit<AddUserGroupMembershipPayload, 'groupId'>>) => void;
    onRemove: () => void;
}>) {
    const apiRoleLocked = isApiRolePrimaryOwnerLocked(group);
    const showDelete = canRemoveFromGroup;
    const deleteCell = showDelete ? (
        <GroupMembershipDeleteCell group={group} saving={saving} apiRoleLocked={apiRoleLocked} onRemove={onRemove} />
    ) : null;

    if (!rolesEditable) {
        return (
            <TableRow>
                <TableCell className="px-4 py-3 align-middle font-medium">
                    <GroupNameCell group={group} />
                </TableCell>
                {GROUP_MEMBERSHIP_TABLE_COLUMNS.map(column =>
                    column.scope === 'GROUP' ? (
                        <TableCell key={column.scope} className="px-4 py-3 align-middle">
                            <GroupAdminCell roles={group.roles} />
                        </TableCell>
                    ) : (
                        <TableCell key={column.scope} className="px-4 py-3 align-middle">
                            <GroupRoleCell roles={group.roles} scope={column.scope} />
                        </TableCell>
                    ),
                )}
                {deleteCell}
            </TableRow>
        );
    }

    return (
        <TableRow>
            <TableCell className="px-4 py-3 align-middle font-medium">
                <GroupNameCell group={group} />
            </TableCell>
            <TableCell className="px-4 py-3 align-middle">
                <Checkbox
                    id={`group-admin-${group.id}`}
                    aria-label={`Group admin for ${group.name ?? group.id}`}
                    checked={group.roles?.GROUP === 'ADMIN'}
                    disabled={saving}
                    onCheckedChange={checked => onRoleChange({ isGroupAdmin: checked === true })}
                />
            </TableCell>
            <TableCell className="px-4 py-3 align-middle">
                <GroupMembershipRoleSelect
                    id={`group-api-role-${group.id}`}
                    ariaLabel={`API role for ${group.name ?? group.id}`}
                    value={group.roles?.API}
                    roles={roleCatalogs.API}
                    disabled={saving || apiRoleLocked}
                    onChange={apiRole => onRoleChange({ apiRole })}
                />
            </TableCell>
            <TableCell className="px-4 py-3 align-middle">
                <GroupMembershipRoleSelect
                    id={`group-api-product-role-${group.id}`}
                    ariaLabel={`API Product role for ${group.name ?? group.id}`}
                    value={group.roles?.API_PRODUCT}
                    roles={roleCatalogs.API_PRODUCT}
                    disabled={saving}
                    onChange={apiProductRole => onRoleChange({ apiProductRole })}
                />
            </TableCell>
            <TableCell className="px-4 py-3 align-middle">
                <GroupMembershipRoleSelect
                    id={`group-application-role-${group.id}`}
                    ariaLabel={`Application role for ${group.name ?? group.id}`}
                    value={group.roles?.APPLICATION}
                    roles={roleCatalogs.APPLICATION}
                    disabled={saving}
                    onChange={applicationRole => onRoleChange({ applicationRole })}
                />
            </TableCell>
            <TableCell className="px-4 py-3 align-middle">
                <GroupMembershipRoleSelect
                    id={`group-integration-role-${group.id}`}
                    ariaLabel={`Integration role for ${group.name ?? group.id}`}
                    value={group.roles?.INTEGRATION}
                    roles={roleCatalogs.INTEGRATION}
                    disabled={saving}
                    onChange={integrationRole => onRoleChange({ integrationRole })}
                />
            </TableCell>
            {deleteCell}
        </TableRow>
    );
}

function EnvironmentGroupMembershipsPanel({
    userId,
    userDisplayName,
    environmentId,
    environments,
    rolesEditable,
    canAddToGroup,
    canRemoveFromGroup,
    addGroupOpen,
    onAddGroupOpenChange,
}: Readonly<{
    userId: string;
    userDisplayName?: string;
    environmentId: string;
    environments: OrganizationEnvironment[];
    rolesEditable: boolean;
    canAddToGroup: boolean;
    canRemoveFromGroup: boolean;
    addGroupOpen: boolean;
    onAddGroupOpenChange: (open: boolean) => void;
}>) {
    const searchInputId = useId();
    const [savingGroupId, setSavingGroupId] = useState<string | null>(null);
    const [groupToRemove, setGroupToRemove] = useState<OrganizationUserGroup | null>(null);
    const addUserToGroup = useAddUserToGroup(userId, environmentId);
    const updateUserGroupMembership = useUpdateUserGroupMembership(userId, environmentId);
    const removeUserFromGroup = useRemoveUserFromGroup(userId, environmentId);
    const apiRoles = useRoleCatalog('API');
    const apiProductRoles = useRoleCatalog('API_PRODUCT');
    const applicationRoles = useRoleCatalog('APPLICATION');
    const integrationRoles = useRoleCatalog('INTEGRATION');
    const roleCatalogs = {
        API: apiRoles,
        API_PRODUCT: apiProductRoles,
        APPLICATION: applicationRoles,
        INTEGRATION: integrationRoles,
    };

    const {
        data: groupsResponse,
        isLoading: groupsLoading,
        isFetching: groupsFetching,
        isError: groupsError,
    } = useOrganizationUserGroups(userId, environmentId);
    const groups = useMemo(() => groupsResponse?.data ?? [], [groupsResponse?.data]);
    const loading = groupsLoading || (groupsFetching && groups.length === 0);
    const isSaving = addUserToGroup.isPending || updateUserGroupMembership.isPending || removeUserFromGroup.isPending;
    const showActionsColumn = canRemoveFromGroup;
    const groupsTableColumnCount = GROUP_MEMBERSHIP_TABLE_COLUMNS.length + 1 + (showActionsColumn ? 1 : 0);
    const {
        search,
        page,
        pageSize,
        totalCount,
        paginatedItems: paginatedGroups,
        handleSearchChange,
        handlePageSizeChange,
        setPage,
    } = useClientSideTableState(groups, GROUP_MEMBERSHIP_SEARCH_IGNORE_KEYS);

    function handleAddGroup(payload: AddUserGroupMembershipPayload) {
        addUserToGroup.mutate(payload, {
            onSuccess: () => {
                notify.success('User successfully added to group.');
                onAddGroupOpenChange(false);
            },
            onError: error => notify.error(error, 'Failed to add user to group'),
        });
    }

    function handleRoleChange(group: OrganizationUserGroup, patch: Partial<Omit<AddUserGroupMembershipPayload, 'groupId'>>) {
        const payload = mergeGroupMembershipPayload(organizationUserGroupToMembershipPayload(group), patch);
        if (!hasAtLeastOneGroupMembershipRole(payload)) {
            notify.error('At least one role is mandatory.');
            return;
        }

        setSavingGroupId(group.id);
        updateUserGroupMembership.mutate(payload, {
            onSuccess: () => {
                notify.success('Roles successfully updated');
                setSavingGroupId(null);
            },
            onError: error => {
                notify.error(error, 'Failed to update group roles');
                setSavingGroupId(null);
            },
        });
    }

    function handleConfirmRemove() {
        if (!groupToRemove) {
            return;
        }
        const group = groupToRemove;
        removeUserFromGroup.mutate(group.id, {
            onSuccess: () => {
                const displayName = userDisplayName ?? 'User';
                notify.success(`"${displayName}" has been deleted from the group "${group.name ?? group.id}"`);
                setGroupToRemove(null);
            },
            onError: error => {
                notify.error(error, 'Failed to remove user from group');
            },
        });
    }

    return (
        <>
            {groupsError ? (
                <p className="text-sm text-destructive" role="alert">
                    Failed to load group memberships. Please refresh and try again.
                </p>
            ) : loading ? (
                <div className="space-y-2">
                    {Array.from({ length: 2 }).map((_, index) => (
                        <Skeleton key={index} className="h-10 rounded-lg" />
                    ))}
                </div>
            ) : groups.length === 0 ? (
                <Empty className="border border-dashed rounded-lg py-10">
                    <EmptyHeader>
                        <EmptyMedia variant="icon">
                            <UsersIcon className="size-5" aria-hidden />
                        </EmptyMedia>
                        <EmptyTitle>No group</EmptyTitle>
                        <EmptyDescription>This user is not a member of any group in this environment.</EmptyDescription>
                    </EmptyHeader>
                </Empty>
            ) : (
                <section className="space-y-3" aria-label="Group memberships table">
                    <DataTablePagination
                        page={page}
                        pageSize={pageSize}
                        totalCount={totalCount}
                        pageSizeOptions={[...TABLE_PAGE_SIZE_OPTIONS]}
                        onPageChange={setPage}
                        onPageSizeChange={handlePageSizeChange}
                    >
                        <ClientSideTableSearchField id={searchInputId} label="Search groups" value={search} onChange={handleSearchChange} />
                    </DataTablePagination>
                    <div className="rounded-lg border">
                        <Table aria-label="Groups table">
                            <TableHeader>
                                <TableRow className="bg-muted/30">
                                    <TableHead scope="col" className="px-4 text-muted-foreground">
                                        Group
                                    </TableHead>
                                    {GROUP_MEMBERSHIP_TABLE_COLUMNS.map(column => (
                                        <TableHead key={column.scope} scope="col" className="px-4 text-muted-foreground">
                                            {column.label}
                                        </TableHead>
                                    ))}
                                    {showActionsColumn ? (
                                        <TableHead scope="col" className="w-12 px-2">
                                            <span className="sr-only">Actions</span>
                                        </TableHead>
                                    ) : null}
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {paginatedGroups.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={groupsTableColumnCount} className="px-4 py-6 text-center text-muted-foreground">
                                            No groups match your search.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    paginatedGroups.map(group => (
                                        <GroupMembershipRow
                                            key={group.id}
                                            group={group}
                                            rolesEditable={rolesEditable}
                                            canRemoveFromGroup={canRemoveFromGroup}
                                            saving={
                                                (isSaving && savingGroupId === group.id) ||
                                                (removeUserFromGroup.isPending && groupToRemove?.id === group.id)
                                            }
                                            roleCatalogs={roleCatalogs}
                                            onRoleChange={patch => handleRoleChange(group, patch)}
                                            onRemove={() => setGroupToRemove(group)}
                                        />
                                    ))
                                )}
                            </TableBody>
                        </Table>
                    </div>
                </section>
            )}

            <UserInheritedPermissionsSection userId={userId} environmentId={environmentId} environments={environments} />

            {canAddToGroup ? (
                <AddUserGroupSheet
                    open={addGroupOpen}
                    environmentId={environmentId}
                    existingGroupIds={groups.map(group => group.id)}
                    onClose={() => onAddGroupOpenChange(false)}
                    onSubmit={handleAddGroup}
                    isPending={addUserToGroup.isPending}
                />
            ) : null}

            {groupToRemove ? (
                <ConfirmDialog
                    open
                    onOpenChange={open => !open && !removeUserFromGroup.isPending && setGroupToRemove(null)}
                    title="Delete user from the group"
                    description={
                        <>
                            Are you sure you want to delete the user from the group{' '}
                            <strong>{groupToRemove.name ?? groupToRemove.id}</strong>?
                        </>
                    }
                    confirmLabel="Delete"
                    pendingLabel="Deleting…"
                    destructive
                    isPending={removeUserFromGroup.isPending}
                    onConfirm={handleConfirmRemove}
                />
            ) : null}
        </>
    );
}

export function UserGroupMembershipsCard({
    userId,
    userDisplayName,
    environments,
    environmentsLoading = false,
    rolesEditable = false,
    canAddToGroup = false,
    canRemoveFromGroup = false,
}: UserGroupMembershipsCardProps) {
    const shellEnvironment = useEnvironment();
    const [selectedEnvironmentId, setSelectedEnvironmentId] = useState<string | undefined>();
    const [addGroupOpen, setAddGroupOpen] = useState(false);

    useEffect(() => {
        if (environments.length === 0) {
            setSelectedEnvironmentId(undefined);
            return;
        }
        setSelectedEnvironmentId(current => {
            if (current && environments.some(environment => environment.id === current)) {
                return current;
            }
            if (environments.some(environment => environment.id === shellEnvironment.id)) {
                return shellEnvironment.id;
            }
            return environments[0]?.id;
        });
    }, [environments, shellEnvironment.id]);

    const activeEnvironmentId = selectedEnvironmentId ?? environments[0]?.id;

    useEffect(() => {
        setAddGroupOpen(false);
    }, [activeEnvironmentId, userId]);

    return (
        <Card>
            <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0 pb-3">
                <CardTitle className="text-base">Group Memberships</CardTitle>
                {canAddToGroup && activeEnvironmentId ? (
                    <Button type="button" size="sm" variant="outline" className="shrink-0" onClick={() => setAddGroupOpen(true)}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add to Group
                    </Button>
                ) : null}
            </CardHeader>
            <CardContent>
                {environmentsLoading ? (
                    <div className="space-y-2">
                        {Array.from({ length: 2 }).map((_, index) => (
                            <Skeleton key={index} className="h-10 rounded-lg" />
                        ))}
                    </div>
                ) : environments.length === 0 ? (
                    <Empty className="border-none p-0">
                        <EmptyHeader>
                            <UsersIcon className="size-8 text-muted-foreground" aria-hidden />
                            <EmptyTitle>No environments available</EmptyTitle>
                            <EmptyDescription>Group memberships are scoped to an environment.</EmptyDescription>
                        </EmptyHeader>
                    </Empty>
                ) : (
                    <>
                        {environments.length > 1 ? (
                            <div role="tablist" aria-label="Environment group memberships" className="mb-4 flex gap-1 border-b">
                                {environments.map(environment => {
                                    const isSelected = environment.id === activeEnvironmentId;
                                    return (
                                        <button
                                            key={environment.id}
                                            role="tab"
                                            type="button"
                                            aria-selected={isSelected}
                                            onClick={() => setSelectedEnvironmentId(environment.id)}
                                            className={cn(
                                                '-mb-px border-b-2 px-4 py-2 text-sm font-medium transition-colors',
                                                isSelected
                                                    ? 'border-primary text-foreground'
                                                    : 'border-transparent text-muted-foreground hover:border-muted-foreground hover:text-foreground',
                                            )}
                                        >
                                            {environment.name ?? environment.id}
                                        </button>
                                    );
                                })}
                            </div>
                        ) : null}

                        {activeEnvironmentId ? (
                            <EnvironmentGroupMembershipsPanel
                                key={`${userId}:${activeEnvironmentId}`}
                                userId={userId}
                                userDisplayName={userDisplayName}
                                environmentId={activeEnvironmentId}
                                environments={environments}
                                rolesEditable={rolesEditable}
                                canAddToGroup={canAddToGroup}
                                canRemoveFromGroup={canRemoveFromGroup}
                                addGroupOpen={addGroupOpen}
                                onAddGroupOpenChange={setAddGroupOpen}
                            />
                        ) : null}
                    </>
                )}
            </CardContent>
        </Card>
    );
}
