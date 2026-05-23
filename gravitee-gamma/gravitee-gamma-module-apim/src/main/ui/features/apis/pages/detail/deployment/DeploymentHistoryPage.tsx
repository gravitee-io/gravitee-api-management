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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Badge,
    Button,
    Card,
    Checkbox,
    DataTablePagination,
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Skeleton,
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
import { EyeIcon, GitBranchIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { DiffDialog } from './DiffDialog';
import { SingleEventDialog } from './SingleEventDialog';
import { formatDate } from './utils';
import { useApiEvents } from '../../../hooks/useApiEvents';
import { rollbackApi } from '../../../services/apis';
import type { ApiEvent } from '../../../types';
import { apiEventsKeys } from '../../../utils/queryKeys';

const PAGE_SIZE_OPTIONS = [10, 20, 25, 50, 100];

export function DeploymentHistoryPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [selectedEvents, setSelectedEvents] = useState<ApiEvent[]>([]);
    const [singleViewEvent, setSingleViewEvent] = useState<ApiEvent | null>(null);
    const [isRollingBack, setIsRollingBack] = useState(false);
    const [rollbackError, setRollbackError] = useState<string | null>(null);

    const { data, isLoading } = useApiEvents(apiId, page, pageSize);
    const events = useMemo(() => data?.data ?? [], [data]);
    const totalCount = data?.pagination.totalCount ?? 0;
    const liveEvent = useMemo(() => (page === 1 && events.length > 0 ? events[0] : null), [page, events]);

    const showDiff = selectedEvents.length === 2;
    const selectionCount = selectedEvents.length;

    const toggleSelect = useCallback((event: ApiEvent) => {
        setSelectedEvents(prev => {
            if (prev.some(e => e.id === event.id)) return prev.filter(e => e.id !== event.id);
            if (prev.length >= 2) return [prev[1], event];
            return [...prev, event];
        });
    }, []);

    const handleCompareWithLive = useCallback(
        (event: ApiEvent) => {
            if (!liveEvent || liveEvent.id === event.id) return;
            setSelectedEvents([liveEvent, event]);
        },
        [liveEvent],
    );

    const handleDiffClose = useCallback(() => setSelectedEvents([]), []);

    const handleRollback = useCallback(
        async (eventId: string) => {
            if (!apiId) return;
            setIsRollingBack(true);
            setRollbackError(null);
            try {
                await rollbackApi(env!.id, apiId, eventId);
                setSelectedEvents([]);
                setSingleViewEvent(null);
                await queryClient.invalidateQueries({ queryKey: apiEventsKeys.all });
            } catch (e) {
                setRollbackError(e instanceof Error ? e.message : 'Rollback failed.');
            } finally {
                setIsRollingBack(false);
            }
        },
        [apiId, env, queryClient],
    );

    const handlePageSizeChange = useCallback((size: number) => {
        setPageSize(size);
        setPage(1);
    }, []);

    return (
        <TooltipProvider>
            <div className="space-y-4 p-6">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Deployment History</h1>
                    <p className="text-sm text-muted-foreground">
                        All API deployments, newest first. Select two versions to diff, or use the eye icon to inspect a single version.
                    </p>
                </div>

                {rollbackError ? (
                    <Card className="rounded-xl border border-destructive/30 bg-destructive/5 p-4">
                        <p className="text-sm text-destructive">{rollbackError}</p>
                    </Card>
                ) : null}

                {/* Toolbar: selection state + pagination on same row */}
                {!isLoading && events.length > 0 ? (
                    <div className="flex items-center justify-between gap-3 min-h-9">
                        {/* Left: selection hint */}
                        <div className="flex items-center gap-2 min-w-0">
                            {selectionCount > 0 ? (
                                <>
                                    <span className="text-sm text-primary font-medium">
                                        {selectionCount === 1
                                            ? '1 version selected — select one more to compare.'
                                            : '2 versions selected — diff view is open below.'}
                                    </span>
                                    <button
                                        type="button"
                                        className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
                                        onClick={() => setSelectedEvents([])}
                                    >
                                        <XIcon className="size-3" />
                                        Clear
                                    </button>
                                </>
                            ) : (
                                <span className="text-xs text-muted-foreground">Select two rows to compare versions.</span>
                            )}
                        </div>
                        {/* Right: pagination */}
                        <DataTablePagination
                            page={page}
                            pageSize={pageSize}
                            totalCount={totalCount}
                            pageSizeOptions={PAGE_SIZE_OPTIONS}
                            onPageChange={setPage}
                            onPageSizeChange={handlePageSizeChange}
                        />
                    </div>
                ) : null}

                {isLoading ? (
                    <div className="space-y-2">
                        {Array.from({ length: 5 }).map((_, i) => (
                            <Skeleton key={i} className="h-12 rounded-lg" />
                        ))}
                    </div>
                ) : events.length === 0 ? (
                    <Card>
                        <Empty>
                            <EmptyHeader>
                                <EmptyTitle>No deployments yet</EmptyTitle>
                                <EmptyDescription>Deployment records will appear here after the first publish.</EmptyDescription>
                            </EmptyHeader>
                            <EmptyContent />
                        </Empty>
                    </Card>
                ) : (
                    <>
                        <div className="rounded-lg border overflow-hidden">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead className="w-10 pl-4">
                                            <span className="sr-only">Select</span>
                                        </TableHead>
                                        <TableHead className="w-28">Version</TableHead>
                                        <TableHead>Date</TableHead>
                                        <TableHead>User</TableHead>
                                        <TableHead>Label</TableHead>
                                        <TableHead className="w-24 text-right pr-4">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {events.map((event, idx) => {
                                        const version = event.properties.DEPLOYMENT_NUMBER;
                                        const isLive = idx === 0 && page === 1;
                                        const isSelected = selectedEvents.some(e => e.id === event.id);
                                        const isDisabled = selectionCount >= 2 && !isSelected;
                                        const canCompareWithLive = !isLive && liveEvent !== null;

                                        return (
                                            <TableRow key={event.id} className={isSelected ? 'bg-primary/5' : undefined}>
                                                <TableCell className="pl-4 w-10">
                                                    <Checkbox
                                                        checked={isSelected}
                                                        disabled={isDisabled}
                                                        onCheckedChange={() => toggleSelect(event)}
                                                        aria-label={`Select version ${version ?? event.id}`}
                                                    />
                                                </TableCell>
                                                <TableCell className="font-medium">
                                                    <div className="flex items-center gap-2">
                                                        <span>{version ?? '—'}</span>
                                                        {isLive ? (
                                                            <Badge variant="default" className="text-xs">
                                                                live
                                                            </Badge>
                                                        ) : null}
                                                    </div>
                                                </TableCell>
                                                <TableCell className="text-sm whitespace-nowrap">{formatDate(event.createdAt)}</TableCell>
                                                <TableCell className="text-sm">{event.initiator.displayName}</TableCell>
                                                <TableCell className="text-sm text-muted-foreground">
                                                    {event.properties.DEPLOYMENT_LABEL || (
                                                        <span className="text-muted-foreground/40 italic">—</span>
                                                    )}
                                                </TableCell>
                                                <TableCell className="text-right pr-4">
                                                    <div className="flex items-center justify-end gap-1">
                                                        {canCompareWithLive ? (
                                                            <Tooltip>
                                                                <TooltipTrigger asChild>
                                                                    <Button
                                                                        type="button"
                                                                        variant="ghost"
                                                                        size="icon"
                                                                        className="size-8"
                                                                        onClick={() => handleCompareWithLive(event)}
                                                                        aria-label="Compare with live version"
                                                                    >
                                                                        <GitBranchIcon className="size-4" />
                                                                    </Button>
                                                                </TooltipTrigger>
                                                                <TooltipContent>Compare with live</TooltipContent>
                                                            </Tooltip>
                                                        ) : null}
                                                        <Tooltip>
                                                            <TooltipTrigger asChild>
                                                                <Button
                                                                    type="button"
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    className="size-8"
                                                                    onClick={() => setSingleViewEvent(event)}
                                                                    aria-label={`View version ${version ?? event.id}`}
                                                                >
                                                                    <EyeIcon className="size-4" />
                                                                </Button>
                                                            </TooltipTrigger>
                                                            <TooltipContent>View definition</TooltipContent>
                                                        </Tooltip>
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        );
                                    })}
                                </TableBody>
                            </Table>
                        </div>

                        {/* Bottom pagination */}
                        <div className="flex justify-end">
                            <DataTablePagination
                                page={page}
                                pageSize={pageSize}
                                totalCount={totalCount}
                                pageSizeOptions={PAGE_SIZE_OPTIONS}
                                onPageChange={setPage}
                                onPageSizeChange={handlePageSizeChange}
                            />
                        </div>
                    </>
                )}

                {singleViewEvent ? (
                    <SingleEventDialog
                        event={singleViewEvent}
                        onRollback={handleRollback}
                        onClose={() => setSingleViewEvent(null)}
                        isRollingBack={isRollingBack}
                    />
                ) : null}

                {showDiff ? (
                    <DiffDialog
                        left={selectedEvents[0]}
                        right={selectedEvents[1]}
                        onClose={handleDiffClose}
                        onRollback={handleRollback}
                        isRollingBack={isRollingBack}
                    />
                ) : null}
            </div>
        </TooltipProvider>
    );
}
