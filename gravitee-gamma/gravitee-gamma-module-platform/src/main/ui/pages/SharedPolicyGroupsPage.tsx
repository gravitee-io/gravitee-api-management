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
import { Button, DataTableEmptyState } from '@gravitee/graphene-core';
import { LayersIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState, type SetStateAction } from 'react';
import { useNavigate } from 'react-router-dom';

import { sortToOrder, type TableSortingState } from '../features/applications/utils/tableSort';
import {
    SharedPolicyGroupCreateSheet,
    type SharedPolicyGroupCreateFormValues,
} from '../features/shared-policy-groups/components/SharedPolicyGroupCreateSheet';
import {
    SharedPolicyGroupEditSheet,
    type SharedPolicyGroupEditFormValues,
} from '../features/shared-policy-groups/components/SharedPolicyGroupEditSheet';
import { SharedPolicyGroupRemoveDialog } from '../features/shared-policy-groups/components/SharedPolicyGroupRemoveDialog';
import { SharedPolicyGroupsEmptyState } from '../features/shared-policy-groups/components/SharedPolicyGroupsEmptyState';
import { SharedPolicyGroupsTable } from '../features/shared-policy-groups/components/SharedPolicyGroupsTable';
import {
    useCreateSharedPolicyGroup,
    useDeleteSharedPolicyGroup,
    useDeploySharedPolicyGroup,
    useUndeploySharedPolicyGroup,
    useUpdateSharedPolicyGroup,
} from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import { useSharedPolicyGroupsPaged } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import { getSharedPolicyGroup } from '../features/shared-policy-groups/services/sharedPolicyGroups';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import {
    DEFAULT_SHARED_POLICY_GROUP_LIST_PAGE_SIZE,
    SHARED_POLICY_GROUP_SEARCH_DEBOUNCE_MS,
} from '../features/shared-policy-groups/utils/paginationConstants';
import {
    sharedPolicyGroupDetailHref,
    sharedPolicyGroupHistoryHref,
} from '../features/shared-policy-groups/utils/sharedPolicyGroupDetailNavigation';
import { toUpdateSharedPolicyGroupPayload } from '../features/shared-policy-groups/utils/sharedPolicyGroupPayload';
import {
    ENVIRONMENT_SHARED_POLICY_GROUP_CREATE_PERMISSION,
    ENVIRONMENT_SHARED_POLICY_GROUP_DELETE_PERMISSION,
    ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX,
    ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION,
} from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { useHasEnvironmentPermission } from '../shared/hooks/useEnvironmentPermissions';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { notify } from '../shared/notify';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

type SheetState =
    | { type: 'closed' }
    | { type: 'create' }
    | { type: 'edit'; sharedPolicyGroup: SharedPolicyGroup }
    | { type: 'delete'; sharedPolicyGroup: SharedPolicyGroup };

