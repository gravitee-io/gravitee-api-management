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
    cn,
    DataTable,
    DataTableEmptyState,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreVerticalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useDeferredValue, useMemo, useState } from 'react';

import { ClientSideTableSearchField } from '../../../shared/components/ClientSideTableSearchField';
import { paginate, totalPagesFor } from '../../../shared/utils/clientPagination';
import type { ColCell } from '../../../shared/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../../shared/utils/paginationConstants';
import type { GroupMember } from '../types/group';
import { primaryOwnerScopesOf } from '../utils/primaryOwnership';

const PAGE_SIZE = 10;
const REMOVE_DISABLED_MESSAGE = 'Add another member and transfer primary ownership before removing this member.';

function roleCell(member: GroupMember, scope: 'API' | 'APPLICATION' | 'API_PRODUCT' | 'INTEGRATION' | 'CLUSTER' | 'EXPLORER') {
    const role = member.roles?.[scope];
    return <span className="text-sm text-muted-foreground">{role ?? '—'}</span>;
}

function isRemoveDisabled(member: GroupMember, totalMemberCount: number): boolean {
    return totalMemberCount === 1 && primaryOwnerScopesOf(member).length > 0;
}

function buildColumns({
    canManageMembers,
    totalMemberCount,
    onEditRoles,
    onRemove,
}: {
    canManageMembers: boolean;
    totalMemberCount: number;
    onEditRoles: (member: GroupMember) => void;
    onRemove: (member: GroupMember) => void;
}): DataTableProps<GroupMember>['columns'] {
    const columns: DataTableProps<GroupMember>['columns'] = [
        {
            id: 'member',
            accessorKey: 'displayName',
            header: 'Member',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupMember>) => (
                <div className="flex items-center gap-2">
                    <span className="text-sm font-medium">{row.original.displayName}</span>
                    {row.original.roles?.GROUP === 'ADMIN' && (
                        <Badge variant="default" className="text-xs font-normal">
                            Group admin
                        </Badge>
                    )}
                </div>
            ),
        },
        {
            id: 'api',
            header: 'API',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupMember>) => roleCell(row.original, 'API'),
        },
        {
            id: 'apiProduct',
            header: 'API product',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupMember>) => roleCell(row.original, 'API_PRODUCT'),
        },
        {
            id: 'application',
            header: 'Application',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupMember>) => roleCell(row.original, 'APPLICATION'),
        },
        {
            id: 'integration',
            header: 'Integration',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupMember>) => roleCell(row.original, 'INTEGRATION'),
        },
        {
            id: 'cluster',
            header: 'Cluster',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupMember>) => roleCell(row.original, 'CLUSTER'),
        },
        {
            id: 'explorer',
            header: 'Explorer',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupMember>) => roleCell(row.original, 'EXPLORER'),
        },
    ];

    if (!canManageMembers) {
        return columns;
    }

    columns.push({
        id: 'actions',
        header: () => <span className="sr-only">Actions</span>,
        size: 56,
        enableSorting: false,
        enableHiding: false,
        cell: ({ row }: ColCell<GroupMember>) => {
            const removeDisabled = isRemoveDisabled(row.original, totalMemberCount);
            const removeDisabledExplanationId = `remove-member-disabled-${row.original.id}`;
            return (
                <div className="flex justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="size-8"
                                aria-label={`Actions for ${row.original.displayName || 'member'}`}
                            >
                                <MoreVerticalIcon className="size-4" aria-hidden />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onSelect={() => onEditRoles(row.original)}>
                                <PencilIcon className="size-4 mr-2" aria-hidden />
                                Edit roles
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                                variant="destructive"
                                disabled={removeDisabled}
                                aria-describedby={removeDisabled ? removeDisabledExplanationId : undefined}
                                onSelect={() => onRemove(row.original)}
                            >
                                <Trash2Icon className="size-4 mr-2" aria-hidden />
                                Remove member
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                    {/* Kept outside DropdownMenuContent: role="menu" may only contain menu items. */}
                    {removeDisabled && (
                        <span id={removeDisabledExplanationId} className="sr-only">
                            {REMOVE_DISABLED_MESSAGE}
                        </span>
                    )}
                </div>
            );
        },
    });

    return columns;
}

interface GroupMembersTableProps {
    readonly members: GroupMember[];
    readonly loading: boolean;
    readonly canManageMembers: boolean;
    readonly canAddMembers: boolean;
    readonly onEditRoles: (member: GroupMember) => void;
    readonly onRemove: (member: GroupMember) => void;
}

export function GroupMembersTable({ members, loading, canManageMembers, canAddMembers, onEditRoles, onRemove }: GroupMembersTableProps) {
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(PAGE_SIZE);
    const deferredSearch = useDeferredValue(search);
    const isFiltering = search !== deferredSearch;

    const filtered = useMemo(() => {
        const query = deferredSearch.trim().toLowerCase();
        const matchingMembers = query ? members.filter(member => (member.displayName ?? '').toLowerCase().includes(query)) : members;
        return [...matchingMembers].sort((left, right) => (left.displayName ?? '').localeCompare(right.displayName ?? ''));
    }, [members, deferredSearch]);

    const totalCount = filtered.length;
    const hasActiveSearch = deferredSearch.trim().length > 0;
    const totalPages = totalPagesFor(totalCount, pageSize);
    const safePage = Math.min(page, totalPages);
    const pageData = useMemo(() => paginate(filtered, safePage, pageSize), [filtered, safePage, pageSize]);
    const columns = useMemo(
        () => buildColumns({ canManageMembers, totalMemberCount: members.length, onEditRoles, onRemove }),
        [canManageMembers, members.length, onEditRoles, onRemove],
    );

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    return (
        <div aria-busy={isFiltering} className={cn('transition-opacity', isFiltering && 'opacity-60')}>
            <DataTable
                aria-label="Members"
                columns={columns}
                data={pageData}
                loading={loading}
                skeletonCount={pageSize}
                // Pagination is actually client-side (paginate() above already slices `data` down to the
                // current page) — `serverSide` here just tells DataTable not to re-paginate what we hand it,
                // since the `pagination` prop below drives the page controls off our own state instead.
                serverSide
                pagination={{
                    page: safePage,
                    pageSize,
                    totalCount,
                    pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                    onPageChange: setPage,
                    onPageSizeChange: handlePageSizeChange,
                }}
                emptyMessage={
                    hasActiveSearch ? (
                        <DataTableEmptyState
                            variant="no-results"
                            title="No members match your search"
                            description="Try adjusting your search terms."
                            action={
                                <Button
                                    size="sm"
                                    variant="outline"
                                    onClick={() => {
                                        handleSearchChange('');
                                    }}
                                >
                                    Clear search
                                </Button>
                            }
                        />
                    ) : (
                        <DataTableEmptyState
                            variant="first-use"
                            title="No members available to display"
                            description={canAddMembers ? 'Use Add members above to search or invite users.' : ''}
                        />
                    )
                }
                toolbar={
                    <ClientSideTableSearchField
                        id="group-members-search"
                        label="Search members"
                        placeholder="Search members…"
                        value={search}
                        onChange={handleSearchChange}
                    />
                }
            />
        </div>
    );
}
