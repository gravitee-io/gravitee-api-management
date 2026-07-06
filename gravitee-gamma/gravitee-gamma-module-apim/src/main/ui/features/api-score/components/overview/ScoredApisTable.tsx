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
import { Button, cn, DataTable, DataTableEmptyState, type DataTableProps } from '@gravitee/graphene-core';
import { RefreshCwIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';

import { CountBadge, ScorePill } from './ScoreCells';
import { ApiAvatar } from '../../../apis/components/ApiAvatar';
import type { EnvironmentApiScore } from '../../types/apiScore';
import { TABLE_SEVERITY_COLUMNS } from '../../utils/scoreFormat';

type ColCell = { row: { original: EnvironmentApiScore } };

// Backend `GET /scoring/apis` supports pagination only (no sort/filter) — columns are not sortable.
const COLUMNS: DataTableProps<EnvironmentApiScore>['columns'] = [
    {
        id: 'API Name',
        accessorFn: row => row.name,
        header: 'API Name',
        enableSorting: false,
        cell: ({ row }: ColCell) => (
            <div className="flex items-center gap-2.5">
                <ApiAvatar src={row.original.pictureUrl} name={row.original.name} />
                <span className="font-medium">{row.original.name}</span>
            </div>
        ),
    },
    {
        id: 'Score',
        accessorFn: row => row.score ?? -1,
        header: 'Score',
        enableSorting: false,
        cell: ({ row }: ColCell) => <ScorePill score={row.original.score} />,
    },
    ...TABLE_SEVERITY_COLUMNS.map(({ key, label }) => ({
        id: label,
        accessorFn: (row: EnvironmentApiScore) => row[key] ?? -1,
        header: label,
        enableSorting: false,
        cell: ({ row }: ColCell) => <CountBadge severity={key} count={row.original[key]} />,
    })),
];

interface ScoredApisTableProps {
    apis: EnvironmentApiScore[];
    isLoading: boolean;
    isRefreshing?: boolean;
    skeletonRowCount?: number;
    page: number;
    pageSize: number;
    totalCount: number;
    onPageChange: (page: number) => void;
    onPageSizeChange: (pageSize: number) => void;
    onRefresh: () => void;
}

export function ScoredApisTable({
    apis,
    isLoading,
    isRefreshing = false,
    skeletonRowCount = 10,
    page,
    pageSize,
    totalCount,
    onPageChange,
    onPageSizeChange,
    onRefresh,
}: ScoredApisTableProps) {
    const toolbar = (
        <Button
            type="button"
            variant="outline"
            size="sm"
            className="ml-auto shrink-0"
            onClick={onRefresh}
            disabled={isLoading || isRefreshing}
        >
            <RefreshCwIcon className={cn('size-4', isRefreshing && 'animate-spin')} aria-hidden="true" />
            Refresh
        </Button>
    );

    return (
        <DataTable
            aria-label="Scored APIs"
            columns={COLUMNS}
            data={apis}
            loading={isLoading}
            skeletonCount={skeletonRowCount}
            toolbar={toolbar}
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [10, 25, 50, 100],
                onPageChange,
                onPageSizeChange,
            }}
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<ShieldCheckIcon />}
                    title="No API proxies yet"
                    description="Create a V4 HTTP proxy to see it listed here."
                />
            }
        />
    );
}
