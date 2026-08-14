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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { GroupMember } from '../types/group';
import { PRIMARY_OWNER_ROLE } from '../types/group';
import { paginate, totalPagesFor } from '../utils/clientPagination';

const PAGE_SIZE = 10;

function roleCell(member: GroupMember, scope: 'API' | 'APPLICATION' | 'API_PRODUCT' | 'INTEGRATION' | 'CLUSTER') {
    const role = member.roles?.[scope];
    return <span className="text-sm text-muted-foreground">{role ?? '—'}</span>;
}

function isPrimaryOwnerMember(member: GroupMember): boolean {
    return member.roles?.API === PRIMARY_OWNER_ROLE || member.roles?.API_PRODUCT === PRIMARY_OWNER_ROLE;
}

function isRemoveDisabled(member: GroupMember, totalMemberCount: number): boolean {
    return totalMemberCount === 1 && isPrimaryOwnerMember(member);
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
            header: ({ column }: ColHeader<GroupMember>) => <DataTableColumnHeader column={column} title="Member" />,
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
    ];

    if (canManageMembers) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<GroupMember>) => {
                const removeDisabled = isRemoveDisabled(row.original, totalMemberCount);
                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="size-8" aria-label="Member actions">
                                    <MoreHorizontalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                <DropdownMenuItem onSelect={() => onEditRoles(row.original)}>
                                    <PencilIcon className="size-4 mr-2" aria-hidden />
                                    Edit roles
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem variant="destructive" disabled={removeDisabled} onSelect={() => onRemove(row.original)}>
                                    <Trash2Icon className="size-4 mr-2" aria-hidden />
                                    Remove member
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        });
    }

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

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        return query ? members.filter(member => (member.displayName ?? '').toLowerCase().includes(query)) : members;
    }, [members, search]);

    const totalCount = filtered.length;
    const totalPages = totalPagesFor(totalCount, pageSize);
    const pageData = useMemo(() => paginate(filtered, page, pageSize), [filtered, page, pageSize]);
    const columns = useMemo(
        () => buildColumns({ canManageMembers, totalMemberCount: members.length, onEditRoles, onRemove }),
        [canManageMembers, members.length, onEditRoles, onRemove],
    );

    useEffect(() => {
        if (page > totalPages) setPage(totalPages);
    }, [page, totalPages]);

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    return (
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
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                onPageChange: setPage,
                onPageSizeChange: handlePageSizeChange,
            }}
            emptyMessage={
                search ? (
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
                <div className="w-64">
                    <InputGroup>
                        <InputGroupAddon align="inline-start">
                            <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                        </InputGroupAddon>
                        <InputGroupInput placeholder="Search members…" value={search} onChange={e => handleSearchChange(e.target.value)} />
                    </InputGroup>
                </div>
            }
        />
    );
}
