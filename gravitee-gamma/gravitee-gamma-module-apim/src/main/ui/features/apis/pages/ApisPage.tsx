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
import { Alert, AlertDescription, type DataTableProps } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { ApisEmptyLanding } from '../components';
import { ApisPageSkeleton } from '../components/ApisPageSkeleton';
import { ApisListView } from '../components/list';
import { toApiListSortBy } from '../components/list/ApiListTable';
import { useApiList } from '../hooks/useApiList';
import { isForbiddenError } from '../utils/apiRequestError';

type SortingState = NonNullable<DataTableProps<unknown>['sorting']>;

const DEFAULT_PAGE = 1;
const DEFAULT_PER_PAGE = 10;

export function ApisPage() {
    const navigate = useNavigate();
    const canCreate = useHasPermission({ anyOf: ['environment-api-c'] });

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [page, setPage] = useState(DEFAULT_PAGE);
    const [perPage, setPerPage] = useState(DEFAULT_PER_PAGE);
    const [sorting, setSorting] = useState<SortingState>([]);

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), 200);
        return () => clearTimeout(timer);
    }, [search]);

    const sortBy = toApiListSortBy(sorting);
    const { data, isLoading, isPlaceholderData, isError, error } = useApiList({ query: debouncedSearch, page, perPage, sortBy });
    const isForbidden = isError && isForbiddenError(error);

    useEffect(() => {
        if (isError && error) {
            if (isForbiddenError(error)) {
                console.warn('User lacks permission to list API proxies', error);
            } else {
                console.error('Failed to load API proxies', error);
            }
        }
    }, [isError, error]);

    const apis = data?.data ?? [];
    const totalCount = data?.pagination?.totalCount ?? 0;
    const hasLoadFailure = isError && !isForbidden;

    const handleSearchChange = (value: string) => {
        setSearch(value);
        setPage(DEFAULT_PAGE);
    };

    const handlePerPageChange = (nextPerPage: number) => {
        setPerPage(nextPerPage);
        setPage(DEFAULT_PAGE);
    };

    const handleSortingChange = (updater: SortingState | ((prev: SortingState) => SortingState)) => {
        setSorting(prev => (typeof updater === 'function' ? updater(prev) : updater));
        setPage(DEFAULT_PAGE);
    };

    const handleCreateProxy = () => navigate('new');

    if (isLoading) {
        return <ApisPageSkeleton />;
    }

    const hasNoApis = !isError && !isPlaceholderData && !search && !debouncedSearch && totalCount === 0;
    if (hasNoApis) {
        return <ApisEmptyLanding onCreateProxy={handleCreateProxy} canCreate={canCreate} />;
    }

    return (
        <div className="space-y-6">
            {hasLoadFailure && (
                <Alert variant="destructive">
                    <AlertDescription>Failed to load API proxies. Change your search, sorting or page, or try again.</AlertDescription>
                </Alert>
            )}
            <ApisListView
                apis={apis}
                totalCount={totalCount}
                isLoading={isLoading}
                search={search}
                debouncedSearch={debouncedSearch}
                page={page}
                perPage={perPage}
                sorting={sorting}
                onSortingChange={handleSortingChange}
                onSearchChange={handleSearchChange}
                onPageChange={setPage}
                onPerPageChange={handlePerPageChange}
                onCreateProxy={handleCreateProxy}
                canCreate={canCreate}
                loadFailed={hasLoadFailure}
                forbidden={isForbidden}
            />
        </div>
    );
}
