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
import { Badge, DataTable, type DataTableProps } from '@gravitee/graphene-core';
import { ShieldCheckIcon } from '@gravitee/graphene-core/icons';

import type { ColCell } from '../../../shared/utils/dataTableTypes';
import { MemberAvatar } from '../../shared/components';
import { NON_SORTABLE_COLUMN } from '../../shared/utils/dataTableHeaders';
import { formatRoleLabel, PRIMARY_OWNER_ROLE } from '../../shared/utils/memberRoles';
import type { IntegrationMember } from '../types/integrationMembers';

const INTEGRATION_SCOPE = 'INTEGRATION';

function isPrimaryOwner(member: IntegrationMember): boolean {
    return member.roles?.some(role => role.name === PRIMARY_OWNER_ROLE) ?? false;
}

function integrationRoleName(member: IntegrationMember): string | undefined {
    return member.roles?.find(role => role.scope === INTEGRATION_SCOPE)?.name ?? member.roles?.[0]?.name;
}

function RoleBadge({ member }: Readonly<{ member: IntegrationMember }>) {
    if (isPrimaryOwner(member)) {
        return (
            <Badge variant="default" className="gap-1 font-normal">
                <ShieldCheckIcon className="size-3" aria-hidden="true" />
                Primary Owner
            </Badge>
        );
    }
    const roleName = integrationRoleName(member);
    return (
        <Badge variant="secondary" className="font-normal">
            {roleName ? formatRoleLabel(roleName) : '—'}
        </Badge>
    );
}

const COLUMNS: DataTableProps<IntegrationMember>['columns'] = [
    {
        id: 'name',
        accessorFn: (row: IntegrationMember) => row.displayName ?? '',
        header: 'Name',
        ...NON_SORTABLE_COLUMN,
        cell: ({ row }: ColCell<IntegrationMember>) => (
            <div className="flex items-center gap-3">
                <MemberAvatar name={row.original.displayName ?? ''} />
                <span className="text-sm font-medium">{row.original.displayName}</span>
            </div>
        ),
    },
    {
        id: 'role',
        header: 'Role',
        ...NON_SORTABLE_COLUMN,
        cell: ({ row }: ColCell<IntegrationMember>) => <RoleBadge member={row.original} />,
    },
];

export function IntegrationDirectMembersTable({ members }: Readonly<{ members: IntegrationMember[] }>) {
    return <DataTable columns={COLUMNS} data={members} />;
}
