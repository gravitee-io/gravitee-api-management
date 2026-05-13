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
import { Badge, Card, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@gravitee/graphene-core';
import { UsersIcon } from '@gravitee/graphene-core/icons';

import { MemberAvatar } from './MemberAvatar';
import type { GroupMember } from '../../types/members.types';

export function GroupMembersSection({ groupName, members }: Readonly<{ groupName: string; members: GroupMember[] }>) {
    const count = members.length;
    return (
        <Card className="overflow-hidden">
            <div className="flex items-center gap-3 px-4 py-3 border-b bg-muted/30">
                <UsersIcon className="size-4 text-muted-foreground shrink-0" aria-hidden="true" />
                <span className="text-sm font-semibold">{groupName}</span>
                <Badge variant="secondary" className="text-xs tabular-nums">
                    {count} inherited {count === 1 ? 'member' : 'members'}
                </Badge>
            </div>
            <p className="px-4 py-2 text-xs text-muted-foreground border-b">
                Members inherited from this group. Permissions are managed at the group level.
            </p>
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Name</TableHead>
                        <TableHead className="w-44">Role</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {members.map(m => (
                        <TableRow key={m.id}>
                            <TableCell>
                                <div className="flex items-center gap-3">
                                    <MemberAvatar name={m.displayName ?? ''} />
                                    <span className="text-sm font-medium">{m.displayName}</span>
                                </div>
                            </TableCell>
                            <TableCell>
                                <Badge variant="secondary" className="font-normal">
                                    {m.roles?.['API'] ?? '—'}
                                </Badge>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Card>
    );
}
