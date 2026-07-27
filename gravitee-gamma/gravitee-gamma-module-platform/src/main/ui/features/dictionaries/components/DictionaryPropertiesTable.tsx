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

import { Button, DataTable, type DataTableProps } from '@gravitee/graphene-core';
import { PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import type { ColCell } from '../../applications/utils/dataTableTypes';

export interface DictionaryPropertyRow {
    key: string;
    value: string;
}

function buildColumns({
    canEdit,
    isMutating,
    onEdit,
    onDelete,
}: {
    canEdit: boolean;
    isMutating: boolean;
    onEdit: (property: DictionaryPropertyRow) => void;
    onDelete: (propertyKey: string) => void;
}): DataTableProps<DictionaryPropertyRow>['columns'] {
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
            cell: ({ row }: ColCell<DictionaryPropertyRow>) => <span className="text-sm">{row.original.value}</span>,
            enableSorting: false,
        },
    ];

    if (canEdit) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 88,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<DictionaryPropertyRow>) => {
                const property = row.original;
                return (
                    <div className="flex items-center justify-end gap-1">
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-8"
                            aria-label={`Edit property ${property.key}`}
                            disabled={isMutating}
                            onClick={() => onEdit(property)}
                        >
                            <PencilIcon className="size-4 text-muted-foreground" aria-hidden />
                        </Button>
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-8 text-destructive hover:text-destructive"
                            aria-label={`Delete property ${property.key}`}
                            disabled={isMutating}
                            onClick={() => onDelete(property.key)}
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

export function DictionaryPropertiesTable({
    properties,
    canEdit,
    isMutating,
    onEdit,
    onDelete,
    emptyMessage,
}: Readonly<{
    properties: DictionaryPropertyRow[];
    canEdit: boolean;
    isMutating: boolean;
    onEdit: (property: DictionaryPropertyRow) => void;
    onDelete: (propertyKey: string) => void;
    emptyMessage: string;
}>) {
    const columns = useMemo(() => buildColumns({ canEdit, isMutating, onEdit, onDelete }), [canEdit, isMutating, onEdit, onDelete]);

    return <DataTable aria-label="Dictionary properties" columns={columns} data={properties} emptyMessage={emptyMessage} />;
}
