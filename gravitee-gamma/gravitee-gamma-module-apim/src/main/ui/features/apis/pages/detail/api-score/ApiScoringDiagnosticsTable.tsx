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
    DataTable,
    DataTableColumnHeader,
    DataTableEmptyState,
    Input,
    type DataTableColumnHeaderProps,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import type { ScoringDiagnostic } from '../../../types/scoring';
import { clampPage, diagnosticMatchesSearch, formatLineColumn, paginateItems } from '../../../utils/scoring';

export const DIAGNOSTICS_PAGE_SIZE = 5;
const PAGE_SIZE_OPTIONS = [5, 10];

const SEVERITY_CLASS: Record<ScoringDiagnostic['severity'], string> = {
    ERROR: 'text-destructive',
    WARN: 'text-warning',
    INFO: 'text-primary',
    HINT: 'text-success',
};

type ColCell = { row: { original: ScoringDiagnostic } };
type ColHeader = { column: DataTableColumnHeaderProps<ScoringDiagnostic, unknown>['column'] };
type TableSorting = NonNullable<DataTableProps<ScoringDiagnostic>['sorting']>;

const COLUMNS: DataTableProps<ScoringDiagnostic>['columns'] = [
    {
        id: 'Severity',
        accessorFn: (row: ScoringDiagnostic) => row.severity,
        header: ({ column }: ColHeader) => <DataTableColumnHeader column={column} title="Severity" />,
        enableSorting: true,
        cell: ({ row }: ColCell) => (
            <span className={`text-xs font-semibold ${SEVERITY_CLASS[row.original.severity]}`}>{row.original.severity}</span>
        ),
    },
    {
        id: 'Line/Column',
        header: 'Line/Column',
        enableSorting: false,
        cell: ({ row }: ColCell) => formatLineColumn(row.original),
    },
    {
        id: 'Recommendation',
        accessorFn: (row: ScoringDiagnostic) => row.message,
        header: 'Recommendation',
        enableSorting: false,
        cell: ({ row }: ColCell) => row.original.message,
    },
    {
        id: 'Path',
        accessorFn: (row: ScoringDiagnostic) => row.path,
        header: ({ column }: ColHeader) => <DataTableColumnHeader column={column} title="Path" />,
        enableSorting: true,
        cell: ({ row }: ColCell) => <span className="font-mono text-xs">{row.original.path}</span>,
    },
];

export function ApiScoringDiagnosticsTable({ diagnostics, ariaLabel }: Readonly<{ diagnostics: ScoringDiagnostic[]; ariaLabel: string }>) {
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DIAGNOSTICS_PAGE_SIZE);
    const [sorting, setSorting] = useState<TableSorting>([]);

    const filtered = useMemo(() => diagnostics.filter(item => diagnosticMatchesSearch(item, search)), [diagnostics, search]);
    const sorted = useMemo(() => {
        const sort = sorting[0];
        if (!sort) return filtered;
        const key = sort.id === 'Path' ? 'path' : sort.id === 'Severity' ? 'severity' : null;
        if (!key) return filtered;
        const direction = sort.desc ? -1 : 1;
        return [...filtered].sort((left, right) => left[key].localeCompare(right[key]) * direction);
    }, [filtered, sorting]);
    const currentPage = clampPage(page, sorted.length, pageSize);
    const paginated = paginateItems(sorted, currentPage, pageSize);
    const searching = Boolean(search.trim());

    return (
        <DataTable
            aria-label={ariaLabel}
            columns={COLUMNS}
            data={paginated}
            serverSide
            sorting={sorting}
            onSortingChange={updater => {
                setSorting(previous => (typeof updater === 'function' ? updater(previous) : updater));
                setPage(1);
            }}
            pagination={
                sorted.length > 0
                    ? {
                          page: currentPage,
                          pageSize,
                          totalCount: sorted.length,
                          pageSizeOptions: PAGE_SIZE_OPTIONS,
                          onPageChange: setPage,
                          onPageSizeChange: next => {
                              setPageSize(next);
                              setPage(1);
                          },
                      }
                    : undefined
            }
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon />}
                    title="No data to display"
                    description={searching ? 'No diagnostics match the current search.' : ''}
                />
            }
            toolbar={
                <div className="relative w-full max-w-md">
                    <SearchIcon
                        className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                        aria-hidden
                    />
                    <Input
                        className="pl-9"
                        placeholder="Search severity, recommendation, or path..."
                        aria-label="Search severity, recommendation, or path"
                        value={search}
                        onChange={event => {
                            setSearch(event.target.value);
                            setPage(1);
                        }}
                    />
                </div>
            }
        />
    );
}

export function ScoringAssetTypeBadge({ type }: Readonly<{ type: string }>) {
    return (
        <Badge variant="secondary" className="text-xs font-mono">
            {type}
        </Badge>
    );
}
