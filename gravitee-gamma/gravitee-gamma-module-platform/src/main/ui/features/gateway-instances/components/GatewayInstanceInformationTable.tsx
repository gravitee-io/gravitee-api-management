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

import { DataTable, InputGroup, InputGroupAddon, InputGroupInput, cn, type DataTableProps } from '@gravitee/graphene-core';
import {
    ActivityIcon,
    CircleHelpIcon,
    CircleStopIcon,
    ClockIcon,
    FileTextIcon,
    InfoIcon,
    ListIcon,
    MonitorIcon,
    NetworkIcon,
    PlayIcon,
    SearchIcon,
    ServerIcon,
    UsersIcon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, type ReactNode } from 'react';

import type { ColCell } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import { useClientFilteredPagination } from '../hooks/useClientFilteredPagination';
import type { InformationRow } from '../utils/buildInformationRows';

function rowIcon(icon: InformationRow['icon']): ReactNode {
    const className = 'size-4 text-muted-foreground';
    switch (icon) {
        case 'hostname':
            return <MonitorIcon className={className} aria-hidden />;
        case 'ip':
        case 'port':
            return <NetworkIcon className={className} aria-hidden />;
        case 'state-started':
            return <PlayIcon className="size-4 text-success" aria-hidden />;
        case 'state-stopped':
            return <CircleStopIcon className="size-4 text-destructive" aria-hidden />;
        case 'state-unknown':
            return <CircleHelpIcon className="size-4 text-muted-foreground" aria-hidden />;
        case 'version':
            return <FileTextIcon className={className} aria-hidden />;
        case 'started':
            return <ClockIcon className={className} aria-hidden />;
        case 'heartbeat':
            return <ActivityIcon className={className} aria-hidden />;
        case 'tags':
            return <ListIcon className={className} aria-hidden />;
        case 'tenant':
            return <ServerIcon className={className} aria-hidden />;
        case 'organizations':
            return <UsersIcon className={className} aria-hidden />;
        case 'environments':
            return <ServerIcon className={className} aria-hidden />;
        case 'stopped':
            return <CircleStopIcon className={className} aria-hidden />;
        case 'os':
            return <MonitorIcon className={className} aria-hidden />;
        default:
            return <InfoIcon className={className} aria-hidden />;
    }
}

function buildColumns(): DataTableProps<InformationRow>['columns'] {
    return [
        {
            id: 'icon',
            accessorKey: 'icon',
            header: '',
            enableSorting: false,
            cell: ({ row }: ColCell<InformationRow>) => rowIcon(row.original.icon),
        },
        {
            id: 'type',
            accessorKey: 'type',
            header: 'Type',
            enableSorting: false,
            cell: ({ row }: ColCell<InformationRow>) => <span className="text-sm font-medium whitespace-nowrap">{row.original.type}</span>,
        },
        {
            id: 'value',
            accessorKey: 'value',
            header: 'Value',
            enableSorting: false,
            cell: ({ row }: ColCell<InformationRow>) => (
                <span
                    className={cn(
                        'text-sm whitespace-pre-line break-all',
                        row.original.tone === 'success' && 'text-success font-medium',
                        row.original.tone === 'danger' && 'text-destructive font-medium',
                        (!row.original.tone || row.original.tone === 'default') && 'text-muted-foreground',
                    )}
                >
                    {row.original.value}
                </span>
            ),
        },
    ];
}

export function GatewayInstanceInformationTable({ rows }: Readonly<{ rows: InformationRow[] }>) {
    const matchesSearch = useCallback(
        (item: InformationRow, query: string) => item.type.toLowerCase().includes(query) || item.value.toLowerCase().includes(query),
        [],
    );
    const { search, page, pageSize, totalCount, pageData, handleSearchChange, setPage, handlePageSizeChange } = useClientFilteredPagination(
        rows,
        matchesSearch,
    );
    const columns = useMemo(() => buildColumns(), []);

    return (
        <section className="space-y-3 rounded-xl border bg-card p-4" data-testid="gateway-instance-information">
            <h2 className="flex items-center gap-2 text-base font-semibold">
                <InfoIcon className="size-5" aria-hidden />
                Information
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
                        aria-label="Search information"
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
                emptyMessage={search.trim() ? 'No information matches your search.' : 'No information'}
            />
        </section>
    );
}
