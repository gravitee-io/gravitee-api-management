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
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    DataTable,
    DataTableColumnHeader,
    DataTablePagination,
    type DataTableProps,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleXIcon, ClockIcon, CopyIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { ApplicationSubscriptionApiKeyRow } from '../../types/applicationSubscription';
import { formatApplicationDateTime } from '../../utils/applicationFormatters';
import { DEFAULT_SUBSCRIPTION_PAGE_SIZE, SUBSCRIPTION_PAGE_SIZE_OPTIONS } from '../../utils/applicationSubscriptionConstants';
import { SUBSCRIPTION_API_KEY_SORTABLE_IDS } from '../../utils/applicationTableSortParity';
import { NON_SORTABLE_COLUMN } from '../../utils/dataTableHeaders';
import type { ColCell, ColHeader } from '../../utils/dataTableTypes';
import type { TableSortingState } from '../../utils/tableSort';
import { toSortableTimestamp } from '../../utils/tableSort';

/** Client-side sort on the full key list (console: gioTableFilterCollection), then paginate. */
function sortApiKeys(keys: ApplicationSubscriptionApiKeyRow[], sorting: TableSortingState): ApplicationSubscriptionApiKeyRow[] {
    const active = sorting[0];
    if (!active?.id || !SUBSCRIPTION_API_KEY_SORTABLE_IDS.has(active.id)) return keys;

    const direction = active.desc ? -1 : 1;
    return [...keys].sort((left, right) => {
        switch (active.id) {
            case 'isValid':
                return (Number(right.isValid) - Number(left.isValid)) * direction;
            case 'createdAt':
                return (toSortableTimestamp(left.createdAt) - toSortableTimestamp(right.createdAt)) * direction;
            case 'endDate':
                return (toSortableTimestamp(left.endDate) - toSortableTimestamp(right.endDate)) * direction;
            default:
                return 0;
        }
    });
}

function ApiKeyStatusIcon({ isValid }: Readonly<{ isValid: boolean }>) {
    const label = isValid ? 'Valid' : 'Revoked or Expired';
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className="inline-flex cursor-default" aria-label={label}>
                    {isValid ? (
                        <CircleCheckIcon className="size-4 text-success" aria-hidden />
                    ) : (
                        <CircleXIcon className="size-4 text-destructive" aria-hidden />
                    )}
                </span>
            </TooltipTrigger>
            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );
}

