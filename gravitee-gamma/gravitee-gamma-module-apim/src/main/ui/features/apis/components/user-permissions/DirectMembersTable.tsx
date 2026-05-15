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
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, ShieldCheckIcon, Trash2Icon, XIcon } from '@gravitee/graphene-core/icons';

import { MemberAvatar } from './MemberAvatar';
import { getApiRole, isMemberPrimaryOwner, type EditState } from './memberHelpers';
import type { Member } from '../../types/members.types';

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
            {roleName || '—'}
        </Badge>
    );
}

export function DirectMembersTable({
    members,
    roles,
    editState,
    onStartEdit,
    onRoleChange,
    onSaveRole,
    onCancelEdit,
    onRemove,
    isSaving,
    isRemoving,
    getRoleName,
}: Readonly<{
    members: Member[];
    roles: string[];
    editState: EditState;
    onStartEdit: (member: Member) => void;
    onRoleChange: (role: string) => void;
    onSaveRole: () => void;
    onCancelEdit: () => void;
    onRemove: (member: Member) => void;
    isSaving: boolean;
    isRemoving: boolean;
    getRoleName?: (member: Member) => string;
}>) {
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
                        const isEditing = editState?.memberId === member.id;
                        const currentRole = getRoleName ? getRoleName(member) : getApiRole(member);

                        return (
                            <TableRow key={member.id} className={isEditing ? 'bg-muted/30' : undefined}>
                                <TableCell>
                                    <div className="flex items-center gap-3">
                                        <MemberAvatar name={member.displayName ?? ''} />
                                        <span className="text-sm font-medium">{member.displayName}</span>
                                    </div>
                                </TableCell>

                                <TableCell className="w-[32%]">
                                    {isEditing ? (
                                        <div className="flex items-center gap-1.5">
                                            <Select value={editState.role} onValueChange={onRoleChange}>
                                                <SelectTrigger className="h-8 flex-1 min-w-0">
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    {roles.map(role => (
                                                        <SelectItem key={role} value={role}>
                                                            {role}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                            <Button
                                                type="button"
                                                size="sm"
                                                className="h-8 shrink-0"
                                                onClick={onSaveRole}
                                                disabled={isSaving || !editState.role}
                                            >
                                                Save
                                            </Button>
                                            <Button
                                                type="button"
                                                size="sm"
                                                variant="ghost"
                                                className="size-8 p-0 shrink-0"
                                                onClick={onCancelEdit}
                                                aria-label="Cancel edit"
                                            >
                                                <XIcon className="size-4" aria-hidden="true" />
                                            </Button>
                                        </div>
                                    ) : (
                                        <RoleBadge roleName={currentRole} isPO={isPO} />
                                    )}
                                </TableCell>

                                <TableCell className="text-right">
                                    {!isPO && !isEditing ? (
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
                                                <DropdownMenuItem onSelect={() => onStartEdit(member)}>
                                                    <PencilIcon className="size-4" />
                                                    Edit role
                                                </DropdownMenuItem>
                                                <DropdownMenuSeparator />
                                                <DropdownMenuItem
                                                    onSelect={() => onRemove(member)}
                                                    disabled={isRemoving}
                                                    className="text-destructive focus:text-destructive"
                                                >
                                                    <Trash2Icon className="size-4" />
                                                    Remove member
                                                </DropdownMenuItem>
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
