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
import { EyeIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';
import { useMemo } from 'react';

import type { ColCell } from '../../applications/utils/dataTableTypes';
import type { AuditLogRow } from '../types/auditLog';
import { formatAuditTargetText, prettyPrintPatch } from '../utils/auditListFormat';

export const AUDIT_PAGE_SIZE_OPTIONS = [10, 25, 50, 100] as const;

export interface AuditLogsTableProps {
    readonly rows: readonly AuditLogRow[];
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
}

function buildColumns(onSelectRow: (row: AuditLogRow) => void): DataTableProps<AuditLogRow>['columns'] {
    return [
        {
            id: 'Date',
            accessorFn: (row: AuditLogRow) => row.createdAt,
            header: 'Date',
            enableSorting: false,
            cell: ({ row }: ColCell<AuditLogRow>) => (
                <button type="button" className="text-left" onClick={() => onSelectRow(row.original)}>
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
                <button type="button" className="text-left text-sm" onClick={() => onSelectRow(row.original)}>
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
            cell: ({ row }: ColCell<AuditLogRow>) => {
                const text = formatAuditTargetText(row.original.targets);
                return text ? (
                    <div className="text-sm">
                        {row.original.targets.map(target => (
                            <div key={`${target.key}:${target.value}`}>
                                {target.key}: {target.value}
                            </div>
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
}: AuditLogsTableProps) {
    const columns = useMemo(() => buildColumns(onSelectRow), [onSelectRow]);

    return (
        <>
            <DataTable
                aria-label="Audit logs"
                columns={columns}
                data={[...rows]}
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
                              onPageSizeChange: (size: number) => {
                                  onPageSizeChange(size);
                                  onPageChange(1);
                              },
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
