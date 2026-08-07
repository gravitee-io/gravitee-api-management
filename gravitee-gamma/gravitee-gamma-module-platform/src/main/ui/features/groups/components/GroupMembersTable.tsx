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
    type DataTableProps,
} from '@gravitee/graphene-core';
import { SearchIcon, UsersRoundIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { GroupMember } from '../types/group';

const PAGE_SIZE = 10;

function roleCell(member: GroupMember, scope: 'API' | 'APPLICATION' | 'API_PRODUCT' | 'INTEGRATION' | 'CLUSTER') {
    const role = member.roles?.[scope];
    return <span className="text-sm text-muted-foreground">{role ?? '—'}</span>;
}

function buildColumns(): DataTableProps<GroupMember>['columns'] {
    return [
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
}

function paginate(items: GroupMember[], page: number, pageSize: number): GroupMember[] {
    const start = (page - 1) * pageSize;
    return items.slice(start, start + pageSize);
}

interface GroupMembersTableProps {
    readonly members: GroupMember[];
    readonly loading: boolean;
}

// Member management (add/invite/edit roles/remove) is added on top of this read-only view in a
// follow-up PR (FOUND-106/FOUND-107) — this component intentionally has no action column yet.
export function GroupMembersTable({ members, loading }: GroupMembersTableProps) {
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(PAGE_SIZE);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        return query ? members.filter(member => member.displayName.toLowerCase().includes(query)) : members;
    }, [members, search]);

    const totalCount = filtered.length;
    const totalPages = pageSize > 0 ? Math.max(1, Math.ceil(totalCount / pageSize)) : 1;
    const pageData = useMemo(() => paginate(filtered, page, pageSize), [filtered, page, pageSize]);
    const columns = useMemo(() => buildColumns(), []);

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
                        icon={<SearchIcon className="size-8" aria-hidden />}
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
                        icon={<UsersRoundIcon className="size-8" aria-hidden />}
                        title="No members yet"
                        description="Add users to this group to get started."
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