export function SharedPolicyGroupsPage() {
    const navigate = useNavigate();
    const env = useEnvironment();
    const canCreate = useHasEnvironmentPermission([ENVIRONMENT_SHARED_POLICY_GROUP_CREATE_PERMISSION]);
    const canEdit = useHasEnvironmentPermission([ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION]);
    const canDelete = useHasEnvironmentPermission([ENVIRONMENT_SHARED_POLICY_GROUP_DELETE_PERMISSION]);

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_SHARED_POLICY_GROUP_LIST_PAGE_SIZE);
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [sheet, setSheet] = useState<SheetState>({ type: 'closed' });

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), SHARED_POLICY_GROUP_SEARCH_DEBOUNCE_MS);
        return () => clearTimeout(timer);
    }, [search]);

    const sortBy = sortToOrder(sorting);

    function handleSortingChange(updater: SetStateAction<TableSortingState>) {
        setSorting(updater);
        setPage(1);
    }

    const { data, isLoading, isError, error } = useSharedPolicyGroupsPaged({
        query: debouncedSearch,
        page,
        perPage: pageSize,
        sortBy,
    });

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        navItemKey: 'shared-policy-groups',
        permissionPrefix: ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX,
        redirectTo: '../applications',
    });

    const createMutation = useCreateSharedPolicyGroup();
    const updateMutation = useUpdateSharedPolicyGroup();
    const deployMutation = useDeploySharedPolicyGroup();
    const undeployMutation = useUndeploySharedPolicyGroup();
    const deleteMutation = useDeleteSharedPolicyGroup();

    const sharedPolicyGroups = data?.data ?? [];
    const totalCount = data?.pagination.totalCount ?? 0;
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

    function handleOpenEdit(sharedPolicyGroup: SharedPolicyGroup) {
        setSheet({ type: 'edit', sharedPolicyGroup });
    }

    async function handleCreate(values: SharedPolicyGroupCreateFormValues) {
        if (sheet.type !== 'create') return;
        try {
            const created = await createMutation.mutateAsync({
                name: values.name,
                description: values.description || undefined,
                prerequisiteMessage: values.prerequisiteMessage || undefined,
                apiType: values.apiType,
                phase: values.phase,
            });
            notify.success('Shared Policy Group created');
            closeSheet();
            navigate(sharedPolicyGroupDetailHref(created.id));
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group creation!');
        }
    }

    async function handleEdit(values: SharedPolicyGroupEditFormValues) {
        if (sheet.type !== 'edit') return;
        try {
            // The list row only carries summary fields, so `sheet.sharedPolicyGroup.steps` here
            // is `[]`, not `null`. On the Java side a `null` `steps` field would leave the
            // existing steps untouched, but an empty array is a real value and replaces them —
            // so we can't just omit the field. Fetch the current detail and send its real steps
            // back instead.
            if (!env?.id) {
                throw new Error('No active environment');
            }
            const current = await getSharedPolicyGroup(env.id, sheet.sharedPolicyGroup.id);
            await updateMutation.mutateAsync({
                id: sheet.sharedPolicyGroup.id,
                payload: { ...toUpdateSharedPolicyGroupPayload(values), steps: current.steps },
            });
            notify.success('Shared Policy Group updated');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group update!');
        }
    }

    async function handleDelete() {
        if (sheet.type !== 'delete') return;
        try {
            await deleteMutation.mutateAsync(sheet.sharedPolicyGroup.id);
            notify.success('Shared Policy Group removed');
            closeSheet();
        } catch (error) {
            notify.error(error, 'An error occurred while removing the Shared Policy Group');
        }
    }

    async function handleDeploy(sharedPolicyGroup: SharedPolicyGroup) {
        try {
            await deployMutation.mutateAsync(sharedPolicyGroup.id);
            notify.success('Shared Policy Group deployed successfully');
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group deployment!');
        }
    }

    async function handleUndeploy(sharedPolicyGroup: SharedPolicyGroup) {
        try {
            await undeployMutation.mutateAsync(sharedPolicyGroup.id);
            notify.success('Shared Policy Group undeployed successfully');
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group undeployment!');
        }
    }

    if (isForbidden) {
        return null;
    }

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load Shared Policy Groups. Please refresh and try again.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Shared Policy Groups</h1>
                    <p className="text-sm text-muted-foreground">
                        Shared Policy Groups let you apply reusable policies to your API event phases.
                    </p>
                </div>
                {canCreate && !isLoading ? (
                    <Button className="shrink-0" size="sm" onClick={openCreateSheet}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add Shared Policy Group
                    </Button>
                ) : null}
            </div>

            {isFirstUse ? (
                <div className="rounded-lg border">
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<LayersIcon className="size-8" aria-hidden />}
                        title="No Shared Policy Groups"
                        description="Shared Policy Groups let you create reusable policy bundles and apply them across multiple API flows."
                    >
                        <SharedPolicyGroupsEmptyState />
                    </DataTableEmptyState>
                </div>
            ) : (
                <SharedPolicyGroupsTable
                    sharedPolicyGroups={sharedPolicyGroups}
                    totalCount={totalCount}
                    loading={isLoading}
                    search={search}
                    page={page}
                    pageSize={pageSize}
                    sorting={sorting}
                    canEdit={canEdit}
                    canDelete={canDelete}
                    onSearchChange={handleSearchChange}
                    onPageChange={setPage}
                    onPageSizeChange={setPageSize}
                    onSortingChange={handleSortingChange}
                    onDeploy={sharedPolicyGroup => void handleDeploy(sharedPolicyGroup)}
                    onUndeploy={sharedPolicyGroup => void handleUndeploy(sharedPolicyGroup)}
                    onHistory={sharedPolicyGroup => navigate(sharedPolicyGroupHistoryHref(sharedPolicyGroup.id))}
                    onEdit={handleOpenEdit}
                    onDelete={sharedPolicyGroup => setSheet({ type: 'delete', sharedPolicyGroup })}
                />
            )}

            {sheet.type === 'create' ? <SharedPolicyGroupCreateSheet open onClose={closeSheet} onSubmit={handleCreate} /> : null}

            {sheet.type === 'edit' ? (
                <SharedPolicyGroupEditSheet
                    key={sheet.sharedPolicyGroup.id}
                    open
                    sharedPolicyGroup={sheet.sharedPolicyGroup}
                    onClose={closeSheet}
                    onSubmit={handleEdit}
                />
            ) : null}

            <SharedPolicyGroupRemoveDialog
                open={sheet.type === 'delete'}
                onOpenChange={isOpen => !isOpen && closeSheet()}
                isPending={deleteMutation.isPending}
                onConfirm={handleDelete}
            />
        </div>
    );
}
