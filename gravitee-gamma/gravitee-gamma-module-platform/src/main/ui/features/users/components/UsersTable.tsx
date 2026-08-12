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
    DateCell,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useId, useMemo } from 'react';
import { Link } from 'react-router-dom';

import { UserAvatar } from './UserAvatar';
import { NON_SORTABLE_COLUMN } from '../../applications/utils/dataTableHeaders';
import type { ColCell } from '../../applications/utils/dataTableTypes';
import type { OrganizationUser } from '../types/user';
import { USER_LIST_PAGE_SIZE_OPTIONS } from '../utils/paginationConstants';
import {
    canDeleteOrganizationUser,
    formatSourceLabel,
    formatUserStatus,
    isOrganizationServiceAccount,
    statusBadgeVariant,
} from '../utils/userDisplay';

function buildColumns(canDelete: boolean, onDeleteUser?: (user: OrganizationUser) => void): DataTableProps<OrganizationUser>['columns'] {
    const columns: DataTableProps<OrganizationUser>['columns'] = [
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
                                        Primary Owner
                                    </Badge>
                                ) : null}
                                {isOrganizationServiceAccount(user) ? (
                                    <Badge variant="outline" className="text-xs uppercase">
                                        Service account
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

    if (canDelete && onDeleteUser) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            cell: ({ row }: ColCell<OrganizationUser>) => {
                const user = row.original;
                if (!canDeleteOrganizationUser(user)) {
                    return <div className="flex justify-end" />;
                }
                const displayName = user.displayName ?? user.email ?? user.id;
                return (
                    <div className="flex justify-end">
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-8 text-muted-foreground hover:text-destructive"
                            aria-label={`Delete user ${displayName}`}
                            onClick={() => onDeleteUser(user)}
                        >
                            <Trash2Icon className="size-4" aria-hidden />
                        </Button>
                    </div>
                );
            },
        });
    }

    return columns;
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
    readonly canDelete?: boolean;
    readonly onDeleteUser?: (user: OrganizationUser) => void;
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
    canDelete = false,
    onDeleteUser,
}: UsersTableProps) {
    const searchInputId = useId();
    const columns = useMemo(() => buildColumns(canDelete, onDeleteUser), [canDelete, onDeleteUser]);

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
                <div className="w-64">
                    <label htmlFor={searchInputId} className="sr-only">
                        Search users
                    </label>
                    <InputGroup>
                        <InputGroupAddon align="inline-start">
                            <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                        </InputGroupAddon>
                        <InputGroupInput
                            id={searchInputId}
                            placeholder="Search by name, email, or ID..."
                            value={search}
                            onChange={e => onSearchChange(e.target.value)}
                        />
                    </InputGroup>
                </div>
            }
        />
    );
}
