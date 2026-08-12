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
    DataTable,
    DataTableColumnHeader,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { GroupMembershipItem } from '../types/group';

const PAGE_SIZE = 10;

function buildColumns(showVersionColumn: boolean): DataTableProps<GroupMembershipItem>['columns'] {
    const columns: DataTableProps<GroupMembershipItem>['columns'] = [
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<GroupMembershipItem>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<GroupMembershipItem>) => <span className="text-sm font-medium">{row.original.name}</span>,
        },
    ];
    if (showVersionColumn) {
        columns.push({
            id: 'version',
            header: 'Version',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupMembershipItem>) => (
                <span className="text-sm text-muted-foreground">{row.original.version ?? '—'}</span>
            ),
        });
    }
    return columns;
}

function paginate(items: GroupMembershipItem[], page: number, pageSize: number): GroupMembershipItem[] {
    const start = (page - 1) * pageSize;
    return items.slice(start, start + pageSize);
}

interface GroupMembershipTableProps {
    readonly items: GroupMembershipItem[];
    readonly loading: boolean;
    readonly ariaLabel: string;
    readonly searchPlaceholder: string;
    readonly emptyMessage: string;
    readonly showVersionColumn?: boolean;
}

export function GroupMembershipTable({
    items,
    loading,
    ariaLabel,
    searchPlaceholder,
    emptyMessage,
    showVersionColumn = true,
}: GroupMembershipTableProps) {
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(PAGE_SIZE);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        return query ? items.filter(item => item.name.toLowerCase().includes(query)) : items;
    }, [items, search]);

    const totalCount = filtered.length;
    const totalPages = pageSize > 0 ? Math.max(1, Math.ceil(totalCount / pageSize)) : 1;
    const pageData = useMemo(() => paginate(filtered, page, pageSize), [filtered, page, pageSize]);
    const columns = useMemo(() => buildColumns(showVersionColumn), [showVersionColumn]);

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
            aria-label={ariaLabel}
            columns={columns}
            data={pageData}
            loading={loading}
            skeletonCount={pageSize}
            // Data is fetched in full up front and paginated/filtered here client-side — `serverSide` just
            // tells DataTable not to layer its own client-side sort/filter/pagination on top of what we
            // already did (see GroupMembersTable.tsx for the same pattern).
            serverSide
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                onPageChange: setPage,
                onPageSizeChange: handlePageSizeChange,
            }}
            // Matches classic Console's group.component.html, which shows this same plain text regardless
            // of whether the table is empty because there's no data or because a search matched none.
            emptyMessage={emptyMessage}
            toolbar={
                <div className="w-64">
                    <InputGroup>
                        <InputGroupAddon align="inline-start">
                            <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                        </InputGroupAddon>
                        <InputGroupInput
                            placeholder={searchPlaceholder}
                            value={search}
                            onChange={e => handleSearchChange(e.target.value)}
                        />
                    </InputGroup>
                </div>
            }
        />
    );
}
