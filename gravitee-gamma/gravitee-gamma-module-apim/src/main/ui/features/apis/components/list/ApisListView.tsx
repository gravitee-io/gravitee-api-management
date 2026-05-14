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
import { Button, DataTablePagination, Input } from '@gravitee/graphene-core';
import { PlusIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';

import { ApiListTable } from './ApiListTable';
import { ApiStatsCards } from './ApiStatsCards';
import type { ApiListItem } from '../../types/api';

interface ApisListViewProps {
    readonly apis: ApiListItem[];
    readonly totalCount: number;
    readonly isLoading: boolean;
    readonly search: string;
    readonly debouncedSearch: string;
    readonly page: number;
    readonly perPage: number;
    readonly onSearchChange: (value: string) => void;
    readonly onPageChange: (page: number) => void;
    readonly onPerPageChange: (perPage: number) => void;
    readonly onCreateProxy: () => void;
    readonly canCreate: boolean;
}

export function ApisListView({
    apis,
    totalCount,
    isLoading,
    search,
    debouncedSearch,
    page,
    perPage,
    onSearchChange,
    onPageChange,
    onPerPageChange,
    onCreateProxy,
    canCreate,
}: ApisListViewProps) {
    const searchInputId = useId();

    return (
        <div className="space-y-6">
            {/* Page header */}
            <div className="flex items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">API Proxies</h1>
                    <p className="text-sm text-muted-foreground">Manage and monitor your API proxies</p>
                </div>
                {canCreate && (
                    <Button onClick={onCreateProxy} className="shrink-0">
                        <PlusIcon className="size-4" aria-hidden />
                        Create New Proxy
                    </Button>
                )}
            </div>

            {/* Stats cards — counts reflect the debounced search to avoid per-keystroke flicker */}
            <ApiStatsCards query={debouncedSearch || undefined} />

            {/* Search + pagination row */}
            <div className="flex items-center justify-between gap-4">
                <div className="relative w-72 shrink-0">
                    <SearchIcon
                        className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                        aria-hidden
                    />
                    <label htmlFor={searchInputId} className="sr-only">
                        Search APIs
                    </label>
                    <Input
                        id={searchInputId}
                        placeholder="Search APIs..."
                        value={search}
                        onChange={e => onSearchChange(e.target.value)}
                        className="pl-9"
                    />
                </div>

                <DataTablePagination
                    page={page}
                    pageSize={perPage}
                    totalCount={totalCount}
                    pageSizeOptions={[10, 25, 50, 100]}
                    onPageChange={onPageChange}
                    onPageSizeChange={onPerPageChange}
                />
            </div>

            <ApiListTable apis={apis} isLoading={isLoading} skeletonRowCount={perPage} />

            {/* Bottom pagination */}
            <div className="flex justify-end">
                <DataTablePagination
                    page={page}
                    pageSize={perPage}
                    totalCount={totalCount}
                    pageSizeOptions={[10, 25, 50, 100]}
                    onPageChange={onPageChange}
                    onPageSizeChange={onPerPageChange}
                />
            </div>
        </div>
    );
}
