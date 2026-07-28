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
    CardHeader,
    CardTitle,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Skeleton,
} from '@gravitee/graphene-core';
import { UsersIcon } from '@gravitee/graphene-core/icons';

import type { OrganizationUserGroup } from '../types/user';

interface UserGroupMembershipsCardProps {
    readonly groups: OrganizationUserGroup[];
    readonly loading: boolean;
}

export function UserGroupMembershipsCard({ groups, loading }: UserGroupMembershipsCardProps) {
    return (
        <Card>
            <CardHeader className="pb-3">
                <CardTitle className="text-base">Group Memberships</CardTitle>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <div className="space-y-2">
                        {Array.from({ length: 2 }).map((_, index) => (
                            <Skeleton key={index} className="h-10 rounded-lg" />
                        ))}
                    </div>
                ) : groups.length === 0 ? (
                    <Empty className="border-none p-0">
                        <EmptyHeader>
                            <UsersIcon className="size-8 text-muted-foreground" aria-hidden />
                            <EmptyTitle>Not a member of any groups</EmptyTitle>
                            <EmptyDescription>Group memberships grant shared access across APIs and applications.</EmptyDescription>
                        </EmptyHeader>
                    </Empty>
                ) : (
                    <ul className="space-y-2">
                        {groups.map(group => (
                            <li key={group.id} className="flex items-center gap-2 rounded-lg border px-3 py-2">
                                <Badge variant="outline">{group.name ?? group.id}</Badge>
                            </li>
                        ))}
                    </ul>
                )}
            </CardContent>
        </Card>
    );
}
