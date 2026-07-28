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
import { Badge, Button, DataTable, DataTableEmptyState, DateCell, Input } from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useId, useMemo } from 'react';
import { Link } from 'react-router-dom';

import { UserAvatar } from './UserAvatar';
import { NON_SORTABLE_COLUMN } from '../../applications/utils/dataTableHeaders';
import type { ColCell } from '../../applications/utils/dataTableTypes';
import type { OrganizationUser } from '../types/user';
import { USER_LIST_PAGE_SIZE_OPTIONS } from '../utils/paginationConstants';
import { formatRoleSummary, formatSourceLabel, formatUserStatus, statusBadgeVariant } from '../utils/userDisplay';

function buildColumns() {
    return [
        {
            id: 'user',
            accessorFn: (user: OrganizationUser) => user.displayName ?? user.email ?? user.id,
            header: 'User',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<OrganizationUser>) => {
                const user = row.original;
                const displayName = user.displayName ?? user.email ?? user.id;
                return (
                    <div className="flex items-center gap-3 min-w-0">
                        <UserAvatar name={displayName} />
                        <div className="min-w-0">
                            <div className="flex flex-wrap items-center gap-2">
                                <Button asChild variant="link" className="h-auto p-0 font-medium">
                                    <Link to={user.id}>
                                        <span className="truncate" title={displayName}>
                                            {displayName}
                                        </span>
                                    </Link>
                                </Button>
                                {user.primary_owner ? (
                                    <Badge variant="outline" className="text-xs uppercase">
                                        Owner
                                    </Badge>
                                ) : null}
                                {(user.number_of_active_tokens ?? 0) > 0 ? (
                                    <Badge variant="warning" className="text-xs">
                                        Active Token{user.number_of_active_tokens === 1 ? '' : 's'}
                                    </Badge>
                                ) : null}
                            </div>
                            {user.email ? <p className="text-sm text-muted-foreground truncate">{user.email}</p> : null}
                        </div>
                    </div>
                );
            },
        },
        {
            id: 'status',
            accessorFn: (user: OrganizationUser) => user.status ?? '',
            header: 'Status',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<OrganizationUser>) => (
                <Badge variant={statusBadgeVariant(row.original.status)}>{formatUserStatus(row.original.status)}</Badge>
            ),
        },
        {
            id: 'source',
            accessorFn: (user: OrganizationUser) => user.source ?? '',
            header: 'Source',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<OrganizationUser>) => (
                <Badge variant="outline" className="font-normal">
                    {formatSourceLabel(row.original.source)}
                </Badge>
            ),
        },
        {
            id: 'roles',
            accessorFn: (user: OrganizationUser) => formatRoleSummary(user.roles),
            header: 'Roles',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<OrganizationUser>) => (
                <span className="text-sm text-muted-foreground">{formatRoleSummary(row.original.roles)}</span>
            ),
        },
        {
            id: 'lastActivity',
            accessorFn: (user: OrganizationUser) => user.lastConnectionAt ?? 0,
            header: 'Last Login',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<OrganizationUser>) => {
                const lastConnectionAt = row.original.lastConnectionAt;
                if (!lastConnectionAt) {
                    return <span className="text-sm text-muted-foreground">Never</span>;
                }
                return <DateCell value={new Date(lastConnectionAt)} />;
            },
        },
    ];
}

interface UsersTableProps {
    readonly users: OrganizationUser[];
    readonly totalCount: number;
    readonly loading: boolean;
    readonly isFirstUse: boolean;
    readonly search: string;
    readonly page: number;
    readonly pageSize: number;
    readonly onSearchChange: (value: string) => void;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
    readonly onAddUser?: () => void;
}

export function UsersTable({
    users,
    totalCount,
    loading,
    isFirstUse,
    search,
    page,
    pageSize,
    onSearchChange,
    onPageChange,
    onPageSizeChange,
    onAddUser,
}: UsersTableProps) {
    const searchInputId = useId();
    const columns = useMemo(() => buildColumns(), []);

    if (isFirstUse) {
        return (
            <div className="rounded-lg border">
                <DataTableEmptyState
                    variant="first-use"
                    icon={<SearchIcon className="size-8" aria-hidden />}
                    title="No users yet"
                    description="Get started by inviting your first team member."
                    primaryAction={onAddUser ? <Button onClick={onAddUser}>Add User</Button> : undefined}
                />
            </div>
        );
    }

    return (
        <DataTable
            columns={columns}
            data={users}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...USER_LIST_PAGE_SIZE_OPTIONS],
                onPageChange,
                onPageSizeChange: size => {
                    onPageSizeChange(size);
                    onPageChange(1);
                },
            }}
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon className="size-8" aria-hidden />}
                    title="No users match your search"
                    description="Try adjusting your search terms."
                    action={
                        <Button
                            size="sm"
                            variant="outline"
                            onClick={() => {
                                onSearchChange('');
                                onPageChange(1);
                            }}
                        >
                            Clear search
                        </Button>
                    }
                />
            }
            toolbar={
                <div className="relative w-64">
                    <SearchIcon
                        className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                        aria-hidden
                    />
                    <label htmlFor={searchInputId} className="sr-only">
                        Search users
                    </label>
                    <Input
                        id={searchInputId}
                        placeholder="Search by name, email, or ID..."
                        value={search}
                        onChange={e => onSearchChange(e.target.value)}
                        className="h-8 w-64 pl-9"
                    />
                </div>
            }
        />
    );
}
