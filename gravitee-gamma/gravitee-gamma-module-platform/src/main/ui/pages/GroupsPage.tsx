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
import { Button } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { GroupDeleteDialog } from '../features/groups/components/GroupDeleteDialog';
import { GroupSheet, type GroupFormValues } from '../features/groups/components/GroupSheet';
import { GroupsRequireGroupSetting } from '../features/groups/components/GroupsRequireGroupSetting';
import { GroupsTable } from '../features/groups/components/GroupsTable';
import { useCreateGroup, useDeleteGroup, useUpdateGroup } from '../features/groups/hooks/useGroupMutations';
import { useGroupRoles } from '../features/groups/hooks/useGroupRoles';
import { useGroupsPaged } from '../features/groups/hooks/useGroups';
import type { Group, UpdateGroupPayload } from '../features/groups/types/group';
import { buildEventRules, buildRolesMap, parseMaxInvitation } from '../features/groups/utils/groupPayload';
import {
    ENVIRONMENT_GROUP_CREATE_PERMISSION,
    ENVIRONMENT_GROUP_DELETE_PERMISSION,
    ENVIRONMENT_GROUP_UPDATE_PERMISSION,
    ORGANIZATION_SETTINGS_READ_PERMISSION,
} from '../features/groups/utils/groupPermissions';
import { DEFAULT_GROUP_LIST_PAGE_SIZE, GROUP_SEARCH_DEBOUNCE_MS } from '../features/groups/utils/paginationConstants';
import { notify } from '../shared/notify';

type SheetState = { type: 'closed' } | { type: 'create' } | { type: 'edit'; group: Group } | { type: 'delete'; group: Group };

