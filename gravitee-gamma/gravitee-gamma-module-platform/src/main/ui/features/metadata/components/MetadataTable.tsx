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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Input,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { MetadataFormatBadge } from './MetadataFormatBadge';
import type { Metadata } from '../types/metadata';

const NON_SORTABLE = { enableSorting: false } as const;

type ColCell<T> = { row: { original: T } };

function buildColumns({
    canWrite,
    onEdit,
    onDelete,
}: {
    canWrite: boolean;
    onEdit: (metadata: Metadata) => void;
    onDelete: (metadata: Metadata) => void;
}): DataTableProps<Metadata>['columns'] {
    const columns: DataTableProps<Metadata>['columns'] = [
        {
            id: 'key',
            accessorKey: 'key',
            header: 'Key',
            ...NON_SORTABLE,
            cell: ({ row }: ColCell<Metadata>) => (
                <span className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded">{row.original.key}</span>
            ),
        },
        {
            id: 'name',
            accessorKey: 'name',
            header: 'Name',
            ...NON_SORTABLE,
            cell: ({ row }: ColCell<Metadata>) => <span className="text-sm font-medium">{row.original.name}</span>,
        },
        {
            id: 'format',
            accessorKey: 'format',
            header: 'Format',
            ...NON_SORTABLE,
            cell: ({ row }: ColCell<Metadata>) => <MetadataFormatBadge format={row.original.format} />,
        },
        {
            id: 'value',
            accessorKey: 'value',
            header: 'Default Value',
            ...NON_SORTABLE,
            cell: ({ row }: ColCell<Metadata>) =>
                row.original.value ? (
                    <span className="text-sm">{row.original.value}</span>
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
    ];

    if (canWrite) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<Metadata>) => (
                <div className="flex justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="size-8" aria-label="Metadata actions">
                                <MoreHorizontalIcon className="size-4" aria-hidden />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onSelect={() => onEdit(row.original)}>
                                <PencilIcon className="size-4 mr-2" aria-hidden />
                                Edit
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem variant="destructive" onSelect={() => onDelete(row.original)}>
                                <Trash2Icon className="size-4 mr-2" aria-hidden />
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            ),
        });
    }

    return columns;
}

export function MetadataTable({
    metadata,
    canWrite,
    onEdit,
    onDelete,
}: Readonly<{
    metadata: Metadata[];
    canWrite: boolean;
    onEdit: (metadata: Metadata) => void;
    onDelete: (metadata: Metadata) => void;
}>) {
    const [search, setSearch] = useState('');

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query) return metadata;
        return metadata.filter(m => m.key.toLowerCase().includes(query) || m.name.toLowerCase().includes(query));
    }, [metadata, search]);

    const columns = useMemo(() => buildColumns({ canWrite, onEdit, onDelete }), [canWrite, onEdit, onDelete]);

    return (
        <div className="space-y-3">
            <div className="relative max-w-sm">
                <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                <Input className="pl-10" placeholder="Search by key or name…" value={search} onChange={e => setSearch(e.target.value)} />
            </div>

            <DataTable
                columns={columns}
                data={filtered}
                emptyMessage={search.trim() ? 'No metadata matches your search.' : 'No global metadata defined for this environment.'}
            />
        </div>
    );
}
