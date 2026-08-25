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
    Checkbox,
    DataTable,
    DataTableColumnHeader,
    DataTableEmptyState,
    DateCell,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { ClockIcon, Code2Icon, EyeIcon } from '@gravitee/graphene-core/icons';

import { SharedPolicyGroupStatusBadge } from './SharedPolicyGroupStatusBadge';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

interface SharedPolicyGroupHistoryTableProps {
    readonly histories: SharedPolicyGroup[];
    readonly totalCount: number;
    readonly loading: boolean;
    readonly selected: SharedPolicyGroup[];
    readonly page: number;
    readonly pageSize: number;
    readonly sorting: TableSortingState;
    readonly onToggleSelected: (sharedPolicyGroup: SharedPolicyGroup) => void;
    readonly onShowJson: (sharedPolicyGroup: SharedPolicyGroup) => void;
    readonly onShowDetails: (sharedPolicyGroup: SharedPolicyGroup) => void;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (pageSize: number) => void;
    readonly onSortingChange: (updater: TableSortingState | ((previous: TableSortingState) => TableSortingState)) => void;
}

function historyKey(sharedPolicyGroup: SharedPolicyGroup): string {
    return `${sharedPolicyGroup.version ?? 'unknown'}-${sharedPolicyGroup.updatedAt ?? sharedPolicyGroup.deployedAt ?? ''}`;
}

function buildColumns({
    selected,
    onToggleSelected,
    onShowJson,
    onShowDetails,
}: Pick<
    SharedPolicyGroupHistoryTableProps,
    'selected' | 'onToggleSelected' | 'onShowJson' | 'onShowDetails'
>): DataTableProps<SharedPolicyGroup>['columns'] {
    const selectedKeys = new Set(selected.map(historyKey));
    return [
        {
            id: 'selection',
            header: () => <span className="sr-only">Select versions to compare</span>,
            enableSorting: false,
            size: 48,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => {
                const item = row.original;
                const checked = selectedKeys.has(historyKey(item));
                return (
                    <Checkbox
                        checked={checked}
                        disabled={!checked && selected.length >= 2}
                        aria-label={`Select version ${item.version ?? 'unknown'}`}
                        onCheckedChange={() => onToggleSelected(item)}
                    />
                );
            },
        },
        {
            id: 'version',
            accessorKey: 'version',
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Version" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => <span className="text-sm">{row.original.version ?? '—'}</span>,
        },
        {
            id: 'name',
            accessorKey: 'name',
            enableSorting: false,
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => (
                <div className="flex items-start justify-between gap-3">
                    <div className="flex flex-col gap-1">
                        <span className="text-sm font-medium">{row.original.name}</span>
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
            accessorKey: 'deployedAt',
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Deployed At" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) =>
                row.original.deployedAt ? (
                    <DateCell value={new Date(row.original.deployedAt)} format="absolute" />
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            enableSorting: false,
            size: 96,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => (
                <TooltipProvider delayDuration={200}>
                    <div className="flex justify-end gap-1">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    type="button"
                                    size="icon"
                                    variant="ghost"
                                    aria-label={`Show JSON for version ${row.original.version ?? 'unknown'}`}
                                    onClick={() => onShowJson(row.original)}
                                >
                                    <Code2Icon className="size-4" aria-hidden />
                                </Button>
                            </TooltipTrigger>
                            <TooltipContent>Show JSON source</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    type="button"
                                    size="icon"
                                    variant="ghost"
                                    aria-label={`View or restore version ${row.original.version ?? 'unknown'}`}
                                    onClick={() => onShowDetails(row.original)}
                                >
                                    <EyeIcon className="size-4" aria-hidden />
                                </Button>
                            </TooltipTrigger>
                            <TooltipContent>Show details or restore version</TooltipContent>
                        </Tooltip>
                    </div>
                </TooltipProvider>
            ),
        },
    ];
}

export function SharedPolicyGroupHistoryTable({
    histories,
    totalCount,
    loading,
    selected,
    page,
    pageSize,
    sorting,
    onToggleSelected,
    onShowJson,
    onShowDetails,
    onPageChange,
    onPageSizeChange,
    onSortingChange,
}: SharedPolicyGroupHistoryTableProps) {
    return (
        <DataTable
            aria-label="Shared Policy Group history"
            columns={buildColumns({ selected, onToggleSelected, onShowJson, onShowDetails })}
            data={histories}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            sorting={sorting}
            onSortingChange={onSortingChange}
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
                <DataTableEmptyState
                    variant="first-use"
                    icon={<ClockIcon className="size-8" aria-hidden />}
                    title="No deployed versions yet"
                    description="Deploy this Shared Policy Group to create its first version."
                />
            }
        />
    );
}
