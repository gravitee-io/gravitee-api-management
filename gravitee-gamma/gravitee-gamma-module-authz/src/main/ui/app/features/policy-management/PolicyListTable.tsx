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
import type { ColumnDef } from '@tanstack/react-table';
import { PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';
import { StatusBadge } from '../../../components/StatusBadge';
import type { PagedResponse, PolicyResponse } from '../../../lib/api/authz-api.types';
import type { ServicePageConfig } from './ServicePolicyPage';

export interface PolicyListTableProps {
    readonly config: ServicePageConfig;
    readonly policies: readonly PolicyResponse[];
    readonly allData: PagedResponse<PolicyResponse>;
    readonly page: number;
    readonly perPage: number;
    readonly onPageChange: (page: number) => void;
    readonly onEdit: (policy: PolicyResponse) => void;
    readonly onDelete: (policy: PolicyResponse) => void;
}

/**
 * Build a `Date` from whatever shape the backend hands us for `updatedAt`.
 *
 * The TypeScript types declare `string`, but the live backend currently emits
 * a UNIX-seconds float (e.g. `1777283625.541`). Treating that number as
 * milliseconds — what `new Date(number)` does — yields 1970 (bug E).
 *
 * Heuristic:
 *   - number / numeric string < 1e12  → seconds, multiply by 1000
 *   - number / numeric string ≥ 1e12  → already milliseconds
 *   - non-numeric string              → ISO / RFC string, hand to Date as-is
 *
 * `1e12` ms is roughly 2001-09; any reasonable seconds-epoch is below that
 * threshold, any reasonable ms-epoch is above, so the heuristic is unambiguous.
 */
export function parseUpdatedAt(value: unknown): Date {
    if (typeof value === 'number' && Number.isFinite(value)) {
        return new Date(value < 1e12 ? value * 1000 : value);
    }
    if (typeof value === 'string') {
        const trimmed = value.trim();
        // Numeric string like "1777283625.541" — same heuristic as the number branch.
        if (trimmed !== '' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
            const n = Number(trimmed);
            if (Number.isFinite(n)) {
                return new Date(n < 1e12 ? n * 1000 : n);
            }
        }
        return new Date(trimmed);
    }
    // Anything else → invalid date so callers can render a fallback.
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

export function PolicyListTable({ config, policies, allData, page, perPage, onPageChange, onEdit, onDelete }: PolicyListTableProps) {
    // Sorting/filtering happen in the parent (ServicePolicyPage); DataTable runs
    // in server-side mode so it doesn't try to apply them client-side. Columns
    // are memoised — `DataTable` re-evaluates the table on every column-ref
    // change, and `hasTarget` flips the schema only between page mounts.
    const columns = useMemo<ColumnDef<PolicyResponse>[]>(() => {
        const cols: ColumnDef<PolicyResponse>[] = [
            {
                id: 'name',
                header: 'Name',
                cell: ({ row }) => {
                    const p = row.original;
                    return (
                        <button
                            type="button"
                            onClick={() => onEdit(p)}
                            data-testid={`row-policy-${p.id}-name`}
                            className="-mx-2 -my-1 block w-full rounded px-2 py-1 text-left hover:bg-muted/40 focus-visible:outline-2 focus-visible:outline-ring"
                        >
                            <p className="font-medium text-primary underline-offset-2 hover:underline">{p.name}</p>
                            {p.description && (
                                <p className="max-w-80 truncate text-xs text-muted-foreground">{p.description}</p>
                            )}
                        </button>
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
                    <span className="whitespace-nowrap text-sm text-muted-foreground">
                        {relativeTime(row.original.updatedAt)}
                    </span>
                ),
            },
            {
                id: 'actions',
                header: '',
                cell: ({ row }) => {
                    const p = row.original;
                    return (
                        <div className="flex justify-end gap-1">
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                onClick={() => onEdit(p)}
                                title="Edit"
                                aria-label={`Edit ${p.name}`}
                                className="text-foreground/70 hover:bg-accent"
                            >
                                <PencilIcon aria-hidden className="size-3.5" />
                            </Button>
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                onClick={() => onDelete(p)}
                                title="Delete"
                                aria-label={`Delete ${p.name}`}
                                className="text-foreground/70 hover:bg-destructive/10 hover:text-destructive"
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
                data={[...policies]}
                serverSide
                emptyMessage="No policies match the current filters."
            />
            <DataTablePagination page={page} pageSize={perPage} totalCount={allData.total} onPageChange={onPageChange} />
        </>
    );
}
