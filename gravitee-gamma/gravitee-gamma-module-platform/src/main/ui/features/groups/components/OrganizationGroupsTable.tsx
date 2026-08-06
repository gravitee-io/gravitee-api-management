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
    DataTableEmptyState,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { SearchIcon, UsersRoundIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useId, useMemo, useState } from 'react';

import { ClientSideTableSearchField } from '../../../shared/components/ClientSideTableSearchField';
import { useClientSideTableState } from '../../../shared/hooks/useClientSideTableState';
import type { OrganizationGroup } from '../../../shared/types/organizationGroup';
import type { ColCell } from '../../../shared/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../../shared/utils/paginationConstants';

const ALL_ENVIRONMENTS = 'all';
const ORGANIZATION_GROUP_SEARCH_IGNORE_KEYS = ['id', 'environmentId', 'environmentName'] as const;

function matchesGroupName(group: OrganizationGroup, normalizedSearch: string): boolean {
    return group.name.toLowerCase().includes(normalizedSearch);
}

const columns: DataTableProps<OrganizationGroup>['columns'] = [
    {
        id: 'name',
        accessorKey: 'name',
        header: 'Name',
        cell: ({ row }: ColCell<OrganizationGroup>) => <span className="font-medium">{row.original.name}</span>,
    },
    {
        id: 'environment',
        accessorKey: 'environmentName',
        header: 'Environment',
        cell: ({ row }: ColCell<OrganizationGroup>) => <Badge variant="secondary">{row.original.environmentName}</Badge>,
    },
];

interface OrganizationGroupsTableProps {
    readonly groups: OrganizationGroup[];
    readonly loading: boolean;
}

export function OrganizationGroupsTable({ groups, loading }: OrganizationGroupsTableProps) {
    const searchInputId = useId();
    const [environmentId, setEnvironmentId] = useState(ALL_ENVIRONMENTS);
    const environmentOptions = useMemo(
        () =>
            [...new Map(groups.map(group => [group.environmentId, group.environmentName])).entries()]
                .map(([id, name]) => ({ id, name }))
                .sort((left, right) => left.name.localeCompare(right.name)),
        [groups],
    );
    const selectedEnvironmentId =
        environmentId === ALL_ENVIRONMENTS || environmentOptions.some(environment => environment.id === environmentId)
            ? environmentId
            : ALL_ENVIRONMENTS;
    const groupsInEnvironment = useMemo(
        () => (selectedEnvironmentId === ALL_ENVIRONMENTS ? groups : groups.filter(group => group.environmentId === selectedEnvironmentId)),
        [groups, selectedEnvironmentId],
    );
    const { search, page, pageSize, totalCount, paginatedItems, hasActiveSearch, handleSearchChange, handlePageSizeChange, setPage } =
        useClientSideTableState(groupsInEnvironment, ORGANIZATION_GROUP_SEARCH_IGNORE_KEYS, { matchesSearch: matchesGroupName });
    const hasActiveFilters = hasActiveSearch || selectedEnvironmentId !== ALL_ENVIRONMENTS;

    useEffect(() => {
        if (environmentId !== selectedEnvironmentId) {
            setEnvironmentId(selectedEnvironmentId);
        }
    }, [environmentId, selectedEnvironmentId]);

    function handleEnvironmentChange(value: string) {
        setEnvironmentId(value);
        setPage(1);
    }

    function clearFilters() {
        handleSearchChange('');
        setEnvironmentId(ALL_ENVIRONMENTS);
    }

    if (!loading && groups.length === 0) {
        return (
            <div className="rounded-lg border">
                <DataTableEmptyState
                    variant="first-use"
                    icon={<UsersRoundIcon className="size-8" aria-hidden />}
                    title="No organization groups"
                    description="No groups exist in this organization."
                />
            </div>
        );
    }

    return (
        <DataTable
            aria-label="Organization groups"
            columns={columns}
            data={paginatedItems}
            loading={loading}
            skeletonCount={pageSize}
            // DataTable owns the pagination controls while this component slices the unpaginated organization response.
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
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon className="size-8" aria-hidden />}
                    title="No organization groups match your filters"
                    description="Try adjusting your search or environment filter."
                    action={
                        hasActiveFilters ? (
                            <Button size="sm" variant="outline" onClick={clearFilters}>
                                Clear filters
                            </Button>
                        ) : undefined
                    }
                />
            }
            toolbar={
                <div className="flex flex-wrap items-center gap-2">
                    <ClientSideTableSearchField
                        id={searchInputId}
                        label="Search organization groups"
                        value={search}
                        onChange={handleSearchChange}
                        placeholder="Search by group name…"
                    />
                    <Select value={selectedEnvironmentId} onValueChange={handleEnvironmentChange}>
                        <SelectTrigger aria-label="Filter by environment" className="w-64">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value={ALL_ENVIRONMENTS}>All environments</SelectItem>
                            {environmentOptions.map(environment => (
                                <SelectItem key={environment.id} value={environment.id}>
                                    {environment.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            }
        />
    );
}
