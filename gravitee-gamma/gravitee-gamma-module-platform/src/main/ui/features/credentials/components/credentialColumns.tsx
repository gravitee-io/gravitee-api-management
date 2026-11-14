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
import { Badge, DataTableColumnHeader, DateCell, type DataTableProps } from '@gravitee/graphene-core';
import { Link } from 'react-router-dom';

import { CredentialActionsMenu } from './CredentialActionsMenu';
import type { Credential } from '../types/credential';
import { isCredentialExpired } from '../utils/credentialExpiry';
import { credentialOwnerLabel } from '../utils/credentialOwner';
import { credentialStrategyLabel } from '../utils/credentialStrategies';

function Empty() {
    return <span className="text-sm text-muted-foreground">—</span>;
}

export const credentialColumns: DataTableProps<Credential>['columns'] = [
    {
        id: 'name',
        accessorKey: 'name',
        enableSorting: false,
        enableHiding: false,
        meta: { label: 'Name' },
        header: ({ column }) => <DataTableColumnHeader column={column} title="Name" />,
        cell: ({ row }) => (
            <Link
                to={row.original.id}
                className="rounded-lg font-medium outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
            >
                {row.original.name}
            </Link>
        ),
    },
    {
        id: 'service',
        accessorKey: 'serviceId',
        enableSorting: false,
        meta: { label: 'Service' },
        header: ({ column }) => <DataTableColumnHeader column={column} title="Service" />,
        cell: ({ row }) =>
            row.original.serviceId ? (
                <span className="text-sm">{row.original.serviceId}</span>
            ) : (
                <span className="text-sm text-muted-foreground">Not set</span>
            ),
    },
    {
        id: 'type',
        accessorKey: 'strategy',
        enableSorting: false,
        meta: { label: 'Type' },
        header: ({ column }) => <DataTableColumnHeader column={column} title="Type" />,
        cell: ({ row }) => <Badge variant="outline">{credentialStrategyLabel(row.original.strategy)}</Badge>,
    },
    {
        id: 'expiresAt',
        accessorKey: 'expiresAt',
        enableSorting: false,
        meta: { label: 'Expires' },
        header: ({ column }) => <DataTableColumnHeader column={column} title="Expires" />,
        cell: ({ row }) =>
            row.original.expiresAt ? (
                <div className="flex items-center gap-2">
                    <DateCell value={new Date(row.original.expiresAt)} format="absolute" />
                    {isCredentialExpired(row.original) ? <Badge variant="destructive">Expired</Badge> : null}
                </div>
            ) : (
                <Empty />
            ),
    },
    {
        id: 'owner',
        accessorKey: 'ownerName',
        enableSorting: false,
        meta: { label: 'Owner' },
        header: ({ column }) => <DataTableColumnHeader column={column} title="Owner" />,
        cell: ({ row }) => {
            const owner = credentialOwnerLabel(row.original);
            return owner ? <span className="text-sm">{owner}</span> : <Empty />;
        },
    },
    {
        id: 'actions',
        header: () => <span className="sr-only">Actions</span>,
        size: 60,
        enableSorting: false,
        enableHiding: false,
        cell: ({ row }) => <CredentialActionsMenu credential={row.original} />,
    },
];
