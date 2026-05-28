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
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    DataTable,
    Skeleton,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { UsersIcon } from '@gravitee/graphene-core/icons';

import { MemberAvatar } from './MemberAvatar';
import { getGroupMemberRole } from './memberHelpers';
import type { GroupMember } from '../../types/applicationMembers.types';
import { NON_SORTABLE_COLUMN } from '../../utils/dataTableHeaders';
import type { ColCell } from '../../utils/dataTableTypes';

const GROUP_MEMBER_COLUMNS: DataTableProps<GroupMember>['columns'] = [
    {
        id: 'name',
        accessorFn: (row: GroupMember) => row.displayName ?? '',
        header: 'Name',
        ...NON_SORTABLE_COLUMN,
        cell: ({ row }: ColCell<GroupMember>) => (
            <div className="flex items-center gap-3">
                <MemberAvatar name={row.original.displayName ?? ''} />
                <span className="text-sm font-medium">{row.original.displayName}</span>
            </div>
        ),
    },
    {
        id: 'role',
        accessorFn: (row: GroupMember) => getGroupMemberRole(row),
        header: 'Role',
        ...NON_SORTABLE_COLUMN,
        cell: ({ row }: ColCell<GroupMember>) => (
            <Badge variant="secondary" className="font-normal">
                {getGroupMemberRole(row.original)}
            </Badge>
        ),
    },
];

export function GroupMembersSection({
    groupName,
    members,
    isLoading = false,
}: Readonly<{ groupName: string; members: GroupMember[]; isLoading?: boolean }>) {
    const count = members.length;

    return (
        <Card className="overflow-hidden">
            <CardHeader className="pb-3">
                <div className="flex items-center gap-3">
                    <UsersIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <CardTitle className="text-sm">{groupName}</CardTitle>
                    <Badge variant="secondary" className="text-xs tabular-nums">
                        {count} inherited {count === 1 ? 'member' : 'members'}
                    </Badge>
                </div>
                <CardDescription className="text-xs">
                    Members inherited from this group. Permissions are managed at the group level.
                </CardDescription>
            </CardHeader>
            <CardContent>
                {isLoading ? (
                    <div className="space-y-2 rounded-lg border p-3">
                        {Array.from({ length: 2 }).map((_, index) => (
                            <Skeleton key={index} className="h-10 w-full rounded" />
                        ))}
                    </div>
                ) : (
                    <DataTable columns={GROUP_MEMBER_COLUMNS} data={members} emptyMessage="No members in this group." />
                )}
            </CardContent>
        </Card>
    );
}
