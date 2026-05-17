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
import { DataTablePagination, Input, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useId, useState } from 'react';

import { ApplicationsPageHeader } from '../ApplicationsPageHeader';
import { ApplicationListTable } from './ApplicationListTable';
import { ApplicationRestoreDialog } from './ApplicationRestoreDialog';
import { ApplicationStatsCards } from './ApplicationStatsCards';
import type { ApplicationStats } from '../../hooks/useApplicationStats';
import { useRestoreApplication } from '../../hooks/useRestoreApplication';
import type { ApplicationListItem, ApplicationStatus } from '../../types/application';

const STATUS_OPTIONS: { value: ApplicationStatus; label: string }[] = [
    { value: 'ACTIVE', label: 'Active' },
    { value: 'ARCHIVED', label: 'Archived' },
];
const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

interface ApplicationsListViewProps {
    readonly applications: ApplicationListItem[];
    readonly totalCount: number;
    readonly stats: ApplicationStats;
    readonly isLoading: boolean;
    readonly search: string;
    readonly status: ApplicationStatus;
    readonly page: number;
    readonly perPage: number;
    readonly onSearchChange: (value: string) => void;
    readonly onStatusChange: (status: ApplicationStatus) => void;
    readonly onPageChange: (page: number) => void;
    readonly onPerPageChange: (perPage: number) => void;
    readonly onRegisterApplication: () => void;
    readonly canCreate: boolean;
    readonly canManageArchived: boolean;
    readonly canRestore: boolean;
}

export function ApplicationsListView({
    applications,
    totalCount,
    stats,
    isLoading,
    search,
    status,
    page,
    perPage,
    onSearchChange,
    onStatusChange,
    onPageChange,
    onPerPageChange,
    onRegisterApplication,
    canCreate,
    canManageArchived,
    canRestore,
}: ApplicationsListViewProps) {
    const searchInputId = useId();
    const [restoreTarget, setRestoreTarget] = useState<ApplicationListItem | null>(null);
    const [restoreError, setRestoreError] = useState<string | null>(null);
    const restoreMutation = useRestoreApplication();

    const handleRestore = () => {
        if (!restoreTarget) return;
        setRestoreError(null);
        restoreMutation.mutate(restoreTarget.id, {
            onSuccess: () => setRestoreTarget(null),
            onError: (e: unknown) => setRestoreError(e instanceof Error ? e.message : 'Failed to restore application.'),
        });
    };

    return (
        <div className="space-y-6">
            <ApplicationsPageHeader
                canCreate={canCreate && status === 'ACTIVE'}
                onRegisterApplication={onRegisterApplication}
                showInfoTooltip
            />

            <ApplicationStatsCards stats={stats} />

            <div className="flex items-center justify-between gap-4">
                <div className="flex min-w-0 items-center gap-3">
                    <div className="relative w-72 shrink-0">
                        <SearchIcon
                            className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                            aria-hidden
                        />
                        <label htmlFor={searchInputId} className="sr-only">
                            Search applications
                        </label>
                        <Input
                            id={searchInputId}
                            placeholder="Search applications..."
                            value={search}
                            onChange={e => onSearchChange(e.target.value)}
                            className="pl-9"
                        />
                    </div>

                    {canManageArchived ? (
                        <Select value={status} onValueChange={value => onStatusChange(value as ApplicationStatus)}>
                            <SelectTrigger className="h-9 w-36 shrink-0" aria-label="Filter by status">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {STATUS_OPTIONS.map(option => (
                                    <SelectItem key={option.value} value={option.value}>
                                        {option.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    ) : null}
                </div>

                <DataTablePagination
                    page={page}
                    pageSize={perPage}
                    totalCount={totalCount}
                    pageSizeOptions={PAGE_SIZE_OPTIONS}
                    onPageChange={onPageChange}
                    onPageSizeChange={onPerPageChange}
                />
            </div>

            <ApplicationListTable
                applications={applications}
                isLoading={isLoading}
                status={status}
                skeletonRowCount={perPage}
                canRestore={canRestore}
                onRestore={
                    canRestore
                        ? application => {
                              setRestoreError(null);
                              setRestoreTarget(application);
                          }
                        : undefined
                }
            />

            <ApplicationRestoreDialog
                application={restoreTarget}
                onClose={() => {
                    setRestoreTarget(null);
                    setRestoreError(null);
                }}
                onConfirm={handleRestore}
                isLoading={restoreMutation.isPending}
                error={restoreError}
            />

            <div className="flex justify-end">
                <DataTablePagination
                    page={page}
                    pageSize={perPage}
                    totalCount={totalCount}
                    pageSizeOptions={PAGE_SIZE_OPTIONS}
                    onPageChange={onPageChange}
                    onPageSizeChange={onPerPageChange}
                />
            </div>
        </div>
    );
}
