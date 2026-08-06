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

import {
    Badge,
    Button,
    DataTable,
    DataTableColumnHeader,
    DataTableEmptyState,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { SearchIcon, UsersRoundIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { OrganizationGroup } from '../types/group';

const PAGE_SIZE = 10;
const ALL_ENVIRONMENTS = '__all__';

function buildColumns(): DataTableProps<OrganizationGroup>['columns'] {
    return [
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<OrganizationGroup>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<OrganizationGroup>) => <span className="text-sm font-medium">{row.original.name}</span>,
        },
        {
            id: 'environment',
            accessorKey: 'environmentName',
            header: ({ column }: ColHeader<OrganizationGroup>) => <DataTableColumnHeader column={column} title="Environment" />,
            cell: ({ row }: ColCell<OrganizationGroup>) => (
                <Badge variant="default" className="text-xs font-normal">
                    {row.original.environmentName}
                </Badge>
            ),
        },
    ];
}

function paginate(items: OrganizationGroup[], page: number, pageSize: number): OrganizationGroup[] {
    const start = (page - 1) * pageSize;
    return items.slice(start, start + pageSize);
}

interface OrganizationGroupsTableProps {
    readonly groups: OrganizationGroup[];
    readonly loading: boolean;
}

// Member count and per-scope roles aren't part of GroupSimpleEntity (the backend entity this org-wide
// list is built from), so this table is deliberately name + environment only — richer detail is only
// available per-environment, from the group's own detail page.
export function OrganizationGroupsTable({ groups, loading }: OrganizationGroupsTableProps) {
    const [search, setSearch] = useState('');
    const [environmentFilter, setEnvironmentFilter] = useState(ALL_ENVIRONMENTS);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(PAGE_SIZE);

    const environments = useMemo(() => {
        const seen = new Map<string, string>();
        for (const group of groups) {
            if (!seen.has(group.environmentId)) seen.set(group.environmentId, group.environmentName);
        }
        return [...seen.entries()].sort((a, b) => a[1].localeCompare(b[1]));
    }, [groups]);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        return groups.filter(group => {
            const matchesSearch = !query || group.name.toLowerCase().includes(query);
            const matchesEnvironment = environmentFilter === ALL_ENVIRONMENTS || group.environmentId === environmentFilter;
            return matchesSearch && matchesEnvironment;
        });
    }, [groups, search, environmentFilter]);

    const totalCount = filtered.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
    const pageData = useMemo(() => paginate(filtered, page, pageSize), [filtered, page, pageSize]);
    const columns = useMemo(() => buildColumns(), []);

    useEffect(() => {
        if (page > totalPages) setPage(totalPages);
    }, [page, totalPages]);

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function handleEnvironmentFilterChange(value: string) {
        setEnvironmentFilter(value);
        setPage(1);
    }

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    const hasActiveFilter = Boolean(search) || environmentFilter !== ALL_ENVIRONMENTS;

    return (
        <DataTable
            aria-label="Organization groups"
            columns={columns}
            data={pageData}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                onPageChange: setPage,
                onPageSizeChange: handlePageSizeChange,
            }}
            emptyMessage={
                hasActiveFilter ? (
                    <DataTableEmptyState
                        variant="no-results"
                        icon={<SearchIcon className="size-8" aria-hidden />}
                        title="No groups match your filters"
                        description="Try adjusting your search or environment filter."
                        action={
                            <Button
                                size="sm"
                                variant="outline"
                                onClick={() => {
                                    handleSearchChange('');
                                    handleEnvironmentFilterChange(ALL_ENVIRONMENTS);
                                }}
                            >
                                Clear filters
                            </Button>
                        }
                    />
                ) : (
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<UsersRoundIcon className="size-8" aria-hidden />}
                        title="No groups"
                        description="Groups created across every environment in this organization will appear here."
                    />
                )
            }
            toolbar={
                <div className="flex flex-wrap items-center gap-2">
                    <div className="w-64">
                        <InputGroup>
                            <InputGroupAddon align="inline-start">
                                <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                            </InputGroupAddon>
                            <InputGroupInput
                                placeholder="Search by name…"
                                value={search}
                                onChange={e => handleSearchChange(e.target.value)}
                            />
                        </InputGroup>
                    </div>
                    <Select value={environmentFilter} onValueChange={handleEnvironmentFilterChange}>
                        <SelectTrigger aria-label="Filter by environment" className="w-48">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value={ALL_ENVIRONMENTS}>All environments</SelectItem>
                            {environments.map(([id, name]) => (
                                <SelectItem key={id} value={id}>
                                    {name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            }
        />
    );
}
