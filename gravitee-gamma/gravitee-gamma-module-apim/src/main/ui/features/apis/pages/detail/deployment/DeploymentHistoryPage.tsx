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
    DataTable,
    DataTableEmptyState,
    DateCell,
    type DataTableProps,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    useLayoutConfig,
} from '@gravitee/graphene-core';
import { EyeIcon, GitBranchIcon, MoreVerticalIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { DiffDialog } from './DiffDialog';
import { SingleEventDialog } from './SingleEventDialog';
import { useApiEvents } from '../../../hooks/useApiEvents';
import { rollbackApi } from '../../../services/apis';
import type { ApiEvent } from '../../../types';
import { apiEventsKeys } from '../../../utils/queryKeys';

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

type Cell<T> = { row: { index: number; original: T } };

export function DeploymentHistoryPage() {
    useLayoutConfig({ contentVariant: 'wide' }, []);
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

    const columns: DataTableProps<ApiEvent>['columns'] = [
        {
            id: 'select',
            header: () => <span className="sr-only">Select</span>,
            size: 40,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: Cell<ApiEvent>) => {
                const event = row.original;
                const isSelected = selectedEvents.some(e => e.id === event.id);
                const isDisabled = selectionCount >= 2 && !isSelected;
                const version = event.properties.DEPLOYMENT_NUMBER;
                return (
                    <Checkbox
                        checked={isSelected}
                        disabled={isDisabled}
                        onCheckedChange={() => toggleSelect(event)}
                        aria-label={`Select version ${version ?? event.id}`}
                    />
                );
            },
        },
        {
            id: 'Version',
            header: 'Version',
            size: 112,
            enableSorting: false,
            cell: ({ row }: Cell<ApiEvent>) => {
                const event = row.original;
                const isLive = row.index === 0 && page === 1;
                const version = event.properties.DEPLOYMENT_NUMBER;
                return (
                    <div className="flex items-center gap-2 font-medium">
                        <span>{version ?? '—'}</span>
                        {isLive ? (
                            <Badge variant="default" className="text-xs">
                                live
                            </Badge>
                        ) : null}
                    </div>
                );
            },
        },
        {
            id: 'Date',
            accessorFn: (row: ApiEvent) => row.createdAt,
            header: 'Date',
            enableSorting: false,
            cell: ({ row }: Cell<ApiEvent>) => <DateCell value={row.original.createdAt} format="absolute" />,
        },
        {
            id: 'User',
            accessorFn: (row: ApiEvent) => row.initiator.displayName,
            header: 'User',
            enableSorting: false,
            cell: ({ row }: Cell<ApiEvent>) => <span className="text-sm">{row.original.initiator.displayName}</span>,
        },
        {
            id: 'Label',
            header: 'Label',
            enableSorting: false,
            cell: ({ row }: Cell<ApiEvent>) =>
                row.original.properties.DEPLOYMENT_LABEL ? (
                    <span className="text-sm text-muted-foreground">{row.original.properties.DEPLOYMENT_LABEL}</span>
                ) : (
                    <span className="text-muted-foreground/40 italic">—</span>
                ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: Cell<ApiEvent>) => {
                const event = row.original;
                const isLive = row.index === 0 && page === 1;
                const canCompareWithLive = !isLive && liveEvent !== null;
                if (!canCompareWithLive) {
                    return (
                        <div className="flex justify-end">
                            <Button
                                variant="ghost"
                                size="icon"
                                className="size-8"
                                aria-label="View definition"
                                title="View definition"
                                onClick={() => setSingleViewEvent(event)}
                            >
                                <EyeIcon className="size-4" aria-hidden />
                            </Button>
                        </div>
                    );
                }
                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" aria-label="Deployment actions">
                                    <MoreVerticalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-auto min-w-48">
                                <DropdownMenuItem onSelect={() => setSingleViewEvent(event)}>
                                    <EyeIcon className="size-3.5" />
                                    View definition
                                </DropdownMenuItem>
                                <DropdownMenuItem onSelect={() => handleCompareWithLive(event)}>
                                    <GitBranchIcon className="size-3.5" />
                                    Compare with live
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        },
    ];

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Deployment History</h1>
                <p className="text-sm text-muted-foreground">
                    All API deployments, newest first. Select two versions to diff, or use the actions menu to inspect a single version.
                </p>
            </div>

            {rollbackError ? (
                <Card className="rounded-xl border border-destructive/30 bg-destructive/5 p-4">
                    <p className="text-sm text-destructive">{rollbackError}</p>
                </Card>
            ) : null}

            {!isLoading && totalCount === 0 ? (
                <div className="rounded-lg border">
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<GitBranchIcon />}
                        title="No deployments yet"
                        description="Deployment records will appear here after the first publish."
                    />
                </div>
            ) : (
                <DataTable
                    aria-label="Deployment history"
                    columns={columns}
                    data={events}
                    loading={isLoading}
                    skeletonCount={pageSize}
                    serverSide
                    pagination={
                        totalCount > 0
                            ? {
                                  page,
                                  pageSize,
                                  totalCount,
                                  pageSizeOptions: PAGE_SIZE_OPTIONS,
                                  onPageChange: setPage,
                                  onPageSizeChange: handlePageSizeChange,
                              }
                            : undefined
                    }
                    toolbar={
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
                    }
                />
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
    );
}
