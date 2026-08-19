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
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { EyeIcon, ScrollTextIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';
import { useMemo } from 'react';

import type { ColCell } from '../../applications/utils/dataTableTypes';
import type { AuditLogRow } from '../types/auditLog';
import { prettyPrintPatch } from '../utils/auditListFormat';

export const AUDIT_PAGE_SIZE_OPTIONS = [10, 25, 50, 100] as const;

export interface AuditLogsTableProps {
    readonly rows: AuditLogRow[];
    readonly loading: boolean;
    readonly page: number;
    readonly pageSize: number;
    readonly totalCount: number;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
    readonly toolbar?: ReactNode;
    readonly selected: AuditLogRow | null;
    readonly onSelectRow: (row: AuditLogRow) => void;
    readonly onCloseDetail: () => void;
    readonly hasActiveFilters?: boolean;
    readonly hideEmptyState?: boolean;
}

// Every row opens the same sheet, so the labels have to say *which* row — a screen reader reading a
// page of identical "View audit details" buttons cannot tell them apart. The event is the most
// identifying field, so it anchors each label.
function detailLabel(row: AuditLogRow, context: string): string {
    return `View audit details for ${row.event} ${context}`;
}

function buildColumns(onSelectRow: (row: AuditLogRow) => void): DataTableProps<AuditLogRow>['columns'] {
    return [
        {
            id: 'Date',
            accessorFn: (row: AuditLogRow) => row.createdAt,
            header: 'Date',
            enableSorting: false,
            cell: ({ row }: ColCell<AuditLogRow>) => (
                <button
                    type="button"
                    className="text-left"
                    aria-label={detailLabel(row.original, `at ${new Date(row.original.createdAt).toISOString()}`)}
                    onClick={() => onSelectRow(row.original)}
                >
                    {/* Audit is a forensic log: wall-clock time belongs in the cell, not a relative "2h ago". */}
                    <DateCell value={new Date(row.original.createdAt).toISOString()} format="absolute" />
                </button>
            ),
        },
        {
            id: 'User',
            accessorFn: (row: AuditLogRow) => row.user,
            header: 'User',
            enableSorting: false,
            cell: ({ row }: ColCell<AuditLogRow>) => (
                <button
                    type="button"
                    className="text-left text-sm"
                    aria-label={detailLabel(row.original, `by ${row.original.user}`)}
                    onClick={() => onSelectRow(row.original)}
                >
                    {row.original.user}
                </button>
            ),
        },
        {
            id: 'Type',
            accessorFn: (row: AuditLogRow) => row.referenceType,
            header: 'Type',
            enableSorting: false,
        },
        {
            id: 'Reference',
            accessorFn: (row: AuditLogRow) => row.reference,
            header: 'Reference',
            enableSorting: false,
        },
        {
            id: 'Event',
            accessorFn: (row: AuditLogRow) => row.event,
            header: 'Event',
            enableSorting: false,
            cell: ({ row }: ColCell<AuditLogRow>) => <span className="font-mono text-sm">{row.original.event}</span>,
        },
        {
            id: 'Target',
            header: 'Target',
            enableSorting: false,
            cell: ({ row }: ColCell<AuditLogRow>) =>
                row.original.targets.length > 0 ? (
                    <div className="text-sm">
                        {row.original.targets.map(target => (
                            <div key={`${target.key}:${target.value}`}>
                                {target.key}: {target.value}
                            </div>
                        ))}
                    </div>
                ) : (
                    <span className="text-muted-foreground/40 italic">—</span>
                ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Patch</span>,
            size: 64,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<AuditLogRow>) => {
                if (!row.original.patch) return null;
                return (
                    <div className="flex justify-end">
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-8"
                            onClick={() => onSelectRow(row.original)}
                            aria-label={`View patch for ${row.original.event}`}
                            title="View patch"
                        >
                            <EyeIcon className="size-4" aria-hidden="true" />
                        </Button>
                    </div>
                );
            },
        },
    ];
}

export function AuditLogsTable({
    rows,
    loading,
    page,
    pageSize,
    totalCount,
    onPageChange,
    onPageSizeChange,
    toolbar,
    selected,
    onSelectRow,
    onCloseDetail,
    hasActiveFilters = false,
    hideEmptyState = false,
}: AuditLogsTableProps) {
    const columns = useMemo(() => buildColumns(onSelectRow), [onSelectRow]);

    return (
        <>
            <DataTable
                aria-label="Audit logs"
                columns={columns}
                data={rows}
                loading={loading}
                skeletonCount={pageSize}
                serverSide
                pagination={
                    totalCount > 0
                        ? {
                              page,
                              pageSize,
                              totalCount,
                              pageSizeOptions: [...AUDIT_PAGE_SIZE_OPTIONS],
                              onPageChange,
                              // The owner of the page state resets the offset; forward the size as-is.
                              onPageSizeChange,
                          }
                        : undefined
                }
                emptyMessage={
                    hideEmptyState ? undefined : hasActiveFilters ? (
                        <DataTableEmptyState
                            variant="no-results"
                            icon={<SearchIcon />}
                            title="No audit logs found"
                            description="Try adjusting or clearing your filters."
                        />
                    ) : (
                        <DataTableEmptyState
                            variant="first-use"
                            icon={<ScrollTextIcon />}
                            title="No audit logs"
                            description="Configuration changes will appear here."
                        />
                    )
                }
                toolbar={toolbar}
            />
            <AuditDetailSheet row={selected} onClose={onCloseDetail} />
        </>
    );
}

function AuditDetailSheet({ row, onClose }: Readonly<{ row: AuditLogRow | null; onClose: () => void }>) {
    return (
        <Sheet open={Boolean(row)} onOpenChange={open => !open && onClose()}>
            <SheetContent side="right" className="sm:max-w-xl">
                <SheetHeader>
                    <SheetTitle>Audit event</SheetTitle>
                    <SheetDescription>User, resource, and JSON Patch for this configuration change.</SheetDescription>
                </SheetHeader>
                {row ? (
                    <div className="flex-1 space-y-4 overflow-y-auto px-4 pb-4 text-sm">
                        <DetailField label="User" value={row.user} />
                        <DetailField label="Type" value={row.referenceType} />
                        <DetailField label="Reference" value={row.reference} />
                        <DetailField label="Event" value={row.event} />
                        <div>
                            <p className="mb-1 text-xs font-medium text-muted-foreground">Target</p>
                            {row.targets.length > 0 ? (
                                row.targets.map(target => (
                                    <div key={`${target.key}:${target.value}`}>
                                        {target.key}: {target.value}
                                    </div>
                                ))
                            ) : (
                                <span className="text-muted-foreground">—</span>
                            )}
                        </div>
                        {row.patch ? (
                            <div>
                                <p className="mb-1 text-xs font-medium text-muted-foreground">JSON Patch</p>
                                <pre className="overflow-auto whitespace-pre-wrap break-words rounded-lg bg-muted p-4 text-xs font-mono text-foreground">
                                    {prettyPrintPatch(row.patch)}
                                </pre>
                            </div>
                        ) : null}
                    </div>
                ) : null}
            </SheetContent>
        </Sheet>
    );
}

function DetailField({ label, value }: { label: string; value: string }) {
    return (
        <div>
            <p className="mb-1 text-xs font-medium text-muted-foreground">{label}</p>
            <p>{value}</p>
        </div>
    );
}
