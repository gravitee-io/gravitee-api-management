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
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { UsersIcon } from '@gravitee/graphene-core/icons';

import { MemberAvatar } from './MemberAvatar';
import { getGroupMemberRole } from './memberHelpers';
import type { GroupMember } from '../../types/applicationMembers.types';

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
                <div className="rounded-lg border overflow-hidden">
                    <Table className="table-fixed w-full">
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>
                                <TableHead style={{ width: '32%' }}>Role</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {isLoading ? (
                                Array.from({ length: 2 }).map((_, i) => (
                                    <TableRow key={`skeleton-${i}`}>
                                        <TableCell colSpan={2}>
                                            <Skeleton className="h-10 w-full rounded" />
                                        </TableCell>
                                    </TableRow>
                                ))
                            ) : members.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={2} className="text-sm text-muted-foreground">
                                        No members in this group.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                members.map(m => (
                                    <TableRow key={m.id}>
                                        <TableCell>
                                            <div className="flex items-center gap-3">
                                                <MemberAvatar name={m.displayName ?? ''} />
                                                <span className="text-sm font-medium">{m.displayName}</span>
                                            </div>
                                        </TableCell>
                                        <TableCell style={{ width: '32%' }}>
                                            <Badge variant="secondary" className="font-normal">
                                                {getGroupMemberRole(m)}
                                            </Badge>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </div>
            </CardContent>
        </Card>
    );
}
