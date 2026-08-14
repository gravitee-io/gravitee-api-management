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
    Button,
    DataTable,
    DataTableColumnHeader,
    DataTableEmptyState,
    DateCell,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MailIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { GroupInvitation } from '../types/group';
import { paginate, totalPagesFor } from '../utils/clientPagination';

const PAGE_SIZE = 10;

function buildColumns({
    canManageMembers,
    onDelete,
}: {
    canManageMembers: boolean;
    onDelete: (invitation: GroupInvitation) => void;
}): DataTableProps<GroupInvitation>['columns'] {
    const columns: DataTableProps<GroupInvitation>['columns'] = [
        {
            id: 'email',
            accessorKey: 'email',
            header: ({ column }: ColHeader<GroupInvitation>) => <DataTableColumnHeader column={column} title="Email" />,
            cell: ({ row }: ColCell<GroupInvitation>) => <span className="text-sm font-medium">{row.original.email}</span>,
        },
        {
            id: 'apiRole',
            header: 'API role',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupInvitation>) => (
                <span className="text-sm text-muted-foreground">{row.original.api_role ?? '—'}</span>
            ),
        },
        {
            id: 'applicationRole',
            header: 'Application role',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupInvitation>) => (
                <span className="text-sm text-muted-foreground">{row.original.application_role ?? '—'}</span>
            ),
        },
        {
            id: 'invitationDate',
            header: 'Invitation date',
            enableSorting: false,
            cell: ({ row }: ColCell<GroupInvitation>) =>
                row.original.created_at ? (
                    <DateCell value={new Date(row.original.created_at)} format="absolute" />
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
    ];

    if (canManageMembers) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<GroupInvitation>) => (
                <div className="flex justify-end">
                    <Button
                        variant="ghost"
                        size="icon"
                        className="size-8"
                        aria-label={`Delete invitation sent to ${row.original.email}`}
                        onClick={() => onDelete(row.original)}
                    >
                        <Trash2Icon className="size-4" aria-hidden />
                    </Button>
                </div>
            ),
        });
    }

    return columns;
}

interface GroupInvitationsTableProps {
    readonly invitations: GroupInvitation[];
    readonly loading: boolean;
    readonly canManageMembers: boolean;
    readonly onDelete: (invitation: GroupInvitation) => void;
}

export function GroupInvitationsTable({ invitations, loading, canManageMembers, onDelete }: GroupInvitationsTableProps) {
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(PAGE_SIZE);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        return query ? invitations.filter(invitation => invitation.email.toLowerCase().includes(query)) : invitations;
    }, [invitations, search]);

    const totalCount = filtered.length;
    const totalPages = totalPagesFor(totalCount, pageSize);
    const pageData = useMemo(() => paginate(filtered, page, pageSize), [filtered, page, pageSize]);
    const columns = useMemo(() => buildColumns({ canManageMembers, onDelete }), [canManageMembers, onDelete]);

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
            aria-label="Invitations"
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
                        title="No invitations match your search"
                        description="Try adjusting your search terms."
                        action={
                            <Button size="sm" variant="outline" onClick={() => handleSearchChange('')}>
                                Clear search
                            </Button>
                        }
                    />
                ) : (
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<MailIcon className="size-8" aria-hidden />}
                        title="No invitations sent to display"
                        description="Invitations sent to this group will appear here."
                    />
                )
            }
            toolbar={
                <div className="w-64">
                    <InputGroup>
                        <InputGroupAddon align="inline-start">
                            <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                        </InputGroupAddon>
                        <InputGroupInput
                            placeholder="Search invitations…"
                            value={search}
                            onChange={e => handleSearchChange(e.target.value)}
                        />
                    </InputGroup>
                </div>
            }
        />
    );
}
