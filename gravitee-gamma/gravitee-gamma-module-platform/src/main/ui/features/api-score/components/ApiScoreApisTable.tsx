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
    Button,
    DataTable,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreVerticalIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';
import { Link } from 'react-router-dom';

import { NON_SORTABLE_COLUMN } from '../../applications/utils/dataTableHeaders';
import type { ColCell } from '../../applications/utils/dataTableTypes';
import type { EnvironmentApiScore } from '../types/scoring';
import { TABLE_PAGE_SIZE_OPTIONS } from '../utils/paginationConstants';
import { formatScorePercent, isScoreAvailable, scoreTone } from '../utils/scoring';

const SCORE_PILL_CLASS = {
    success: 'border-success/20 text-success bg-success/10',
    warning: 'text-warning border-warning/30 bg-warning/10',
    error: 'border-destructive/20 text-destructive bg-destructive/10',
} as const;

const COUNT_TONE_CLASS = {
    errors: 'border-destructive/20 text-destructive',
    warnings: 'text-warning border-warning/30',
    infos: 'border-primary/20 text-primary',
    hints: 'border-success/20 text-success',
} as const;

function ScoreCell({ score }: Readonly<{ score: number | null | undefined }>) {
    if (!isScoreAvailable(score)) {
        return <span className="text-muted-foreground">Not available</span>;
    }
    const tone = scoreTone(score);
    return (
        <Badge variant="outline" className={`gap-1 ${SCORE_PILL_CLASS[tone]}`}>
            <ShieldCheckIcon className="size-3.5" aria-hidden />
            {formatScorePercent(score)}
        </Badge>
    );
}

function CountCell({
    value,
    scored,
    tone,
}: Readonly<{ value: number | null | undefined; scored: boolean; tone: keyof typeof COUNT_TONE_CLASS }>) {
    if (!scored || value === null || value === undefined) {
        return <span className="text-muted-foreground">—</span>;
    }
    if (value === 0) {
        return <span className="tabular-nums">{value}</span>;
    }
    return (
        <Badge variant="outline" className={COUNT_TONE_CLASS[tone]}>
            {value}
        </Badge>
    );
}

function buildColumns(detailHref: (apiId: string) => string): DataTableProps<EnvironmentApiScore>['columns'] {
    return [
        {
            id: 'name',
            accessorKey: 'name',
            ...NON_SORTABLE_COLUMN,
            header: 'API Name',
            cell: ({ row }: ColCell<EnvironmentApiScore>) => <span className="font-medium">{row.original.name}</span>,
        },
        {
            id: 'score',
            accessorKey: 'score',
            ...NON_SORTABLE_COLUMN,
            header: 'Score',
            cell: ({ row }: ColCell<EnvironmentApiScore>) => <ScoreCell score={row.original.score} />,
        },
        {
            id: 'errors',
            accessorKey: 'errors',
            ...NON_SORTABLE_COLUMN,
            header: 'Errors',
            cell: ({ row }: ColCell<EnvironmentApiScore>) => (
                <CountCell value={row.original.errors} scored={isScoreAvailable(row.original.score)} tone="errors" />
            ),
        },
        {
            id: 'warnings',
            accessorKey: 'warnings',
            ...NON_SORTABLE_COLUMN,
            header: 'Warnings',
            cell: ({ row }: ColCell<EnvironmentApiScore>) => (
                <CountCell value={row.original.warnings} scored={isScoreAvailable(row.original.score)} tone="warnings" />
            ),
        },
        {
            id: 'infos',
            accessorKey: 'infos',
            ...NON_SORTABLE_COLUMN,
            header: 'Infos',
            cell: ({ row }: ColCell<EnvironmentApiScore>) => (
                <CountCell value={row.original.infos} scored={isScoreAvailable(row.original.score)} tone="infos" />
            ),
        },
        {
            id: 'hints',
            accessorKey: 'hints',
            ...NON_SORTABLE_COLUMN,
            header: 'Hints',
            cell: ({ row }: ColCell<EnvironmentApiScore>) => (
                <CountCell value={row.original.hints} scored={isScoreAvailable(row.original.score)} tone="hints" />
            ),
        },
        {
            id: 'actions',
            ...NON_SORTABLE_COLUMN,
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            cell: ({ row }: ColCell<EnvironmentApiScore>) => (
                <div className="flex justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="size-8"
                                aria-label={`Actions for ${row.original.name}`}
                            >
                                <MoreVerticalIcon className="size-4" aria-hidden />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-auto min-w-56">
                            <DropdownMenuItem asChild className="whitespace-nowrap">
                                <Link to={detailHref(row.original.id)}>View API-level Score Details</Link>
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            ),
        },
    ];
}

export function ApiScoreApisTable({
    apis,
    totalCount,
    loading,
    page,
    pageSize,
    detailHref,
    onPageChange,
    onPageSizeChange,
}: Readonly<{
    apis: EnvironmentApiScore[];
    totalCount: number;
    loading: boolean;
    page: number;
    pageSize: number;
    detailHref: (apiId: string) => string;
    onPageChange: (page: number) => void;
    onPageSizeChange: (size: number) => void;
}>) {
    return (
        <div className="space-y-4">
            <h2 className="text-lg font-semibold tracking-tight">APIs</h2>
            <DataTable
                aria-label="APIs scores table"
                columns={buildColumns(detailHref)}
                data={apis}
                loading={loading}
                skeletonCount={pageSize}
                serverSide
                pagination={{
                    page,
                    pageSize,
                    totalCount,
                    pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                    onPageChange,
                    onPageSizeChange: size => {
                        onPageSizeChange(size);
                        onPageChange(1);
                    },
                }}
                emptyMessage="No items to display"
            />
        </div>
    );
}
