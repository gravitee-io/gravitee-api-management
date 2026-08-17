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
import { SharedPolicyGroupsTable } from '../features/shared-policy-groups/components/SharedPolicyGroupsTable';
import {
    useCreateSharedPolicyGroup,
    useDeleteSharedPolicyGroup,
    useUpdateSharedPolicyGroup,
} from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import { useSharedPolicyGroupsPaged } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import {
    DEFAULT_SHARED_POLICY_GROUP_LIST_PAGE_SIZE,
    SHARED_POLICY_GROUP_SEARCH_DEBOUNCE_MS,
} from '../features/shared-policy-groups/utils/paginationConstants';
import { toUpdateSharedPolicyGroupPayload } from '../features/shared-policy-groups/utils/sharedPolicyGroupPayload';
import {
    ENVIRONMENT_SHARED_POLICY_GROUP_CREATE_PERMISSION,
    ENVIRONMENT_SHARED_POLICY_GROUP_DELETE_PERMISSION,
    ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX,
    ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION,
} from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
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
    const canCreate = useHasPermission({ anyOf: [ENVIRONMENT_SHARED_POLICY_GROUP_CREATE_PERMISSION] });
    const canEdit = useHasPermission({ anyOf: [ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION] });
    const canDelete = useHasPermission({ anyOf: [ENVIRONMENT_SHARED_POLICY_GROUP_DELETE_PERMISSION] });

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
        permissionPrefix: ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX,
        redirectTo: '../applications',
    });

    const createMutation = useCreateSharedPolicyGroup();
    const updateMutation = useUpdateSharedPolicyGroup();
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
            navigate(created.id);
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group creation!');
        }
    }

    async function handleEdit(values: SharedPolicyGroupEditFormValues) {
        if (sheet.type !== 'edit') return;
        try {
            await updateMutation.mutateAsync({
                id: sheet.sharedPolicyGroup.id,
                payload: toUpdateSharedPolicyGroupPayload(values),
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
                {canCreate && !isLoading && !isFirstUse ? (
                    <Button className="shrink-0" size="sm" onClick={() => setSheet({ type: 'create' })}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add Shared Policy Group
                    </Button>
                ) : null}
            </div>

            <SharedPolicyGroupsTable
                sharedPolicyGroups={sharedPolicyGroups}
                totalCount={totalCount}
                loading={isLoading}
                isFirstUse={isFirstUse}
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
                onView={sharedPolicyGroup => navigate(sharedPolicyGroup.id)}
                onEdit={handleOpenEdit}
                onDelete={sharedPolicyGroup => setSheet({ type: 'delete', sharedPolicyGroup })}
                onCreateSharedPolicyGroup={canCreate ? () => setSheet({ type: 'create' }) : undefined}
            />

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

            <ConfirmDialog
                open={sheet.type === 'delete'}
                onOpenChange={isOpen => !isOpen && closeSheet()}
                title="Remove Shared Policy Group"
                description={
                    <span className="block space-y-2">
                        <span className="block">Are you sure you want to remove this Shared Policy Group?</span>
                        <span className="block">
                            If this Shared Policy Group is used in API flows, be sure to inform API publishers before making this change.
                        </span>
                        <span className="block">
                            If an API flow still uses this Shared Policy Group, the API flow will ignore it and continue to run.
                        </span>
                    </span>
                }
                confirmLabel="Remove"
                pendingLabel="Removing…"
                destructive
                isPending={deleteMutation.isPending}
                onConfirm={handleDelete}
            />
        </div>
    );
}
