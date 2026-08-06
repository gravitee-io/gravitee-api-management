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

import { AddUserSheet } from '../features/users/components/AddUserSheet';
import { UsersTable } from '../features/users/components/UsersTable';
import { useOrganizationUsers } from '../features/users/hooks/useOrganizationUsers';
import { useCreateOrganizationUser, useDeleteOrganizationUser } from '../features/users/hooks/useUserMutations';
import type { NewPreRegisterUserPayload, OrganizationUser } from '../features/users/types/user';
import { DEFAULT_USER_LIST_PAGE_SIZE, USER_SEARCH_DEBOUNCE_MS } from '../features/users/utils/paginationConstants';
import { formatUserDisplayName } from '../features/users/utils/userDetailDisplay';
import { isDuplicateUserError, isStillPrimaryOwnerError } from '../features/users/utils/userDisplay';
import { ORGANIZATION_USER_CREATE_PERMISSION, ORGANIZATION_USER_DELETE_PERMISSION } from '../features/users/utils/userPermissions';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { notify } from '../shared/notify';

export function UsersPage() {
    const canCreate = useHasPermission({ anyOf: [ORGANIZATION_USER_CREATE_PERMISSION] });
    const canDelete = useHasPermission({ anyOf: [ORGANIZATION_USER_DELETE_PERMISSION] });

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_USER_LIST_PAGE_SIZE);
    const [sheetOpen, setSheetOpen] = useState(false);
    const [submitEmailError, setSubmitEmailError] = useState<string | null>(null);
    const [userToDelete, setUserToDelete] = useState<OrganizationUser | null>(null);

    const createMutation = useCreateOrganizationUser();
    const deleteMutation = useDeleteOrganizationUser();

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), USER_SEARCH_DEBOUNCE_MS);
        return () => clearTimeout(timer);
    }, [search]);

    const { data, isLoading, isError } = useOrganizationUsers({
        query: debouncedSearch,
        page,
        size: pageSize,
    });

    const users = data?.data ?? [];
    const totalCount = data?.page.total_elements ?? 0;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));

    useEffect(() => {
        if (page > totalPages) {
            setPage(totalPages);
        }
    }, [page, totalPages]);
    // `keepPreviousData` keeps the previous page mounted across refetches, so only the very first
    // load shows skeletons — reacting to `isFetching` here would flash them on every page change.
    const isFirstUse = !isLoading && totalCount === 0 && !search.trim() && !debouncedSearch.trim();

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function openCreateSheet() {
        setSubmitEmailError(null);
        setSheetOpen(true);
    }

    function handleCreate(payload: NewPreRegisterUserPayload) {
        setSubmitEmailError(null);
        createMutation.mutate(payload, {
            onSuccess: () => {
                notify.success('New user successfully registered!');
                setSheetOpen(false);
            },
            onError: error => {
                const message = error instanceof Error ? error.message : '';
                if (isDuplicateUserError(message)) {
                    setSubmitEmailError(message);
                    return;
                }
                notify.error(error, 'Failed to register user.');
            },
        });
    }

    function handleDeleteConfirm() {
        if (!userToDelete) return;
        const displayName = formatUserDisplayName(userToDelete);
        deleteMutation.mutate(userToDelete.id, {
            onSuccess: () => {
                setUserToDelete(null);
                notify.success(`User ${displayName} is being deleted!`);
            },
            onError: error => {
                const message = error instanceof Error ? error.message : '';
                if (isStillPrimaryOwnerError(message)) {
                    notify.error(error, message);
                    return;
                }
                notify.error(error, 'Failed to delete user.');
            },
        });
    }

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load users. Please refresh and try again.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Users</h1>
                    <p className="text-sm text-muted-foreground">Manage users and service accounts across your organization.</p>
                </div>
                {canCreate && !isFirstUse ? (
                    <Button size="sm" className="shrink-0" onClick={openCreateSheet}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add User
                    </Button>
                ) : null}
            </div>

            <UsersTable
                users={users}
                totalCount={totalCount}
                loading={isLoading}
                isFirstUse={isFirstUse}
                search={search}
                page={page}
                pageSize={pageSize}
                onSearchChange={handleSearchChange}
                onPageChange={setPage}
                onPageSizeChange={setPageSize}
                onAddUser={canCreate ? openCreateSheet : undefined}
                canDelete={canDelete}
                onDeleteUser={user => setUserToDelete(user)}
            />

            {userToDelete ? (
                <ConfirmDialog
                    open
                    onOpenChange={open => !open && !deleteMutation.isPending && setUserToDelete(null)}
                    title="Delete a user"
                    description={
                        <>
                            Are you sure you want to delete the user <strong>{formatUserDisplayName(userToDelete)}</strong>?
                        </>
                    }
                    confirmLabel="Delete"
                    pendingLabel="Deleting…"
                    destructive
                    isPending={deleteMutation.isPending}
                    onConfirm={handleDeleteConfirm}
                />
            ) : null}

            <AddUserSheet
                open={sheetOpen}
                onClose={() => setSheetOpen(false)}
                onSubmit={handleCreate}
                isPending={createMutation.isPending}
                serverEmailError={submitEmailError}
            />
        </div>
    );
}
