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
    DataTablePagination,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Tooltip,
    TooltipContent,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleXIcon, ClockIcon, CopyIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { ApplicationSubscriptionApiKeyRow } from '../../types/applicationSubscription';
import { formatApplicationDateTime } from '../../utils/applicationFormatters';

const DEFAULT_PAGE_SIZE = 10;

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
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const sortedKeys = useMemo(() => [...apiKeys].sort((a, b) => Number(b.isValid) - Number(a.isValid)), [apiKeys]);
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
                        pageSizeOptions={[10, 25, 50, 100]}
                        onPageChange={setPage}
                        onPageSizeChange={handlePageSizeChange}
                    />
                </div>
                <Table aria-label="API keys table">
                    <TableHeader>
                        <TableRow>
                            <TableHead className="w-10" aria-label="Status" />
                            <TableHead>Key</TableHead>
                            <TableHead className="w-[200px]">Created at</TableHead>
                            <TableHead className="w-[200px]">Revoked/Expired at</TableHead>
                            <TableHead className="w-[180px] text-right">Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {isLoading ? (
                            Array.from({ length: pageSize }).map((_, index) => (
                                <TableRow key={index}>
                                    <TableCell colSpan={5} className="h-12">
                                        <Skeleton className="h-4 w-32" />
                                    </TableCell>
                                </TableRow>
                            ))
                        ) : paginatedKeys.length > 0 ? (
                            paginatedKeys.map(apiKey => (
                                <TableRow key={apiKey.id} className={apiKey.isValid ? undefined : 'text-muted-foreground'}>
                                    <TableCell className="w-10">
                                        <ApiKeyStatusIcon isValid={apiKey.isValid} />
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex items-center gap-2">
                                            <code
                                                className={`rounded px-2 py-1 font-mono text-sm ${
                                                    apiKey.isValid ? 'bg-muted' : 'bg-muted/50'
                                                }`}
                                            >
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
                                    </TableCell>
                                    <TableCell className="text-sm whitespace-nowrap">
                                        {formatApplicationDateTime(apiKey.createdAt)}
                                    </TableCell>
                                    <TableCell className="text-sm whitespace-nowrap">{formatApplicationDateTime(apiKey.endDate)}</TableCell>
                                    <TableCell className="text-right">
                                        {!readOnly && apiKey.isValid ? (
                                            <div className="flex justify-end gap-1">
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    size="sm"
                                                    className="text-destructive"
                                                    onClick={() => onRevoke(apiKey)}
                                                >
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
                                        ) : null}
                                    </TableCell>
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={5} className="h-16 text-center text-sm text-muted-foreground">
                                    No API keys
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>

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
                        pageSizeOptions={[10, 25, 50, 100]}
                        onPageChange={setPage}
                        onPageSizeChange={handlePageSizeChange}
                    />
                </div>
            </CardContent>
        </Card>
    );
}