export function ApplicationSubscriptionApiKeysCard({
    apiKeys,
    isLoading,
    readOnly,
    renewPending,
    expireAvailable,
    onRenew,
    onRevoke,
    onExpire,
}: Readonly<{
    apiKeys: ApplicationSubscriptionApiKeyRow[];
    isLoading: boolean;
    readOnly: boolean;
    renewPending: boolean;
    /** When false, expire uses Management API v2 parent resolution — hide or disable if unknown. */
    expireAvailable: boolean;
    onRenew: () => void;
    onRevoke: (apiKey: ApplicationSubscriptionApiKeyRow) => void;
    onExpire: (apiKey: ApplicationSubscriptionApiKeyRow) => void;
}>) {
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_SUBSCRIPTION_PAGE_SIZE);
    const [sorting, setSorting] = useState<TableSortingState>([{ id: 'isValid', desc: true }]);

    const sortedKeys = useMemo(() => sortApiKeys(apiKeys, sorting), [apiKeys, sorting]);
    const totalCount = sortedKeys.length;
    const paginatedKeys = useMemo(() => {
        const start = (page - 1) * pageSize;
        return sortedKeys.slice(start, start + pageSize);
    }, [sortedKeys, page, pageSize]);

    useEffect(() => {
        setPage(1);
    }, [apiKeys]);

    useEffect(() => {
        const maxPage = Math.max(1, Math.ceil(totalCount / pageSize));
        if (page > maxPage) {
            setPage(1);
        }
    }, [page, pageSize, totalCount]);

    const handlePageSizeChange = (size: number) => {
        setPageSize(size);
        setPage(1);
    };

    const handleSortingChange = (updater: TableSortingState | ((prev: TableSortingState) => TableSortingState)) => {
        setSorting(previous => (typeof updater === 'function' ? updater(previous) : updater));
        setPage(1);
    };

    const apiKeyColumns = useMemo((): DataTableProps<ApplicationSubscriptionApiKeyRow>['columns'] => {
        return [
            {
                id: 'isValid',
                accessorFn: (row: ApplicationSubscriptionApiKeyRow) => Number(row.isValid),
                header: () => <span className="sr-only">Status</span>,
                cell: ({ row }: ColCell<ApplicationSubscriptionApiKeyRow>) => <ApiKeyStatusIcon isValid={row.original.isValid} />,
                enableSorting: true,
            },
            {
                id: 'key',
                accessorKey: 'maskedKey',
                header: 'Key',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ApplicationSubscriptionApiKeyRow>) => {
                    const apiKey = row.original;
                    return (
                        <div className="flex items-center gap-2">
                            <code className={`rounded px-2 py-1 font-mono text-sm ${apiKey.isValid ? 'bg-muted' : 'bg-muted/50'}`}>
                                {apiKey.maskedKey}
                            </code>
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="size-7 shrink-0"
                                aria-label="Copy API key"
                                onClick={() => void navigator.clipboard?.writeText(apiKey.key)}
                            >
                                <CopyIcon className="size-3.5" aria-hidden />
                            </Button>
                        </div>
                    );
                },
            },
            {
                id: 'createdAt',
                accessorFn: (row: ApplicationSubscriptionApiKeyRow) => toSortableTimestamp(row.createdAt),
                header: ({ column }: ColHeader<ApplicationSubscriptionApiKeyRow>) => (
                    <DataTableColumnHeader column={column} title="Created at" />
                ),
                cell: ({ row }: ColCell<ApplicationSubscriptionApiKeyRow>) => (
                    <span className="whitespace-nowrap text-sm">{formatApplicationDateTime(row.original.createdAt)}</span>
                ),
            },
            {
                id: 'endDate',
                accessorFn: (row: ApplicationSubscriptionApiKeyRow) => toSortableTimestamp(row.endDate),
                header: ({ column }: ColHeader<ApplicationSubscriptionApiKeyRow>) => (
                    <DataTableColumnHeader column={column} title="Revoked/Expired at" />
                ),
                cell: ({ row }: ColCell<ApplicationSubscriptionApiKeyRow>) => (
                    <span className="whitespace-nowrap text-sm">{formatApplicationDateTime(row.original.endDate)}</span>
                ),
            },
            {
                id: 'actions',
                header: () => <div className="text-right">Actions</div>,
                cell: ({ row }: ColCell<ApplicationSubscriptionApiKeyRow>) => {
                    const apiKey = row.original;
                    return !readOnly && apiKey.isValid ? (
                        <div className="flex justify-end gap-1">
                            <Button type="button" variant="outline" size="sm" className="text-destructive" onClick={() => onRevoke(apiKey)}>
                                Revoke
                            </Button>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                disabled={!expireAvailable}
                                onClick={() => onExpire(apiKey)}
                                title={!expireAvailable ? 'Cannot resolve API for this subscription.' : undefined}
                            >
                                <ClockIcon className="size-3.5" aria-hidden />
                                Expire
                            </Button>
                        </div>
                    ) : null;
                },
                enableSorting: false,
                enableHiding: false,
            },
        ];
    }, [expireAvailable, onExpire, onRevoke, readOnly]);

    return (
        <Card>
            <CardHeader className="pb-3">
                <CardTitle className="text-base">API Keys</CardTitle>
                <CardDescription>Keys issued for this subscription. Renew, expire, or revoke as needed.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 p-6 pt-0">
                <div className="flex justify-end">
                    <DataTablePagination
                        page={page}
                        pageSize={pageSize}
                        totalCount={totalCount}
                        pageSizeOptions={SUBSCRIPTION_PAGE_SIZE_OPTIONS}
                        onPageChange={setPage}
                        onPageSizeChange={handlePageSizeChange}
                    />
                </div>
                <DataTable
                    columns={apiKeyColumns}
                    data={paginatedKeys}
                    sorting={sorting}
                    onSortingChange={handleSortingChange}
                    loading={isLoading}
                    skeletonCount={pageSize}
                    emptyMessage="No API keys"
                />

                <div className="flex flex-wrap items-center justify-between gap-4">
                    {!readOnly ? (
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            disabled={renewPending || !sortedKeys.some(k => k.isValid)}
                            onClick={onRenew}
                        >
                            <RefreshCwIcon className="size-3.5" aria-hidden />
                            Renew
                        </Button>
                    ) : (
                        <span />
                    )}
                    <DataTablePagination
                        page={page}
                        pageSize={pageSize}
                        totalCount={totalCount}
                        pageSizeOptions={SUBSCRIPTION_PAGE_SIZE_OPTIONS}
                        onPageChange={setPage}
                        onPageSizeChange={handlePageSizeChange}
                    />
                </div>
            </CardContent>
        </Card>
    );
}
