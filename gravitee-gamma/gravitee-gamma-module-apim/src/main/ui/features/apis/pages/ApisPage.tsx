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
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { useFederationEnabled } from '../../license/useFederationEnabled';
import { ApisEmptyLanding } from '../components';
import { ApisPageSkeleton } from '../components/ApisPageSkeleton';
import { ApisListView } from '../components/list';
import { apiListFilterSelectionsEqual, hasActiveApiListFilters, type ApiListFilterSelection } from '../components/list/apiListFilters';
import { sortingFromApiListOrder, toApiListSortBy } from '../components/list/ApiListTable';
import { useApiList } from '../hooks/useApiList';
import {
    API_LIST_DEFAULT_PAGE,
    buildApiListSearchParams,
    parseApiListSearchParams,
    urlSearchParamsEqual,
    type ApiListUrlState,
} from '../utils/apiListSearchParams';
import { isForbiddenError } from '../utils/apiRequestError';

type SortingState = NonNullable<DataTableProps<unknown>['sorting']>;

function applyUrlStateToListControls(
    fromUrl: ApiListUrlState,
    setters: {
        setSearch: (value: string) => void;
        setDebouncedSearch: (value: string) => void;
        setPage: (page: number) => void;
        setPerPage: (perPage: number) => void;
        setSorting: (sorting: SortingState) => void;
        setFilters: (filters: ApiListFilterSelection) => void;
    },
) {
    setters.setSearch(fromUrl.query);
    setters.setDebouncedSearch(fromUrl.query);
    setters.setPage(fromUrl.page);
    setters.setPerPage(fromUrl.perPage);
    setters.setSorting(sortingFromApiListOrder(fromUrl.order));
    setters.setFilters({ ...fromUrl.filters });
}

export function ApisPage() {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const canCreate = useHasPermission({ anyOf: ['environment-api-c'] });
    const { enabled: includeFederated, isResolved: isFederationResolved } = useFederationEnabled();

    const initialUrl = parseApiListSearchParams(searchParams, { sanitizeApiTypes: false });
    const [search, setSearch] = useState(initialUrl.query);
    const [debouncedSearch, setDebouncedSearch] = useState(initialUrl.query);
    const [page, setPage] = useState(initialUrl.page);
    const [perPage, setPerPage] = useState(initialUrl.perPage);
    const [sorting, setSorting] = useState<SortingState>(() => sortingFromApiListOrder(initialUrl.order));
    const [filters, setFilters] = useState<ApiListFilterSelection>(initialUrl.filters);

    const navigatingFromUrlRef = useRef(false);
    const lastSerializedUrlRef = useRef(searchParams.toString());

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), 200);
        return () => clearTimeout(timer);
    }, [search]);

    const explicitSortBy = toApiListSortBy(sorting);
    const sortBy = explicitSortBy ?? (debouncedSearch ? undefined : 'name');

    const listUrlState = useMemo(
        (): ApiListUrlState => ({
            query: debouncedSearch,
            page,
            perPage,
            order: sortBy,
            filters,
        }),
        [debouncedSearch, filters, page, perPage, sortBy],
    );

    // Pull URL → state only when the location changes (back/forward, shared link). Writes pre-update
    // `lastSerializedUrlRef` so this effect skips our own `setSearchParams` updates.
    useEffect(() => {
        const serialized = searchParams.toString();
        if (serialized === lastSerializedUrlRef.current) {
            return;
        }

        const fromUrl = parseApiListSearchParams(searchParams, { includeFederated });
        navigatingFromUrlRef.current = true;
        lastSerializedUrlRef.current = serialized;
        applyUrlStateToListControls(fromUrl, {
            setSearch,
            setDebouncedSearch,
            setPage,
            setPerPage,
            setSorting,
            setFilters,
        });
    }, [includeFederated, searchParams]);

    useEffect(() => {
        if (!isFederationResolved) {
            return;
        }
        const fromUrl = parseApiListSearchParams(searchParams, { includeFederated });
        setFilters(prev => (apiListFilterSelectionsEqual(prev, fromUrl.filters) ? prev : fromUrl.filters));
    }, [includeFederated, isFederationResolved, searchParams]);

    useEffect(() => {
        if (navigatingFromUrlRef.current) {
            navigatingFromUrlRef.current = false;
            return;
        }
        const built = buildApiListSearchParams(listUrlState);
        const builtSerialized = built.toString();
        if (urlSearchParamsEqual(searchParams, built, { includeFederated })) {
            lastSerializedUrlRef.current = builtSerialized;
            return;
        }
        lastSerializedUrlRef.current = builtSerialized;
        setSearchParams(built, { replace: true });
    }, [includeFederated, listUrlState, searchParams, setSearchParams]);

    const { data, isLoading, isPlaceholderData, isError, error } = useApiList({
        query: debouncedSearch,
        page,
        perPage,
        sortBy: explicitSortBy,
        filters,
        includeFederated,
        isFederationResolved,
    });
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
        setPage(API_LIST_DEFAULT_PAGE);
    };

    const handlePerPageChange = (nextPerPage: number) => {
        setPerPage(nextPerPage);
        setPage(API_LIST_DEFAULT_PAGE);
    };

    const handleFiltersChange = (next: ApiListFilterSelection) => {
        setFilters(next);
        setPage(API_LIST_DEFAULT_PAGE);
    };

    const handleSortingChange = (updater: SortingState | ((prev: SortingState) => SortingState)) => {
        setSorting(prev => (typeof updater === 'function' ? updater(prev) : updater));
        setPage(API_LIST_DEFAULT_PAGE);
    };

    const handleCreateProxy = () => navigate('new');

    if (isLoading) {
        return <ApisPageSkeleton />;
    }

    const hasNoApis =
        !isError && !isPlaceholderData && !search && !debouncedSearch && !hasActiveApiListFilters(filters) && totalCount === 0;
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
                filters={filters}
                onFiltersChange={handleFiltersChange}
                includeFederated={includeFederated}
            />
        </div>
    );
}
