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
    type DataTableProps,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { Link } from 'react-router-dom';

import { PortalLogo } from './PortalLogo';
import { ViewPortalLink } from './ViewPortalLink';
import type { Portal } from '../types/portal';

interface PortalTableProps {
    readonly portals: Portal[];
    readonly canConfigure: boolean;
    readonly onRowClick?: (portal: Portal) => void;
    readonly getOverviewPath: (portalId: string) => string;
}

export function PortalTable({ portals, canConfigure, getOverviewPath, onRowClick }: PortalTableProps) {
    const renderIdentity = (portal: Portal) => (
        <div className="flex items-center gap-3 min-w-0">
            <PortalLogo name={portal.name} src={portal.logoUrl} />
            <span className="font-medium break-words">{portal.name}</span>
            {portal.default ? <Badge variant="secondary">Default</Badge> : null}
        </div>
    );
    const renderActions = (portal: Portal) => (
        <div className="flex flex-wrap justify-end gap-2">
            <ViewPortalLink url={portal.url} portalName={portal.name} />
            {canConfigure ? (
                <Button variant="ghost" size="sm" asChild>
                    <Link to={getOverviewPath(portal.id)} aria-label={`Configure ${portal.name}`}>
                        Configure
                    </Link>
                </Button>
            ) : null}
        </div>
    );
    const renderUrl = (portal: Portal) => (
        <span className="block w-64 max-w-xs break-all text-muted-foreground">{portal.url || 'No public URL yet'}</span>
    );
    const columns: DataTableProps<Portal>['columns'] = [
        {
            accessorKey: 'name',
            header: 'Portal',
            enableSorting: false,
            cell: ({ row }) => renderIdentity(row.original),
        },
        {
            accessorKey: 'url',
            header: 'URL',
            enableSorting: false,
            cell: ({ row }) => renderUrl(row.original),
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
            cell: ({ row }) => renderActions(row.original),
        },
    ];

    if (onRowClick) {
        return (
            <Table aria-label="Portals">
                <TableHeader>
                    <TableRow>
                        <TableHead>Portal</TableHead>
                        <TableHead>URL</TableHead>
                        <TableHead>Published APIs</TableHead>
                        <TableHead>
                            <span className="sr-only">Actions</span>
                        </TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {portals.map(portal => (
                        <TableRow
                            key={portal.id}
                            tabIndex={0}
                            aria-label={`Open portal ${portal.name}`}
                            className="cursor-pointer focus-visible:ring-ring/50 focus-visible:ring-[3px] focus-visible:outline-none"
                            onClick={event => {
                                if (event.target instanceof Element && event.target.closest('a, button')) return;
                                onRowClick(portal);
                            }}
                            onKeyDown={event => {
                                if (event.key === 'Enter' && event.target === event.currentTarget) {
                                    event.preventDefault();
                                    onRowClick(portal);
                                }
                            }}
                        >
                            <TableCell>{renderIdentity(portal)}</TableCell>
                            <TableCell>{renderUrl(portal)}</TableCell>
                            <TableCell>{portal.publishedApisCount}</TableCell>
                            <TableCell>{renderActions(portal)}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        );
    }

    return <DataTable aria-label="Portals" columns={columns} data={portals} serverSide />;
}
