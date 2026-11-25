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
    Alert,
    AlertDescription,
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
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleXIcon, CopyIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { ApplicationSharedApiKeyRenewDialog } from './ApplicationSharedApiKeyRenewDialog';
import { ApplicationSubscriptionRevokeApiKeyDialog } from './ApplicationSubscriptionRevokeApiKeyDialog';
import { copyTextToClipboardWithNotifyHandler } from '../../../../shared/copyToClipboard';
import type { ApiKeyMode } from '../../types/application';
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
                return (Number(left.isValid) - Number(right.isValid)) * direction;
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

function settle(result: void | Promise<void>, onSuccess: () => void) {
    if (result instanceof Promise) {
        void result.then(onSuccess, () => undefined);
        return;
    }
    onSuccess();
}

export function ApplicationSharedApiKeysView({
    apiKeyMode,
    apiKeys,
    isLoading,
    isError,
    canManage,
    canUseCustomApiKey,
    renewPending,
    revokePending,
    onRenew,
    onRevoke,
}: Readonly<{
    apiKeyMode?: ApiKeyMode;
    apiKeys: ApplicationSubscriptionApiKeyRow[];
    isLoading: boolean;
    isError: boolean;
    canManage: boolean;
    canUseCustomApiKey: boolean;
    renewPending: boolean;
    revokePending: boolean;
    onRenew: (customApiKey?: string) => void | Promise<void>;
    onRevoke: (apiKeyId: string) => void | Promise<void>;
}>) {
    const [renewOpen, setRenewOpen] = useState(false);
    const [revokeTarget, setRevokeTarget] = useState<ApplicationSubscriptionApiKeyRow | null>(null);
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
        const maxPage = Math.max(1, Math.ceil(totalCount / pageSize));
        if (page > maxPage) {
            setPage(1);
        }
    }, [page, pageSize, totalCount]);

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
                accessorKey: 'key',
                header: 'Key',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ApplicationSubscriptionApiKeyRow>) => {
                    const apiKey = row.original;
                    return (
                        <div className="flex items-center gap-2">
                            <code className="max-w-xs truncate rounded bg-muted px-2 py-1 font-mono text-sm">{apiKey.key}</code>
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
                    if (!canManage || !apiKey.isValid) return null;
                    return (
                        <div className="flex justify-end">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        className="size-8"
                                        aria-label="Revoke"
                                        onClick={() => setRevokeTarget(apiKey)}
                                    >
                                        <CircleXIcon className="size-4" aria-hidden />
                                    </Button>
                                </TooltipTrigger>
                                <TooltipContent>Revoke</TooltipContent>
                            </Tooltip>
                        </div>
                    );
                },
                enableSorting: false,
                enableHiding: false,
            },
        ];
    }, [canManage]);

    if (apiKeyMode !== 'SHARED') {
        return (
            <p className="text-sm text-muted-foreground">Shared API keys are available only for an application that uses shared mode.</p>
        );
    }

    return (
        <TooltipProvider delayDuration={200}>
            <div className="space-y-6">
                <div>
                    <h1 className="text-2xl font-semibold">Shared API Keys</h1>
                    <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
                        These API keys are shared with all application subscriptions with an API_KEY plan.
                    </p>
                </div>

                {isError ? (
                    <Alert variant="destructive">
                        <AlertDescription>Unable to load API keys. Please try again.</AlertDescription>
                    </Alert>
                ) : null}

                <Card>
                    <CardHeader className="flex flex-row items-center justify-between pb-2">
                        <div className="space-y-1">
                            <CardTitle className="text-base">API Keys</CardTitle>
                            <CardDescription>
                                The previous key stays valid for two hours after a renewal. You can revoke it from the list.
                            </CardDescription>
                        </div>
                        {canManage ? (
                            <Button type="button" variant="outline" size="sm" disabled={renewPending} onClick={() => setRenewOpen(true)}>
                                <RefreshCwIcon className="size-3.5" aria-hidden />
                                {renewPending ? 'Renewing…' : 'Renew'}
                            </Button>
                        ) : null}
                    </CardHeader>
                    <CardContent className="p-6 pt-0">
                        <DataTable
                            aria-label="Shared API keys"
                            columns={apiKeyColumns}
                            data={isError ? [] : paginatedKeys}
                            sorting={sorting}
                            onSortingChange={updater => {
                                setSorting(previous => (typeof updater === 'function' ? updater(previous) : updater));
                                setPage(1);
                            }}
                            serverSide
                            loading={isLoading}
                            skeletonCount={pageSize}
                            pagination={{
                                page,
                                pageSize,
                                totalCount: isError ? 0 : totalCount,
                                pageSizeOptions: SUBSCRIPTION_PAGE_SIZE_OPTIONS,
                                onPageChange: setPage,
                                onPageSizeChange: size => {
                                    setPageSize(size);
                                    setPage(1);
                                },
                            }}
                            emptyMessage={isError ? '' : 'No API keys'}
                        />
                    </CardContent>
                </Card>

                <ApplicationSharedApiKeyRenewDialog
                    open={renewOpen}
                    canUseCustomApiKey={canUseCustomApiKey}
                    pending={renewPending}
                    onClose={() => setRenewOpen(false)}
                    onConfirm={customApiKey => {
                        settle(onRenew(customApiKey), () => setRenewOpen(false));
                    }}
                />

                <ApplicationSubscriptionRevokeApiKeyDialog
                    apiKey={revokeTarget}
                    onClose={() => setRevokeTarget(null)}
                    onConfirm={() => {
                        if (!revokeTarget) return;
                        settle(onRevoke(revokeTarget.id), () => setRevokeTarget(null));
                    }}
                    isLoading={revokePending}
                />
            </div>
        </TooltipProvider>
    );
}
