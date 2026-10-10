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

import { buildModuleNavPath } from '@gravitee/gamma-modules-sdk/routing';
import { Button, DataTable, type DataTableProps } from '@gravitee/graphene-core';
import { Link, useLocation } from 'react-router-dom';

import type { ColCell } from '../../../shared/utils/dataTableTypes';
import type { IngestedApi } from '../types/integration';
import { SMALLEST_TABLE_PAGE_SIZE, TABLE_PAGE_SIZE_OPTIONS } from '../utils/paginationConstants';

function IngestedApiLink({ api }: Readonly<{ api: IngestedApi }>) {
    const { pathname } = useLocation();

    return (
        <Button asChild variant="link" className="h-auto p-0 text-left text-sm font-medium text-foreground hover:underline">
            <Link to={buildModuleNavPath('apim', `apis/${encodeURIComponent(api.id)}`, pathname)}>{api.name}</Link>
        </Button>
    );
}

const COLUMNS: DataTableProps<IngestedApi>['columns'] = [
    {
        id: 'name',
        accessorKey: 'name',
        enableSorting: false,
        header: 'Name',
        cell: ({ row }: ColCell<IngestedApi>) => <IngestedApiLink api={row.original} />,
    },
    {
        id: 'version',
        accessorKey: 'version',
        enableSorting: false,
        header: 'Version',
    },
];

interface IngestedApisTableProps {
    readonly apis: IngestedApi[];
    readonly totalCount: number;
    readonly page: number;
    readonly pageSize: number;
    readonly loading: boolean;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
}

export function IngestedApisTable({ apis, totalCount, page, pageSize, loading, onPageChange, onPageSizeChange }: IngestedApisTableProps) {
    const pagination =
        totalCount > SMALLEST_TABLE_PAGE_SIZE
            ? {
                  page,
                  pageSize,
                  totalCount,
                  pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                  onPageChange,
                  onPageSizeChange: (size: number) => {
                      onPageSizeChange(size);
                      onPageChange(1);
                  },
              }
            : undefined;

    return (
        <DataTable
            aria-label="Ingested APIs"
            columns={COLUMNS}
            data={apis}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            pagination={pagination}
        />
    );
}
