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
import { Alert, AlertAction, AlertDescription, AlertTitle, Button, DataTableEmptyState, PageHeader } from '@gravitee/graphene-core';
import { CircleXIcon, KeyRoundIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useRef } from 'react';
import { Link } from 'react-router-dom';

import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { useForbiddenResourceRedirect } from '../../../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../../../shared/utils/apiErrors';
import { CredentialDeletedContext } from '../components/CredentialActionsMenu';
import { CredentialsTable } from '../components/CredentialsTable';
import { useCredentialList } from '../hooks/useCredentialList';
import { ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION, ENVIRONMENT_CREDENTIAL_PERMISSION_PREFIX } from '../utils/credentialPermissions';

export function CredentialsPage() {
    const canCreate = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION]);
    const list = useCredentialList();
    const searchRef = useRef<HTMLInputElement>(null);

    const { result, error, settled } = list;
    const isForbidden = isForbiddenApiError(Boolean(error), error);
    useForbiddenResourceRedirect({
        isForbidden,
        navItemKey: 'credentials',
        permissionPrefix: ENVIRONMENT_CREDENTIAL_PERMISSION_PREFIX,
        redirectTo: '../applications',
    });

    const totalCount = result?.pagination.totalCount ?? 0;
    const isFirstUse = settled && totalCount === 0 && !list.hasCriteria;
    const announcement = !settled || isFirstUse ? '' : totalCount === 0 ? 'No results' : `${totalCount} result${totalCount > 1 ? 's' : ''}`;
    const focusSearch = () => searchRef.current?.focus();

    if (isForbidden) {
        return null;
    }

    return (
        <div className="flex flex-col gap-6">
            <PageHeader
                title="Credentials"
                description="Credentials saved for reuse across modules."
                actions={
                    canCreate && (
                        <Button asChild>
                            <Link to="new">
                                <PlusIcon aria-hidden />
                                Add credential
                            </Link>
                        </Button>
                    )
                }
            />

            <p role="status" className="sr-only">
                {announcement}
            </p>

            {error && (
                <Alert variant="destructive">
                    <CircleXIcon aria-hidden />
                    <AlertTitle>Could not load the credentials</AlertTitle>
                    <AlertDescription>{result ? 'The list below may be out of date.' : error.message}</AlertDescription>
                    <AlertAction align="center">
                        <Button variant="outline" size="xs" disabled={list.isFetching} onClick={() => void list.refetch()}>
                            {list.isFetching ? 'Retrying...' : 'Retry'}
                        </Button>
                    </AlertAction>
                </Alert>
            )}

            {isFirstUse ? (
                <div className="rounded-lg border">
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<KeyRoundIcon />}
                        title="No credentials yet"
                        description="A credential holds the secrets a module uses to call a service, such as an LLM provider API key. Create it once and reuse it everywhere."
                    />
                </div>
            ) : (
                (result || !error) && (
                    <CredentialDeletedContext value={focusSearch}>
                        <CredentialsTable
                            credentials={result?.data ?? []}
                            totalCount={totalCount}
                            loading={list.isFetching}
                            searchRef={searchRef}
                            search={list.q}
                            hasCriteria={list.hasCriteria}
                            page={list.page}
                            pageSize={list.size}
                            pageSizeOptions={list.pageSizeOptions}
                            onSearchChange={list.setQuery}
                            onReset={() => {
                                list.reset();
                                focusSearch();
                            }}
                            onPageChange={list.setPage}
                            onPageSizeChange={list.setPageSize}
                        />
                    </CredentialDeletedContext>
                )
            )}
        </div>
    );
}
