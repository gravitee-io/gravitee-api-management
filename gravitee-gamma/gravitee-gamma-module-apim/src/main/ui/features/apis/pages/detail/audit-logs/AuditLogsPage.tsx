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
    DataTable,
    DataTableEmptyState,
    DateCell,
    type DataTableProps,
    DateRangePicker,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { EyeIcon, SearchIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, useState } from 'react';
import type { DateRange } from 'react-day-picker';
import { useParams } from 'react-router-dom';

import { AuditLogsLanding } from './AuditLogsLanding';
import { useAuditEvents } from '../../../hooks/useAuditEvents';
import { useAuditLogs } from '../../../hooks/useAuditLogs';
import type { Audit } from '../../../types/auditLogs.types';

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
const ALL_EVENTS = '__all__';

type ColCell<T> = { row: { original: T } };

function formatPatch(patch: string): string {
    try {
        return JSON.stringify(JSON.parse(patch), null, 2);
    } catch {
        return patch;
    }
}

function formatTarget(audit: Audit): string {
    return audit.properties.map(p => `${p.key}: ${p.value}`).join('\n');
}

function buildColumns(onViewPatch: (audit: Audit) => void): DataTableProps<Audit>['columns'] {
    return [
        {
            id: 'Date',
            accessorFn: (row: Audit) => row.createdAt,
            header: 'Date',
            enableSorting: false,
            cell: ({ row }: ColCell<Audit>) => <DateCell value={row.original.createdAt} format="absolute" />,
        },
        {
            id: 'Actor',
            accessorFn: (row: Audit) => row.user.displayName,
            header: 'Actor',
            enableSorting: false,
            cell: ({ row }: ColCell<Audit>) => <span className="text-sm">{row.original.user.displayName}</span>,
        },
        {
            id: 'Event',
            accessorFn: (row: Audit) => row.event,
            header: 'Event',
            enableSorting: false,
            cell: ({ row }: ColCell<Audit>) => <span className="font-mono text-sm">{row.original.event}</span>,
        },
        {
            id: 'Target',
            header: 'Target',
            enableSorting: false,
            cell: ({ row }: ColCell<Audit>) => {
                const target = formatTarget(row.original);
                return target ? (
                    <div className="text-sm">
                        {target.split('\n').map(line => (
                            <div key={line}>{line}</div>
                        ))}
                    </div>
                ) : (
                    <span className="text-muted-foreground/40 italic">—</span>
                );
            },
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Patch</span>,
            size: 64,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<Audit>) => {
                const audit = row.original;
                if (!audit.patch) return null;
                return (
                    <div className="flex justify-end">
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-8"
                            onClick={() => onViewPatch(audit)}
                            aria-label="View patch"
                            title="View patch"
                        >
                            <EyeIcon className="size-4" />
                        </Button>
                    </div>
                );
            },
        },
    ];
}

export function AuditLogsPage() {
    const { apiId } = useParams<{ apiId: string }>();

    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(10);
    const [selectedEvent, setSelectedEvent] = useState('');
    const [dateRange, setDateRange] = useState<DateRange | undefined>();
    const [patchAudit, setPatchAudit] = useState<Audit | null>(null);

    const from = dateRange?.from ? dateRange.from.getTime() : undefined;
    const to = dateRange?.to ? dateRange.to.getTime() : undefined;

    const params = useMemo(
        () => ({ page, perPage, events: selectedEvent || undefined, from, to }),
        [page, perPage, selectedEvent, from, to],
    );

    const { data, isLoading } = useAuditLogs(apiId, params);
    const { data: eventTypes } = useAuditEvents(apiId);

    const audits = data?.data ?? [];
    const totalCount = data?.pagination.totalCount ?? 0;
    const hasFilters = Boolean(selectedEvent || dateRange);

    const handleEventChange = useCallback((value: string) => {
        setSelectedEvent(value === ALL_EVENTS ? '' : value);
        setPage(1);
    }, []);

    const handleDateChange = useCallback((range: DateRange | undefined) => {
        setDateRange(range);
        setPage(1);
    }, []);

    const handleReset = useCallback(() => {
        setSelectedEvent('');
        setDateRange(undefined);
        setPage(1);
    }, []);

    const handlePageSizeChange = useCallback((size: number) => {
        setPerPage(size);
        setPage(1);
    }, []);

    const columns = buildColumns(setPatchAudit);

    if (!isLoading && !hasFilters && totalCount === 0) {
        return <AuditLogsLanding />;
    }

    return (
        <div className="space-y-4">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Audit Logs</h1>
                <p className="text-sm text-muted-foreground">Gather all events and changes generated by the API Management</p>
            </div>

            <DataTable
                aria-label="Audit logs"
                columns={columns}
                data={audits}
                loading={isLoading}
                skeletonCount={perPage}
                serverSide
                pagination={
                    totalCount > 0
                        ? {
                              page,
                              pageSize: perPage,
                              totalCount,
                              pageSizeOptions: PAGE_SIZE_OPTIONS,
                              onPageChange: setPage,
                              onPageSizeChange: handlePageSizeChange,
                          }
                        : undefined
                }
                emptyMessage={
                    <DataTableEmptyState
                        variant="no-results"
                        icon={<SearchIcon />}
                        title="No audit logs found"
                        description="Try adjusting or clearing your filters."
                    />
                }
                toolbar={
                    <>
                        <Select value={selectedEvent || ALL_EVENTS} onValueChange={handleEventChange}>
                            <SelectTrigger className="w-44" aria-label="Filter by event type">
                                <SelectValue placeholder="All events" />
                            </SelectTrigger>
                            <SelectContent position="popper" className="max-h-72 overflow-y-auto">
                                <SelectItem value={ALL_EVENTS}>All events</SelectItem>
                                {(eventTypes ?? []).map(event => (
                                    <SelectItem key={event} value={event}>
                                        {event}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <DateRangePicker
                            value={dateRange}
                            onChange={handleDateChange}
                            placeholder="Date range"
                            numberOfMonths={2}
                            aria-label="Filter by date range"
                            className="w-64"
                        />
                        {hasFilters ? (
                            <Button type="button" variant="ghost" size="sm" onClick={handleReset} className="gap-1">
                                <XIcon className="size-3.5" aria-hidden="true" />
                                Reset
                            </Button>
                        ) : null}
                    </>
                }
            />

            <PatchDrawer audit={patchAudit} onClose={() => setPatchAudit(null)} />
        </div>
    );
}

function PatchDrawer({ audit, onClose }: Readonly<{ audit: Audit | null; onClose: () => void }>) {
    return (
        <Sheet open={Boolean(audit)} onOpenChange={open => !open && onClose()}>
            <SheetContent side="right" className="sm:max-w-xl">
                <SheetHeader>
                    <SheetTitle>JSON Patch</SheetTitle>
                </SheetHeader>
                <div className="flex-1 overflow-y-auto px-4 pb-4">
                    {audit?.patch ? (
                        <pre className="overflow-auto whitespace-pre-wrap break-words rounded-lg bg-muted p-4 text-xs font-mono text-foreground">
                            {formatPatch(audit.patch)}
                        </pre>
                    ) : null}
                </div>
            </SheetContent>
        </Sheet>
    );
}
