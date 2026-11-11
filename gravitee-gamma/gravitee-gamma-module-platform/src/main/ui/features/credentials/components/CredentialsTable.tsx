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
    DateCell,
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
import { KeyRoundIcon, MoreVerticalIcon, PencilIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { Link } from 'react-router-dom';

import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { Credential } from '../types/credential';
import { isCredentialExpired } from '../utils/credentialExpiry';
import { credentialStrategyLabel } from '../utils/credentialStrategies';

function Empty() {
    return <span className="text-sm text-muted-foreground">—</span>;
}

function buildColumns({
    ownerNames,
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: {
    ownerNames: Readonly<Record<string, string>>;
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (credential: Credential) => void;
    onDelete: (credential: Credential) => void;
}): DataTableProps<Credential>['columns'] {
    return [
        {
            id: 'name',
            accessorKey: 'name',
            enableSorting: false,
            header: ({ column }: ColHeader<Credential>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<Credential>) => (
                <Link className="text-sm font-medium text-foreground hover:underline" to={row.original.id}>
                    {row.original.name}
                </Link>
            ),
        },
        {
            id: 'provider',
            accessorKey: 'serviceId',
            enableSorting: false,
            header: ({ column }: ColHeader<Credential>) => <DataTableColumnHeader column={column} title="Provider" />,
            cell: ({ row }: ColCell<Credential>) =>
                row.original.serviceId ? (
                    <span className="text-sm">{row.original.serviceId}</span>
                ) : (
                    <span className="text-sm text-muted-foreground">Not set</span>
                ),
        },
        {
            id: 'type',
            accessorKey: 'strategy',
            enableSorting: false,
            header: ({ column }: ColHeader<Credential>) => <DataTableColumnHeader column={column} title="Type" />,
            cell: ({ row }: ColCell<Credential>) => <Badge variant="outline">{credentialStrategyLabel(row.original.strategy)}</Badge>,
        },
        {
            id: 'owner',
            accessorKey: 'ownerId',
            enableSorting: false,
            header: ({ column }: ColHeader<Credential>) => <DataTableColumnHeader column={column} title="Owner" />,
            cell: ({ row }: ColCell<Credential>) =>
                row.original.ownerId ? (
                    <span className="text-sm">{ownerNames[row.original.ownerId] ?? row.original.ownerId}</span>
                ) : (
                    <Empty />
                ),
        },
        {
            id: 'expiresAt',
            accessorKey: 'expiresAt',
            enableSorting: false,
            header: ({ column }: ColHeader<Credential>) => <DataTableColumnHeader column={column} title="Expires" />,
            cell: ({ row }: ColCell<Credential>) =>
                row.original.expiresAt ? (
                    <div className="flex items-center gap-2">
                        <DateCell value={new Date(row.original.expiresAt)} format="absolute" />
                        {isCredentialExpired(row.original) ? <Badge variant="destructive">Expired</Badge> : null}
                    </div>
                ) : (
                    <Empty />
                ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 60,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<Credential>) => {
                const credential = row.original;
                if (!canEdit && !canDelete) {
                    return null;
                }
                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="size-8" aria-label={`${credential.name} actions`}>
                                    <MoreVerticalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                {canEdit && (
                                    <DropdownMenuItem onSelect={() => onEdit(credential)}>
                                        <PencilIcon className="size-4 mr-2" aria-hidden />
                                        Edit
                                    </DropdownMenuItem>
                                )}
                                {canEdit && canDelete && <DropdownMenuSeparator />}
                                {canDelete && (
                                    <DropdownMenuItem variant="destructive" onSelect={() => onDelete(credential)}>
                                        <Trash2Icon className="size-4 mr-2" aria-hidden />
                                        Delete
                                    </DropdownMenuItem>
                                )}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        },
    ];
}

interface CredentialsTableProps {
    readonly credentials: Credential[];
    readonly ownerNames: Readonly<Record<string, string>>;
    readonly totalCount: number;
    readonly loading: boolean;
    readonly search: string;
    readonly page: number;
    readonly pageSize: number;
    readonly canEdit: boolean;
    readonly canDelete: boolean;
    readonly onSearchChange: (value: string) => void;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
    readonly onEdit: (credential: Credential) => void;
    readonly onDelete: (credential: Credential) => void;
}

export function CredentialsTable({
    credentials,
    ownerNames,
    totalCount,
    loading,
    search,
    page,
    pageSize,
    canEdit,
    canDelete,
    onSearchChange,
    onPageChange,
    onPageSizeChange,
    onEdit,
    onDelete,
}: CredentialsTableProps) {
    return (
        <DataTable
            aria-label="Credentials"
            columns={buildColumns({ ownerNames, canEdit, canDelete, onEdit, onDelete })}
            data={credentials}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                onPageChange,
                onPageSizeChange: size => {
                    onPageSizeChange(size);
                    onPageChange(1);
                },
            }}
            emptyMessage={
                search.trim() ? (
                    <DataTableEmptyState
                        variant="no-results"
                        icon={<SearchIcon className="size-8" aria-hidden />}
                        title="No credential matches your search"
                        description="Try adjusting your search terms."
                        action={
                            <Button size="sm" variant="outline" onClick={() => onSearchChange('')}>
                                Clear search
                            </Button>
                        }
                    />
                ) : (
                    <DataTableEmptyState
                        variant="no-results"
                        icon={<KeyRoundIcon className="size-8" aria-hidden />}
                        title="No credentials"
                        description="No credentials to display."
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
                            aria-label="Search credentials"
                            placeholder="Search by name…"
                            value={search}
                            onChange={e => onSearchChange(e.target.value)}
                        />
                    </InputGroup>
                </div>
            }
        />
    );
}
