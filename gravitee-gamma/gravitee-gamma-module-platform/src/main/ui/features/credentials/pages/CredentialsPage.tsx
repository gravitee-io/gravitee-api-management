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
import { Button, DataTableEmptyState } from '@gravitee/graphene-core';
import { KeyRoundIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { useForbiddenResourceRedirect } from '../../../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../../../shared/utils/apiErrors';
import { CredentialsTable } from '../components/CredentialsTable';
import { DeleteCredentialDialog } from '../components/DeleteCredentialDialog';
import { useCredentialOwnerNames, useCredentialsPaged } from '../hooks/useCredentials';
import type { Credential } from '../types/credential';
import {
    ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION,
    ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION,
    ENVIRONMENT_CREDENTIAL_PERMISSION_PREFIX,
    ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION,
} from '../utils/credentialPermissions';

const DEFAULT_PAGE_SIZE = 25;
const SEARCH_DEBOUNCE_MS = 300;

export function CredentialsPage() {
    const navigate = useNavigate();
    const canCreate = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION]);
    const canEdit = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION]);
    const canDelete = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION]);

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
    const [credentialToDelete, setCredentialToDelete] = useState<Credential | undefined>();

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), SEARCH_DEBOUNCE_MS);
        return () => clearTimeout(timer);
    }, [search]);

    const { data, isLoading, isError, error } = useCredentialsPaged({ query: debouncedSearch, page, perPage: pageSize });
    const credentials = data?.data ?? [];
    const ownerNames = useCredentialOwnerNames(credentials.map(credential => credential.ownerId));

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        navItemKey: 'credentials',
        permissionPrefix: ENVIRONMENT_CREDENTIAL_PERMISSION_PREFIX,
        redirectTo: '../applications',
    });

    const totalCount = data?.pagination.totalCount ?? 0;
    const isFirstUse = !isLoading && totalCount === 0 && !search.trim() && !debouncedSearch.trim();

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    if (isForbidden) {
        return null;
    }

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load the credentials. Please refresh and try again.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Credentials</h1>
                    <p className="text-sm text-muted-foreground">Credentials saved for reuse across modules.</p>
                </div>
                {canCreate && !isLoading ? (
                    <Button className="shrink-0" size="sm" onClick={() => navigate('new')}>
                        <PlusIcon className="size-4" aria-hidden />
                        New credential
                    </Button>
                ) : null}
            </div>

            {isFirstUse ? (
                <div className="rounded-lg border">
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<KeyRoundIcon className="size-8" aria-hidden />}
                        title="No credentials yet"
                        description="A credential holds the secrets a module uses to call a service, such as an LLM provider API key. Create it once and reuse it everywhere."
                    />
                </div>
            ) : (
                <CredentialsTable
                    credentials={credentials}
                    ownerNames={ownerNames}
                    totalCount={totalCount}
                    loading={isLoading}
                    search={search}
                    page={page}
                    pageSize={pageSize}
                    canEdit={canEdit}
                    canDelete={canDelete}
                    onSearchChange={handleSearchChange}
                    onPageChange={setPage}
                    onPageSizeChange={setPageSize}
                    onEdit={credential => navigate(`${credential.id}/edit`)}
                    onDelete={setCredentialToDelete}
                />
            )}

            {credentialToDelete ? (
                <DeleteCredentialDialog
                    credential={credentialToDelete}
                    open
                    onOpenChange={open => !open && setCredentialToDelete(undefined)}
                    onDeleted={() => setCredentialToDelete(undefined)}
                />
            ) : null}
        </div>
    );
}
