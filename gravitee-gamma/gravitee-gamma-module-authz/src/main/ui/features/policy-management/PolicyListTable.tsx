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
import { Badge, Button, DataTable, DataTablePagination } from '@gravitee/graphene-core';
import { PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import type { ColumnDef } from '@tanstack/react-table';
import { useMemo } from 'react';
import { StatusBadge } from '../../components/StatusBadge';
import type { PolicyResponse } from '../../shared/api/authz-api.types';
import type { ServicePageConfig } from './ServicePolicyPage';

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

export interface PolicyListTableProps {
    readonly config: ServicePageConfig;
    readonly policies: readonly PolicyResponse[];
    readonly totalCount: number;
    readonly page: number;
    readonly perPage: number;
    readonly isLoading?: boolean;
    readonly onPageChange: (page: number) => void;
    readonly onPerPageChange?: (perPage: number) => void;
    readonly onEdit: (policy: PolicyResponse) => void;
    readonly onDelete: (policy: PolicyResponse) => void;
}

// Backend currently emits a UNIX-seconds float for updatedAt (e.g. 1777283625.541)
// while the TS type declares string. new Date(seconds) yields 1970, so we widen.
export function parseUpdatedAt(value: unknown): Date {
    if (typeof value === 'number' && Number.isFinite(value)) {
        return new Date(value < 1e12 ? value * 1000 : value);
    }
    if (typeof value === 'string') {
        const trimmed = value.trim();
        if (trimmed !== '' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
            const n = Number(trimmed);
            if (Number.isFinite(n)) {
                return new Date(n < 1e12 ? n * 1000 : n);
            }
        }
        return new Date(trimmed);
    }
    return new Date(NaN);
}

export function relativeTime(value: unknown): string {
    const date = parseUpdatedAt(value);
    if (Number.isNaN(date.getTime())) return '—';
    const now = Date.now();
    const diffMs = now - date.getTime();
    const diffMin = Math.round(diffMs / 60000);
    if (diffMin < 1) return 'Just now';
    if (diffMin < 60) return `${diffMin} min ago`;
    const diffH = Math.round(diffMin / 60);
    if (diffH < 24) return `${diffH}h ago`;
    const diffD = Math.round(diffH / 24);
    if (diffD < 7) return `${diffD}d ago`;
    return date.toLocaleDateString();
}

export function PolicyListTable({
    config,
    policies,
    totalCount,
    page,
    perPage,
    isLoading = false,
    onPageChange,
    onPerPageChange,
    onEdit,
    onDelete,
}: PolicyListTableProps) {
    const data = useMemo(() => [...policies], [policies]);

    const columns = useMemo<ColumnDef<PolicyResponse>[]>(() => {
        const cols: ColumnDef<PolicyResponse>[] = [
            {
                id: 'name',
                header: 'Name',
                cell: ({ row }) => {
                    const policy = row.original;
                    return (
                        <Button
                            variant="ghost"
                            type="button"
                            onClick={() => onEdit(policy)}
                            data-testid={`row-policy-${policy.id}-name`}
                            className="-mx-2 -my-1 h-auto w-full max-w-full justify-start overflow-hidden rounded-sm px-2 py-1"
                        >
                            <span className="block truncate font-medium text-primary underline-offset-2 hover:underline">
                                {policy.name}
                            </span>
                            {policy.description ? (
                                <span className="block truncate text-xs text-muted-foreground">{policy.description}</span>
                            ) : null}
                        </Button>
                    );
                },
            },
        ];

        if (config.hasTarget) {
            cols.push({
                id: 'target',
                header: 'Target',
                cell: ({ row }) =>
                    row.original.target ? (
                        <Badge variant="outline" className="max-w-48 truncate text-xs">
                            {row.original.target.label}
                        </Badge>
                    ) : (
                        '—'
                    ),
            });
        }

        cols.push(
            {
                id: 'status',
                header: 'Status',
                cell: ({ row }) => <StatusBadge status={row.original.status} />,
            },
            {
                id: 'updated',
                header: 'Updated',
                cell: ({ row }) => (
                    <span className="whitespace-nowrap text-sm text-muted-foreground">{relativeTime(row.original.updatedAt)}</span>
                ),
            },
            {
                id: 'actions',
                header: '',
                cell: ({ row }) => {
                    const policy = row.original;
                    return (
                        <div className="flex justify-end gap-1">
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                onClick={() => onEdit(policy)}
                                title="Edit"
                                aria-label={`Edit ${policy.name}`}
                                className="text-muted-foreground"
                            >
                                <PencilIcon aria-hidden className="size-3.5" />
                            </Button>
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                onClick={() => onDelete(policy)}
                                title="Delete"
                                aria-label={`Delete ${policy.name}`}
                                className="text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                            >
                                <Trash2Icon aria-hidden className="size-3.5" />
                            </Button>
                        </div>
                    );
                },
            },
        );

        return cols;
    }, [config.hasTarget, onDelete, onEdit]);

    return (
        <>
            <DataTable<PolicyResponse>
                columns={columns}
                data={data}
                serverSide
                loading={isLoading}
                skeletonCount={perPage}
                emptyMessage="No policies match the current filters."
            />
            <DataTablePagination
                page={page}
                pageSize={perPage}
                totalCount={totalCount}
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                onPageChange={onPageChange}
                onPageSizeChange={onPerPageChange}
            />
        </>
    );
}