export function GroupsPage() {
    const navigate = useNavigate();
    const canCreate = useHasPermission({ anyOf: [ENVIRONMENT_GROUP_CREATE_PERMISSION] });
    const canEdit = useHasPermission({ anyOf: [ENVIRONMENT_GROUP_UPDATE_PERMISSION] });
    const canDelete = useHasPermission({ anyOf: [ENVIRONMENT_GROUP_DELETE_PERMISSION] });
    const canReadOrgSettings = useHasPermission({ anyOf: [ORGANIZATION_SETTINGS_READ_PERMISSION] });

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_GROUP_LIST_PAGE_SIZE);
    const [sheet, setSheet] = useState<SheetState>({ type: 'closed' });

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), GROUP_SEARCH_DEBOUNCE_MS);
        return () => clearTimeout(timer);
    }, [search]);

    const { data, isLoading, isError } = useGroupsPaged({ query: debouncedSearch, page, size: pageSize });
    const { apiRoles, apiRolesLoading, applicationRoles, applicationRolesLoading, apiProductRoles, apiProductRolesLoading } =
        useGroupRoles();

    const createMutation = useCreateGroup();
    const updateMutation = useUpdateGroup();
    const deleteMutation = useDeleteGroup();

    const groups = data?.data ?? [];
    const totalCount = data?.page.total_elements ?? 0;
    const isFirstUse = !isLoading && totalCount === 0 && !search.trim() && !debouncedSearch.trim();

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function closeSheet() {
        setSheet({ type: 'closed' });
    }

    function openCreateSheet() {
        setSheet({ type: 'create' });
    }

    async function handleCreate(values: GroupFormValues) {
        let created: Group;
        try {
            created = await createMutation.mutateAsync({
                name: values.name,
                lock_api_role: values.lockApiRole,
                lock_api_product_role: values.lockApiProductRole,
                lock_application_role: values.lockApplicationRole,
                event_rules: buildEventRules({
                    apiCreate: values.defaultGroupForNewApis,
                    applicationCreate: values.defaultGroupForNewApplications,
                    apiProductCreate: values.defaultGroupForNewApiProducts,
                }),
                max_invitation: parseMaxInvitation(values.maxInvitation),
                system_invitation: values.systemInvitation,
                email_invitation: values.emailInvitation,
                disable_membership_notifications: !values.notifyOnMemberAdded,
            });
        } catch (error) {
            notify.error(error, 'Failed to create group');
            return;
        }

        // The group already exists at this point (POST succeeded) — close the sheet and refresh the list
        // regardless of what happens next, so a failure below can't be mistaken for "nothing was created"
        // and lead to a duplicate on retry.
        closeSheet();

        if (values.apiRole || values.applicationRole || values.apiProductRole) {
            try {
                const rolesUpdate: UpdateGroupPayload = {
                    name: created.name,
                    lock_api_role: created.lock_api_role ?? values.lockApiRole,
                    lock_api_product_role: created.lock_api_product_role ?? values.lockApiProductRole,
                    lock_application_role: created.lock_application_role ?? values.lockApplicationRole,
                    event_rules: created.event_rules ?? [],
                    roles: buildRolesMap(created.roles, values.apiRole, values.applicationRole, values.apiProductRole),
                    max_invitation: created.max_invitation ?? parseMaxInvitation(values.maxInvitation),
                    system_invitation: created.system_invitation ?? values.systemInvitation,
                    email_invitation: created.email_invitation ?? values.emailInvitation,
                    disable_membership_notifications: created.disable_membership_notifications ?? !values.notifyOnMemberAdded,
                };
                await updateMutation.mutateAsync({ groupId: created.id, data: rolesUpdate });
                notify.success('Group created successfully');
            } catch {
                notify.warning(`Group "${created.name}" was created, but default roles could not be applied. Edit the group to set them.`);
            }
        } else {
            notify.success('Group created successfully');
        }

        navigate(created.id);
    }

    async function handleUpdate(values: GroupFormValues) {
        if (sheet.type !== 'edit') return;
        const group = sheet.group;
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
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to update group');
        }
    }

    async function handleDelete() {
        if (sheet.type !== 'delete') return;
        try {
            await deleteMutation.mutateAsync(sheet.group.id);
            notify.success('Group deleted successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to delete group');
        }
    }

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load groups. Please refresh and try again.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Groups</h1>
                    <p className="text-sm text-muted-foreground">
                        Organize users into groups to share default roles and simplify access management.
                    </p>
                </div>
                {canCreate && !isFirstUse ? (
                    <Button className="shrink-0" size="sm" onClick={openCreateSheet}>
                        <PlusIcon className="size-4" aria-hidden />
                        Create Group
                    </Button>
                ) : null}
            </div>

            {canReadOrgSettings && <GroupsRequireGroupSetting />}

            <GroupsTable
                groups={groups}
                totalCount={totalCount}
                loading={isLoading}
                isFirstUse={isFirstUse}
                search={search}
                page={page}
                pageSize={pageSize}
                canEdit={canEdit}
                canDelete={canDelete}
                onSearchChange={handleSearchChange}
                onPageChange={setPage}
                onPageSizeChange={setPageSize}
                onEdit={group => setSheet({ type: 'edit', group })}
                onDelete={group => setSheet({ type: 'delete', group })}
                onCreateGroup={canCreate ? openCreateSheet : undefined}
            />

            <GroupSheet
                open={sheet.type === 'create' || sheet.type === 'edit'}
                mode={sheet.type === 'edit' ? 'edit' : 'create'}
                group={sheet.type === 'edit' ? sheet.group : undefined}
                apiRoles={apiRoles}
                applicationRoles={applicationRoles}
                apiProductRoles={apiProductRoles}
                rolesLoading={apiRolesLoading || applicationRolesLoading || apiProductRolesLoading}
                onClose={closeSheet}
                onSubmit={sheet.type === 'edit' ? handleUpdate : handleCreate}
                isSaving={createMutation.isPending || updateMutation.isPending}
            />

            <GroupDeleteDialog
                open={sheet.type === 'delete'}
                group={sheet.type === 'delete' ? sheet.group : undefined}
                onClose={closeSheet}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />
        </div>
    );
}
