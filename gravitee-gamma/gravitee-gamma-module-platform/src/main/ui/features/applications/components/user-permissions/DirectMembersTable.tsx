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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, ShieldCheckIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { MemberAvatar } from './MemberAvatar';
import { formatRoleLabel, getApplicationRole, isMemberPrimaryOwner } from './memberHelpers';
import type { ApplicationUiMember } from '../../types/applicationMembers.types';

function RoleBadge({ roleName, isPO }: Readonly<{ roleName: string; isPO: boolean }>) {
    if (isPO) {
        return (
            <Badge className="gap-1 bg-primary/10 text-primary border-transparent font-normal">
                <ShieldCheckIcon className="size-3" aria-hidden="true" />
                Primary Owner
            </Badge>
        );
    }
    return (
        <Badge variant="secondary" className="font-normal">
            {roleName ? formatRoleLabel(roleName) : '—'}
        </Badge>
    );
}

export function DirectMembersTable({
    members,
    onEditRole,
    onRemove,
    isRemoving,
    getRoleName,
    canManageMembers = true,
    canEditRole = true,
    canRemoveMember = true,
}: Readonly<{
    members: ApplicationUiMember[];
    onEditRole: (member: ApplicationUiMember) => void;
    onRemove: (member: ApplicationUiMember) => void;
    isRemoving: boolean;
    getRoleName?: (member: ApplicationUiMember) => string;
    canManageMembers?: boolean;
    canEditRole?: boolean;
    canRemoveMember?: boolean;
}>) {
    const showActionsMenu = canManageMembers && (canEditRole || canRemoveMember);

    return (
        <div className="rounded-lg border overflow-hidden">
            <Table className="table-fixed w-full">
                <TableHeader>
                    <TableRow>
                        <TableHead>Name</TableHead>
                        <TableHead className="w-[32%]">Role</TableHead>
                        <TableHead />
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {members.map(member => {
                        const isPO = isMemberPrimaryOwner(member);
                        const currentRole = getRoleName ? getRoleName(member) : getApplicationRole(member);

                        return (
                            <TableRow key={member.id}>
                                <TableCell>
                                    <div className="flex items-center gap-3">
                                        <MemberAvatar name={member.displayName ?? ''} />
                                        <span className="text-sm font-medium">{member.displayName}</span>
                                    </div>
                                </TableCell>

                                <TableCell className="w-[32%]">
                                    <RoleBadge roleName={currentRole} isPO={isPO} />
                                </TableCell>

                                <TableCell className="text-right">
                                    {showActionsMenu && !isPO ? (
                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="icon"
                                                    className="size-8"
                                                    aria-label={`Actions for ${member.displayName}`}
                                                >
                                                    <MoreHorizontalIcon className="size-4" />
                                                </Button>
                                            </DropdownMenuTrigger>
                                            <DropdownMenuContent align="end" className="w-auto min-w-[12rem]">
                                                {canEditRole ? (
                                                    <DropdownMenuItem onSelect={() => onEditRole(member)}>
                                                        <PencilIcon className="size-4" />
                                                        Edit role
                                                    </DropdownMenuItem>
                                                ) : null}
                                                {canEditRole && canRemoveMember ? <DropdownMenuSeparator /> : null}
                                                {canRemoveMember ? (
                                                    <DropdownMenuItem
                                                        onSelect={() => onRemove(member)}
                                                        disabled={isRemoving}
                                                        className="text-destructive focus:text-destructive"
                                                    >
                                                        <Trash2Icon className="size-4" />
                                                        Remove member
                                                    </DropdownMenuItem>
                                                ) : null}
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    ) : null}
                                </TableCell>
                            </TableRow>
                        );
                    })}
                </TableBody>
            </Table>
        </div>
    );
}
