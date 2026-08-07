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

import { DataTable, InputGroup, InputGroupAddon, InputGroupInput, type DataTableProps } from '@gravitee/graphene-core';
import { CableIcon, DatabaseIcon, FolderOpenIcon, PlugIcon, PuzzleIcon, SearchIcon, ShieldIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, type ReactNode } from 'react';

import type { ColCell } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import { useClientFilteredPagination } from '../hooks/useClientFilteredPagination';
import type { Instance } from '../types/instance';

type PluginRow = NonNullable<Instance['plugins']>[number];

function pluginIcon(type: string | undefined): ReactNode {
    const className = 'size-4 text-muted-foreground';
    switch (type) {
        case 'policy':
            return <CableIcon className={className} aria-hidden />;
        case 'service':
        case 'service_discovery':
            return <PuzzleIcon className={className} aria-hidden />;
        case 'repository':
            return <FolderOpenIcon className={className} aria-hidden />;
        case 'reporter':
            return <DatabaseIcon className={className} aria-hidden />;
        case 'resource':
            return <PuzzleIcon className={className} aria-hidden />;
        case 'connector':
        case 'endpoint-connector':
        case 'entrypoint-connector':
            return <PlugIcon className={className} aria-hidden />;
        case 'alert':
            return <ShieldIcon className={className} aria-hidden />;
        default:
            return <PuzzleIcon className={className} aria-hidden />;
    }
}

function buildColumns(): DataTableProps<PluginRow>['columns'] {
    return [
        {
            id: 'icon',
            accessorKey: 'type',
            header: '',
            enableSorting: false,
            cell: ({ row }: ColCell<PluginRow>) => pluginIcon(row.original.type),
        },
        {
            id: 'id',
            accessorKey: 'id',
            header: 'ID',
            enableSorting: false,
            cell: ({ row }: ColCell<PluginRow>) => <span className="font-mono text-sm">{row.original.id}</span>,
        },
        {
            id: 'name',
            accessorKey: 'name',
            header: 'Name',
            enableSorting: false,
            cell: ({ row }: ColCell<PluginRow>) => <span className="text-sm text-muted-foreground">{row.original.name}</span>,
        },
        {
            id: 'version',
            accessorKey: 'version',
            header: 'Version',
            enableSorting: false,
            cell: ({ row }: ColCell<PluginRow>) => (
                <span className="text-sm text-muted-foreground whitespace-nowrap">{row.original.version}</span>
            ),
        },
    ];
}

export function GatewayInstancePluginsTable({ plugins }: Readonly<{ plugins: PluginRow[] }>) {
    const matchesSearch = useCallback(
        (item: PluginRow, query: string) =>
            item.id.toLowerCase().includes(query) || item.name.toLowerCase().includes(query) || item.version.toLowerCase().includes(query),
        [],
    );
    const { search, page, pageSize, totalCount, pageData, handleSearchChange, setPage, handlePageSizeChange } = useClientFilteredPagination(
        plugins,
        matchesSearch,
    );
    const columns = useMemo(() => buildColumns(), []);

    return (
        <section className="space-y-3 rounded-xl border bg-card p-4" data-testid="gateway-instance-plugins">
            <h2 className="flex items-center gap-2 text-base font-semibold">
                <PuzzleIcon className="size-5" aria-hidden />
                Plugins
            </h2>
            <div className="max-w-sm">
                <InputGroup>
                    <InputGroupAddon align="inline-start">
                        <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                    </InputGroupAddon>
                    <InputGroupInput
                        placeholder="Search"
                        value={search}
                        onChange={e => handleSearchChange(e.target.value)}
                        aria-label="Search plugins"
                    />
                </InputGroup>
            </div>
            <DataTable
                columns={columns}
                data={pageData}
                serverSide
                pagination={{
                    page,
                    pageSize,
                    totalCount,
                    pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                    onPageChange: setPage,
                    onPageSizeChange: handlePageSizeChange,
                }}
                emptyMessage={search.trim() ? 'No plugins match your search.' : 'No plugin'}
            />
        </section>
    );
}
