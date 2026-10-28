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
    type DataTableProps,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@gravitee/graphene-core';
import { LockIcon, MoreVerticalIcon, RefreshCwIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { ColCell } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { DictionaryPropertyRow } from '../types/dictionary';

const DEFAULT_PAGE_SIZE = 10;
const MASK = '•'.repeat(8);

function paginateProperties(rows: DictionaryPropertyRow[], page: number, pageSize: number): DictionaryPropertyRow[] {
    const start = (page - 1) * pageSize;
    return rows.slice(start, start + pageSize);
}

interface ColumnOptions {
    canEdit: boolean;
    isMutating: boolean;
    onEdit: (property: DictionaryPropertyRow) => void;
    onRenew: (property: DictionaryPropertyRow) => void;
    onEncrypt: (propertyKey: string) => void;
    onDelete: (propertyKey: string) => void;
}

function PropertyActions({
    property,
    isMutating,
    onEdit,
    onRenew,
    onEncrypt,
    onDelete,
}: Readonly<Omit<ColumnOptions, 'canEdit'> & { property: DictionaryPropertyRow }>) {
    return (
        <div className="flex justify-end">
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8"
                        aria-label={`Actions for ${property.key}`}
                        disabled={isMutating}
                    >
                        <MoreVerticalIcon className="size-4" aria-hidden />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="min-w-48 whitespace-nowrap">
                    {property.encrypted ? (
                        <DropdownMenuItem onSelect={() => onRenew(property)}>
                            <RefreshCwIcon className="size-4" aria-hidden />
                            Renew encryption
                        </DropdownMenuItem>
                    ) : (
                        <>
                            <DropdownMenuItem onSelect={() => onEdit(property)}>Edit value</DropdownMenuItem>
                            <DropdownMenuItem onSelect={() => onEncrypt(property.key)}>
                                <LockIcon className="size-4" aria-hidden />
                                Encrypt value
                            </DropdownMenuItem>
                        </>
                    )}
                    <DropdownMenuSeparator />
                    <DropdownMenuItem className="text-destructive" onSelect={() => onDelete(property.key)}>
                        <Trash2Icon className="size-4" aria-hidden />
                        Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}

function buildColumns({ canEdit, ...actions }: ColumnOptions): DataTableProps<DictionaryPropertyRow>['columns'] {
    const columns: DataTableProps<DictionaryPropertyRow>['columns'] = [
        {
            id: 'key',
            accessorKey: 'key',
            header: 'Key',
            cell: ({ row }: ColCell<DictionaryPropertyRow>) => <span className="font-mono text-xs">{row.original.key}</span>,
            enableSorting: false,
        },
        {
            id: 'value',
            accessorKey: 'value',
            header: 'Value',
            cell: ({ row }: ColCell<DictionaryPropertyRow>) =>
                row.original.encrypted ? (
                    <span className="font-mono text-sm text-muted-foreground">{MASK}</span>
                ) : (
                    <span className="text-sm">{row.original.value}</span>
                ),
            enableSorting: false,
        },
        {
            id: 'characteristics',
            header: 'Characteristics',
            enableSorting: false,
            cell: ({ row }: ColCell<DictionaryPropertyRow>) => (
                <div className="flex flex-wrap gap-1">
                    {row.original.encrypted ? (
                        <Badge className="gap-1">
                            <LockIcon className="size-3" aria-hidden />
                            Encrypted
                        </Badge>
                    ) : (
                        <Badge variant="outline">Unencrypted</Badge>
                    )}
                </div>
            ),
        },
    ];

    if (canEdit) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 64,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<DictionaryPropertyRow>) => <PropertyActions property={row.original} {...actions} />,
        });
    }

    return columns;
}

export function DictionaryPropertiesTable({
    properties,
    canEdit,
    isMutating,
    onEdit,
    onRenew,
    onEncrypt,
    onDelete,
    emptyMessage,
}: Readonly<
    ColumnOptions & {
        properties: DictionaryPropertyRow[];
        emptyMessage: string;
    }
>) {
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const totalCount = properties.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
    const currentPage = Math.min(page, totalPages);
    const paginatedData = useMemo(() => paginateProperties(properties, currentPage, pageSize), [properties, currentPage, pageSize]);

    const columns = useMemo(
        () => buildColumns({ canEdit, isMutating, onEdit, onRenew, onEncrypt, onDelete }),
        [canEdit, isMutating, onEdit, onRenew, onEncrypt, onDelete],
    );

    useEffect(() => {
        if (page > totalPages) {
            setPage(totalPages);
        }
    }, [page, totalPages]);

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    return (
        <DataTable
            aria-label="Dictionary properties"
            columns={columns}
            data={paginatedData}
            serverSide
            pagination={{
                page: currentPage,
                pageSize,
                totalCount,
                pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                onPageChange: setPage,
                onPageSizeChange: handlePageSizeChange,
            }}
            emptyMessage={emptyMessage}
        />
    );
}
