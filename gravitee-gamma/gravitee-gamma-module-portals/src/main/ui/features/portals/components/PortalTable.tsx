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
import { Badge, Button, DataTable, type DataTableProps } from '@gravitee/graphene-core';
import { Link } from 'react-router-dom';

import { PortalLogo } from './PortalLogo';
import { ViewPortalLink } from './ViewPortalLink';
import type { Portal } from '../types/portal';

interface PortalTableProps {
    readonly portals: Portal[];
    readonly canConfigure: boolean;
    readonly getOverviewPath: (portalId: string) => string;
}

export function PortalTable({ portals, canConfigure, getOverviewPath }: PortalTableProps) {
    const columns: DataTableProps<Portal>['columns'] = [
        {
            accessorKey: 'name',
            header: 'Portal',
            enableSorting: false,
            cell: ({ row }) => (
                <div className="flex items-center gap-3 min-w-0">
                    <PortalLogo name={row.original.name} src={row.original.logoUrl} />
                    <span className="font-medium break-words">{row.original.name}</span>
                    {row.original.default ? <Badge variant="secondary">Default</Badge> : null}
                </div>
            ),
        },
        {
            accessorKey: 'url',
            header: 'URL',
            enableSorting: false,
            cell: ({ row }) => (
                <span className="block w-64 max-w-xs break-all text-muted-foreground">{row.original.url || 'No public URL yet'}</span>
            ),
        },
        {
            accessorKey: 'publishedApisCount',
            header: 'Published APIs',
            enableSorting: false,
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            enableSorting: false,
            cell: ({ row }) => (
                <div className="flex flex-wrap justify-end gap-2">
                    <ViewPortalLink url={row.original.url} portalName={row.original.name} />
                    {canConfigure ? (
                        <Button variant="ghost" size="sm" asChild>
                            <Link to={getOverviewPath(row.original.id)} aria-label={`Configure ${row.original.name}`}>
                                Configure
                            </Link>
                        </Button>
                    ) : null}
                </div>
            ),
        },
    ];

    return <DataTable aria-label="Portals" columns={columns} data={portals} serverSide />;
}
