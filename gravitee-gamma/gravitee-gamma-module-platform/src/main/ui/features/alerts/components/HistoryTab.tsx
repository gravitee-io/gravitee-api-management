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
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';

import { formatAbsoluteDateTime, formatRelativeDateTime } from '../../../shared/time';
import type { AlertHistoryPage } from '../types';

export interface HistoryTabProps {
    historyPage: AlertHistoryPage | undefined;
    onRefresh?: () => void;
    isRefreshing?: boolean;
    page?: number;
    pageSize?: number;
    onPageChange?: (page: number) => void;
    onPageSizeChange?: (pageSize: number) => void;
}

const HISTORY_PAGE_SIZE_OPTIONS = [10, 25, 50, 75, 100];

export function HistoryTab({
    historyPage,
    onRefresh,
    isRefreshing = false,
    page = 1,
    pageSize = 10,
    onPageChange,
    onPageSizeChange,
}: HistoryTabProps) {
    const totalCount = historyPage?.totalElements ?? 0;
    return (
        <div className="mt-6">
            <Card>
                <CardHeader>
                    <div className="flex items-start justify-between gap-4">
                        <div>
                            <CardTitle className="text-base">History</CardTitle>
                            <CardDescription>Events history for this alert</CardDescription>
                        </div>
                        {onRefresh && (
                            <Button type="button" variant="outline" size="sm" disabled={isRefreshing} onClick={onRefresh}>
                                {isRefreshing ? 'Refreshing…' : 'Refresh'}
                            </Button>
                        )}
                    </div>
                </CardHeader>
                <CardContent>
                    {historyPage && historyPage.content.length > 0 ? (
                        <TooltipProvider delayDuration={200}>
                            <div className="rounded-lg border">
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>Date</TableHead>
                                            <TableHead>Message</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {historyPage.content.map((evt, i) => (
                                            <TableRow key={`${evt.created_at}-${i}`}>
                                                <TableCell className="text-sm">
                                                    <Tooltip>
                                                        <TooltipTrigger asChild>
                                                            <span>{formatRelativeDateTime(evt.created_at)}</span>
                                                        </TooltipTrigger>
                                                        <TooltipContent>{formatAbsoluteDateTime(evt.created_at)}</TooltipContent>
                                                    </Tooltip>
                                                </TableCell>
                                                <TableCell className="text-sm text-muted-foreground">{evt.message}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </div>
                        </TooltipProvider>
                    ) : (
                        <div className="py-8 text-center text-sm text-muted-foreground">No data to display.</div>
                    )}
                    {onPageChange && onPageSizeChange && totalCount > 0 && (
                        <div className="mt-4">
                            <DataTablePagination
                                page={page}
                                pageSize={pageSize}
                                totalCount={totalCount}
                                pageSizeOptions={HISTORY_PAGE_SIZE_OPTIONS}
                                onPageChange={onPageChange}
                                onPageSizeChange={onPageSizeChange}
                            />
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
