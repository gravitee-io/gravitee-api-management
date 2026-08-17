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
import { Button } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState, type Dispatch, type SetStateAction } from 'react';
import { useNavigate } from 'react-router-dom';

import { sortToOrder, type TableSortingState } from '../features/applications/utils/tableSort';
import {
    SharedPolicyGroupCreateSheet,
    type SharedPolicyGroupCreateFormValues,
} from '../features/shared-policy-groups/components/SharedPolicyGroupCreateSheet';
import { SharedPolicyGroupDeleteSheet } from '../features/shared-policy-groups/components/SharedPolicyGroupDeleteSheet';
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
import { getSharedPolicyGroup } from '../features/shared-policy-groups/services/sharedPolicyGroups';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import {
    DEFAULT_SHARED_POLICY_GROUP_LIST_PAGE_SIZE,
    SHARED_POLICY_GROUP_SEARCH_DEBOUNCE_MS,
} from '../features/shared-policy-groups/utils/paginationConstants';
import { sharedPolicyGroupKeys } from '../features/shared-policy-groups/utils/queryKeys';
import { sharedPolicyGroupDetailHref } from '../features/shared-policy-groups/utils/sharedPolicyGroupDetailNavigation';
import {
    ENVIRONMENT_SHARED_POLICY_GROUP_CREATE_PERMISSION,
    ENVIRONMENT_SHARED_POLICY_GROUP_DELETE_PERMISSION,
    ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION,
} from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { toUpdateSharedPolicyGroupPayload } from '../features/shared-policy-groups/utils/sharedPolicyGroupPayload';
import { notify } from '../shared/notify';

type SheetState =
    | { type: 'closed' }
    | { type: 'create' }
    | { type: 'edit'; sharedPolicyGroup: SharedPolicyGroup }
    | { type: 'delete'; sharedPolicyGroup: SharedPolicyGroup };

export function SharedPolicyGroupsPage() {
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();
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

    const sortBy = useMemo(() => sortToOrder(sorting), [sorting]);

    const handleSortingChange = useCallback<Dispatch<SetStateAction<TableSortingState>>>(updater => {
        setSorting(updater);
        setPage(1);
    }, []);

    const { data, isLoading, isError } = useSharedPolicyGroupsPaged({ query: debouncedSearch, page, perPage: pageSize, sortBy });

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

    async function handleOpenEdit(sharedPolicyGroup: SharedPolicyGroup) {
        if (!env?.id) {
            notify.error(new Error('No active environment'), 'Error during Shared Policy Group update!');
            return;
        }
        try {
            // List rows may omit steps — fetch via React Query so detail cache stays warm and the table is not re-skeletoned.
            const full = await queryClient.fetchQuery({
                queryKey: sharedPolicyGroupKeys.detail(env.id, sharedPolicyGroup.id),
                queryFn: () => getSharedPolicyGroup(env.id, sharedPolicyGroup.id),
            });
            setSheet({ type: 'edit', sharedPolicyGroup: full });
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group update!');
        }
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
            // Classic Console opens the studio tab after create.
            navigate(sharedPolicyGroupDetailHref(created.id));
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group creation!');
        }
    }

    async function handleEdit(values: SharedPolicyGroupEditFormValues) {
        if (sheet.type !== 'edit') return;
        try {
            await updateMutation.mutateAsync({
                id: sheet.sharedPolicyGroup.id,
                payload: toUpdateSharedPolicyGroupPayload(sheet.sharedPolicyGroup, values),
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
                        Shared Policy Group allow to apply different policies on your API event phases.
                    </p>
                </div>
                {canCreate && !isFirstUse ? (
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
                onView={sharedPolicyGroup => navigate(sharedPolicyGroupDetailHref(sharedPolicyGroup.id))}
                onEdit={handleOpenEdit}
                onDelete={sharedPolicyGroup => setSheet({ type: 'delete', sharedPolicyGroup })}
                onCreateSharedPolicyGroup={canCreate ? () => setSheet({ type: 'create' }) : undefined}
            />

            <SharedPolicyGroupCreateSheet
                open={sheet.type === 'create'}
                onClose={closeSheet}
                onSubmit={handleCreate}
                isSaving={createMutation.isPending}
            />

            <SharedPolicyGroupEditSheet
                open={sheet.type === 'edit'}
                sharedPolicyGroup={sheet.type === 'edit' ? sheet.sharedPolicyGroup : null}
                onClose={closeSheet}
                onSubmit={handleEdit}
                isSaving={updateMutation.isPending}
            />

            <SharedPolicyGroupDeleteSheet
                open={sheet.type === 'delete'}
                onClose={closeSheet}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />
        </div>
    );
}
