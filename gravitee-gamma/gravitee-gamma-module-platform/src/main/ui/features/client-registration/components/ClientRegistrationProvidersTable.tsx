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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { formatApplicationDateTime } from '../../applications/utils/applicationFormatters';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import type { ClientRegistrationProvider } from '../types/clientRegistrationProvider';

function buildColumns({
    canDelete,
    onEdit,
    onDelete,
}: {
    canDelete: boolean;
    onEdit: (provider: ClientRegistrationProvider) => void;
    onDelete: (provider: ClientRegistrationProvider) => void;
}): DataTableProps<ClientRegistrationProvider>['columns'] {
    return [
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<ClientRegistrationProvider>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<ClientRegistrationProvider>) => <span className="text-sm font-medium">{row.original.name}</span>,
        },
        {
            id: 'description',
            accessorKey: 'description',
            header: ({ column }: ColHeader<ClientRegistrationProvider>) => <DataTableColumnHeader column={column} title="Description" />,
            cell: ({ row }: ColCell<ClientRegistrationProvider>) => (
                <span className="text-sm text-muted-foreground">{row.original.description?.trim() || '—'}</span>
            ),
        },
        {
            id: 'updated_at',
            accessorKey: 'updated_at',
            header: ({ column }: ColHeader<ClientRegistrationProvider>) => (
                <DataTableColumnHeader column={column} title="Last updated at" />
            ),
            cell: ({ row }: ColCell<ClientRegistrationProvider>) => (
                <span className="text-sm text-muted-foreground">{formatApplicationDateTime(row.original.updated_at)}</span>
            ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<ClientRegistrationProvider>) => (
                <div className="flex justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="size-8" aria-label="Provider actions">
                                <MoreHorizontalIcon className="size-4" aria-hidden />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="min-w-48">
                            <DropdownMenuItem className="whitespace-nowrap" onSelect={() => onEdit(row.original)}>
                                <PencilIcon className="mr-2 size-4 shrink-0" aria-hidden />
                                Edit
                            </DropdownMenuItem>
                            {canDelete ? (
                                <DropdownMenuItem
                                    className="whitespace-nowrap"
                                    variant="destructive"
                                    onSelect={() => onDelete(row.original)}
                                >
                                    <Trash2Icon className="mr-2 size-4 shrink-0" aria-hidden />
                                    Delete
                                </DropdownMenuItem>
                            ) : null}
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            ),
        },
    ];
}

export function ClientRegistrationProvidersTable({
    providers,
    canDelete,
    onEdit,
    onDelete,
}: Readonly<{
    providers: ClientRegistrationProvider[];
    canDelete: boolean;
    onEdit: (provider: ClientRegistrationProvider) => void;
    onDelete: (provider: ClientRegistrationProvider) => void;
}>) {
    const columns = useMemo(() => buildColumns({ canDelete, onEdit, onDelete }), [canDelete, onEdit, onDelete]);

    return <DataTable columns={columns} data={providers} emptyMessage="No DCR provider configured." />;
}
