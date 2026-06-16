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
    type DataTableProps,
    DateCell,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleXIcon, ClockIcon, CopyIcon, MoreVerticalIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { copyTextToClipboardWithNotifyHandler } from '../../../../shared/copyToClipboard';
import type { ApplicationSubscriptionApiKeyRow } from '../../types/applicationSubscription';
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
                                onClick={() => copyTextToClipboardWithNotifyHandler(apiKey.key, 'API Key has been copied to clipboard')}
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
                cell: ({ row }: ColCell<ApplicationSubscriptionApiKeyRow>) =>
                    row.original.createdAt ? (
                        <DateCell value={new Date(row.original.createdAt)} format="absolute" />
                    ) : (
                        <span className="text-sm text-muted-foreground">—</span>
                    ),
            },
            {
                id: 'endDate',
                accessorFn: (row: ApplicationSubscriptionApiKeyRow) => toSortableTimestamp(row.endDate),
                header: ({ column }: ColHeader<ApplicationSubscriptionApiKeyRow>) => (
                    <DataTableColumnHeader column={column} title="Revoked/Expired at" />
                ),
                cell: ({ row }: ColCell<ApplicationSubscriptionApiKeyRow>) =>
                    row.original.endDate ? (
                        <DateCell value={new Date(row.original.endDate)} format="absolute" />
                    ) : (
                        <span className="text-sm text-muted-foreground">—</span>
                    ),
            },
            {
                id: 'actions',
                header: () => <span className="sr-only">Actions</span>,
                size: 56,
                cell: ({ row }: ColCell<ApplicationSubscriptionApiKeyRow>) => {
                    const apiKey = row.original;
                    if (readOnly || !apiKey.isValid) return null;
                    return (
                        <div className="flex justify-end">
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button type="button" variant="ghost" size="icon" className="size-8" aria-label="API key actions">
                                        <MoreVerticalIcon className="size-4" aria-hidden />
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end" className="min-w-48">
                                    <DropdownMenuItem className="text-destructive" onSelect={() => onRevoke(apiKey)}>
                                        Revoke
                                    </DropdownMenuItem>
                                    <DropdownMenuItem disabled={!expireAvailable} onSelect={() => onExpire(apiKey)}>
                                        <ClockIcon className="size-4" aria-hidden />
                                        Expire
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        </div>
                    );
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
                <DataTable
                    aria-label="Subscription API keys"
                    columns={apiKeyColumns}
                    data={paginatedKeys}
                    sorting={sorting}
                    onSortingChange={handleSortingChange}
                    serverSide
                    loading={isLoading}
                    skeletonCount={pageSize}
                    pagination={{
                        page,
                        pageSize,
                        totalCount,
                        pageSizeOptions: SUBSCRIPTION_PAGE_SIZE_OPTIONS,
                        onPageChange: setPage,
                        onPageSizeChange: handlePageSizeChange,
                    }}
                    emptyMessage="No API keys"
                />

                {!readOnly ? (
                    <div className="flex">
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
                    </div>
                ) : null}
            </CardContent>
        </Card>
    );
}
