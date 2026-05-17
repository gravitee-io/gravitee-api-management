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
import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { ApplicationsEmptyLanding } from '../features/applications/components';
import { ApplicationsListView } from '../features/applications/components/list';
import { useApplicationList } from '../features/applications/hooks/useApplicationList';
import { useApplicationStats } from '../features/applications/hooks/useApplicationStats';
import { useOrganizationAdmin } from '../features/applications/hooks/useOrganizationAdmin';
import type { ApplicationStatus } from '../features/applications/types/application';
import type { ApplicationsListLocationState } from '../features/applications/types/navigation';
import { SuccessBanner } from '../features/shared/components';

const DEFAULT_PAGE = 1;
const DEFAULT_PER_PAGE = 25;
const DEFAULT_STATUS: ApplicationStatus = 'ACTIVE';
const SEARCH_DEBOUNCE_MS = 300;

export function ApplicationsPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const canCreate = useHasPermission({ anyOf: ['environment-application-c'] });
    const { isAdmin: canManageArchived, isLoading: isAdminLoading } = useOrganizationAdmin();

    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [status, setStatus] = useState<ApplicationStatus>(DEFAULT_STATUS);
    const [page, setPage] = useState(DEFAULT_PAGE);
    const [perPage, setPerPage] = useState(DEFAULT_PER_PAGE);

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
        const state = location.state as ApplicationsListLocationState | null;
        const message = state?.successMessage;
        if (!message) return;

        setSuccessMessage(message);
        navigate(location.pathname, { replace: true, state: null });
    }, [location.pathname, location.state, navigate]);

    const { data, isLoading, isFetching, isError } = useApplicationList({
        query: debouncedSearch,
        status,
        page,
        perPage,
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
        setPage(DEFAULT_PAGE);
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
        <div className="space-y-4">
            {successMessage && <SuccessBanner message={successMessage} />}
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
                onPageChange={setPage}
                onPerPageChange={handlePerPageChange}
                onRegisterApplication={handleRegisterApplication}
                canCreate={canCreate}
                canManageArchived={canManageArchived}
                canRestore={canManageArchived}
            />
        </div>
    );
}
