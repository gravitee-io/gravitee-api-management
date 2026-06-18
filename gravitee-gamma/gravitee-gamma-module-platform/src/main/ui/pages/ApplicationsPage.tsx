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
import { useLayoutConfig } from '@gravitee/graphene-core';
import { useCallback, useEffect, useMemo, useState, type Dispatch, type SetStateAction } from 'react';
import { useNavigate } from 'react-router-dom';

import { ApplicationsEmptyLanding } from '../features/applications/components';
import { ApplicationsListView } from '../features/applications/components/list';
import { useApplicationList } from '../features/applications/hooks/useApplicationList';
import { useApplicationStats } from '../features/applications/hooks/useApplicationStats';
import { useOrganizationAdmin } from '../features/applications/hooks/useOrganizationAdmin';
import type { ApplicationStatus } from '../features/applications/types/application';
import {
    APPLICATION_LIST_SERVER_SORT_IDS,
    defaultApplicationListOrder,
    defaultApplicationListSort,
} from '../features/applications/utils/applicationListSort';
import { DEFAULT_APPLICATION_LIST_PAGE_SIZE } from '../features/applications/utils/paginationConstants';
import { sortToOrder, type TableSortingState } from '../features/applications/utils/tableSort';

const DEFAULT_PAGE = 1;
const DEFAULT_PER_PAGE = DEFAULT_APPLICATION_LIST_PAGE_SIZE;
const DEFAULT_STATUS: ApplicationStatus = 'ACTIVE';
const SEARCH_DEBOUNCE_MS = 300;

export function ApplicationsPage() {
    useLayoutConfig({ contentVariant: 'wide' }, []);
    const navigate = useNavigate();
    const canCreate = useHasPermission({ anyOf: ['environment-application-c'] });
    const { isAdmin: canManageArchived, isLoading: isAdminLoading } = useOrganizationAdmin();

    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [status, setStatus] = useState<ApplicationStatus>(DEFAULT_STATUS);
    const [page, setPage] = useState(DEFAULT_PAGE);
    const [perPage, setPerPage] = useState(DEFAULT_PER_PAGE);
    const [sorting, setSorting] = useState<TableSortingState>(() => defaultApplicationListSort(DEFAULT_STATUS));

    const order = useMemo(() => sortToOrder(sorting) ?? defaultApplicationListOrder(status), [sorting, status]);

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), SEARCH_DEBOUNCE_MS);
        return () => clearTimeout(timer);
    }, [search]);

    useEffect(() => {
        if (!isAdminLoading && !canManageArchived && status === 'ARCHIVED') {
            setStatus(DEFAULT_STATUS);
            setPage(DEFAULT_PAGE);
        }
    }, [canManageArchived, isAdminLoading, status]);

    useEffect(() => {
        setSorting(defaultApplicationListSort(status));
        setPage(DEFAULT_PAGE);
    }, [status]);

    const handleSortingChange = useCallback<Dispatch<SetStateAction<TableSortingState>>>(updater => {
        let resetPage = false;
        setSorting(previous => {
            const next = typeof updater === 'function' ? updater(previous) : updater;
            const active = next[0];
            resetPage = Boolean(active?.id && APPLICATION_LIST_SERVER_SORT_IDS.has(active.id));
            return next;
        });
        if (resetPage) {
            setPage(DEFAULT_PAGE);
        }
    }, []);

    const { data, isLoading, isFetching, isError } = useApplicationList({
        query: debouncedSearch,
        status,
        page,
        perPage,
        order,
    });

    const applications = data?.data ?? [];
    const totalCount = data?.page.total_elements ?? 0;

    const knownCounts =
        data?.page.total_elements !== undefined
            ? status === 'ACTIVE'
                ? { active: data.page.total_elements }
                : { archived: data.page.total_elements }
            : undefined;

    const stats = useApplicationStats(debouncedSearch || undefined, { knownCounts });

    const handleSearchChange = (value: string) => {
        setSearch(value);
        setPage(DEFAULT_PAGE);
    };

    const handlePerPageChange = (nextPerPage: number) => {
        setPerPage(nextPerPage);
        setPage(DEFAULT_PAGE);
    };

    const handleStatusChange = (nextStatus: ApplicationStatus) => {
        setStatus(nextStatus);
    };

    const handleRegisterApplication = () => {
        navigate('new');
    };

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load applications. Please refresh and try again.</p>
            </div>
        );
    }

    const hasNoApplications = !isLoading && !isFetching && !search && !debouncedSearch && !stats.isLoading && stats.total === 0;
    if (hasNoApplications) {
        return <ApplicationsEmptyLanding onRegisterApplication={handleRegisterApplication} canCreate={canCreate} />;
    }

    return (
        <ApplicationsListView
            applications={applications}
            totalCount={totalCount}
            stats={stats}
            isLoading={isLoading}
            search={search}
            status={status}
            page={page}
            perPage={perPage}
            onSearchChange={handleSearchChange}
            onStatusChange={handleStatusChange}
            sorting={sorting}
            onSortingChange={handleSortingChange}
            onPageChange={setPage}
            onPerPageChange={handlePerPageChange}
            onRegisterApplication={handleRegisterApplication}
            canCreate={canCreate}
            canManageArchived={canManageArchived}
            canRestore={canManageArchived}
        />
    );
}
