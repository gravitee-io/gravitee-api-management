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
    Badge,
    Pagination,
    PaginationContent,
    PaginationEllipsis,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { Trash2 } from 'lucide-react';
import type { MouseEvent } from 'react';
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

/** Returns up to 7 page entries with optional ellipsis collapsing. */
function pageWindow(current: number, count: number): Array<number | 'ellipsis'> {
    if (count <= 7) return Array.from({ length: count }, (_, i) => i + 1);
    if (current <= 4) return [1, 2, 3, 4, 5, 'ellipsis', count];
    if (current >= count - 3) return [1, 'ellipsis', count - 4, count - 3, count - 2, count - 1, count];
    return [1, 'ellipsis', current - 1, current, current + 1, 'ellipsis', count];
}

function PoliciesPagination({
    page,
    perPage,
    total,
    onPageChange,
}: {
    readonly page: number;
    readonly perPage: number;
    readonly total: number;
    readonly onPageChange: (page: number) => void;
}) {
    const pageCount = Math.max(1, Math.ceil(total / perPage));
    const from = total === 0 ? 0 : (page - 1) * perPage + 1;
    const to = Math.min(page * perPage, total);
    const goTo = (target: number) => (event: MouseEvent<HTMLAnchorElement>) => {
        event.preventDefault();
        if (target >= 1 && target <= pageCount && target !== page) onPageChange(target);
    };
    return (
        <Pagination className="justify-end py-2">
            <PaginationContent>
                <PaginationItem>
                    <span className="px-2 text-xs text-muted-foreground">
                        {from}–{to} of {total}
                    </span>
                </PaginationItem>
                <PaginationItem>
                    <PaginationPrevious
                        href="#"
                        aria-disabled={page <= 1}
                        className={page <= 1 ? 'pointer-events-none opacity-50' : undefined}
                        onClick={goTo(page - 1)}
                    />
                </PaginationItem>
                {pageWindow(page, pageCount).map((p, i) =>
                    p === 'ellipsis' ? (
                        <PaginationItem key={`e-${i}`}>
                            <PaginationEllipsis />
                        </PaginationItem>
                    ) : (
                        <PaginationItem key={p}>
                            <PaginationLink href="#" isActive={p === page} onClick={goTo(p)}>
                                {p}
                            </PaginationLink>
                        </PaginationItem>
                    ),
                )}
                <PaginationItem>
                    <PaginationNext
                        href="#"
                        aria-disabled={page >= pageCount}
                        className={page >= pageCount ? 'pointer-events-none opacity-50' : undefined}
                        onClick={goTo(page + 1)}
                    />
                </PaginationItem>
            </PaginationContent>
        </Pagination>
    );
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
    return (
        <>
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Name</TableHead>
                        {config.hasTarget && <TableHead>Target</TableHead>}
                        <TableHead>Status</TableHead>
                        <TableHead>Updated</TableHead>
                        <TableHead aria-label="Actions" />
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {policies.map(p => (
                        <TableRow
                            key={p.id}
                            data-testid={`row-policy-${p.id}`}
                            onClick={() => onEdit(p)}
                            className="cursor-pointer hover:bg-muted/40"
                        >
                            <TableCell>
                                <div>
                                    <p className="font-medium" data-testid={`row-policy-${p.id}-name`}>
                                        {p.name}
                                    </p>
                                    {p.description && (
                                        <p className="text-xs text-muted-foreground truncate max-w-[320px]">{p.description}</p>
                                    )}
                                </div>
                            </TableCell>
                            {config.hasTarget && (
                                <TableCell>
                                    {p.target ? (
                                        <Badge variant="outline" className="truncate max-w-[180px]" style={{ fontSize: '11px' }}>
                                            {p.target.label}
                                        </Badge>
                                    ) : (
                                        '—'
                                    )}
                                </TableCell>
                            )}
                            <TableCell>
                                <StatusBadge status={p.status} />
                            </TableCell>
                            <TableCell className="text-muted-foreground text-sm whitespace-nowrap">{relativeTime(p.updatedAt)}</TableCell>
                            <TableCell>
                                <div className="flex justify-end">
                                    <button
                                        type="button"
                                        onClick={e => {
                                            e.stopPropagation();
                                            onDelete(p);
                                        }}
                                        title="Delete"
                                        aria-label={`Delete ${p.name}`}
                                        className="inline-flex items-center justify-center size-8 rounded-md border border-border bg-background text-foreground/70 hover:bg-red-50 hover:text-red-600 hover:border-red-200"
                                    >
                                        <Trash2 className="size-3.5" />
                                    </button>
                                </div>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
            <PoliciesPagination page={page} perPage={perPage} total={allData.total} onPageChange={onPageChange} />
        </>
    );
}
