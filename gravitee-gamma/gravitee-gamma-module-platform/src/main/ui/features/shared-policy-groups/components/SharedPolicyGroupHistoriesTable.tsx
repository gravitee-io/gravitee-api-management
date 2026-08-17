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
    DataTableEmptyState,
    DateCell,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { ClockIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { SharedPolicyGroupStatusBadge } from './SharedPolicyGroupStatusBadge';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';
import { toSharedPolicyGroupHistoryRowId } from '../utils/sharedPolicyGroupHistoriesSort';

function buildColumns(): DataTableProps<SharedPolicyGroup>['columns'] {
    return [
        {
            id: 'version',
            accessorKey: 'version',
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Version" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => (
                <span className="text-sm tabular-nums">{row.original.version ?? '—'}</span>
            ),
        },
        {
            id: 'name',
            accessorKey: 'name',
            enableSorting: false,
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => (
                <div className="flex items-start gap-3 text-left">
                    <div className="min-w-0 flex-1 flex flex-col items-start gap-1">
                        <span className="text-sm font-medium text-foreground">{row.original.name}</span>
                        {row.original.description ? (
                            <span className="text-xs text-muted-foreground">{row.original.description}</span>
                        ) : null}
                    </div>
                    <SharedPolicyGroupStatusBadge lifecycleState={row.original.lifecycleState} />
                </div>
            ),
        },
        {
            id: 'deployedAt',
            accessorFn: (row: SharedPolicyGroup) => row.deployedAt ?? '',
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Deployed At" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) =>
                row.original.deployedAt ? (
                    <DateCell value={new Date(row.original.deployedAt)} format="absolute" />
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
    ];
}

interface SharedPolicyGroupHistoriesTableProps {
    readonly histories: SharedPolicyGroup[];
    readonly totalCount: number;
    readonly loading: boolean;
    readonly page: number;
    readonly pageSize: number;
    readonly sorting: TableSortingState;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
    readonly onSortingChange: (updater: TableSortingState | ((previous: TableSortingState) => TableSortingState)) => void;
}

export function SharedPolicyGroupHistoriesTable({
    histories,
    totalCount,
    loading,
    page,
    pageSize,
    sorting,
    onPageChange,
    onPageSizeChange,
    onSortingChange,
}: SharedPolicyGroupHistoriesTableProps) {
    const columns = useMemo(() => buildColumns(), []);

    if (!loading && totalCount === 0) {
        return (
            <div className="rounded-lg border" data-testid="shared-policy-group-history-empty">
                <DataTableEmptyState
                    variant="first-use"
                    icon={<ClockIcon className="size-8" aria-hidden />}
                    title="No version history yet"
                    description="Deploy this Shared Policy Group to start recording versions."
                />
            </div>
        );
    }

    return (
        <DataTable
            aria-label="Shared Policy Group history"
            columns={columns}
            data={histories}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            sorting={sorting}
            onSortingChange={onSortingChange}
            tableOptions={{
                getRowId: row => toSharedPolicyGroupHistoryRowId(row),
            }}
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                onPageChange,
                onPageSizeChange,
            }}
        />
    );
}
