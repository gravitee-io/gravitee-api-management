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

import { Button, Input, Label, Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import { GroupRoleSelect } from './GroupRoleSelect';
import type { GroupMember, GroupRole } from '../types/group';
import { PRIMARY_OWNER_ROLE } from '../types/group';
import { getMemberRoleLockFlags } from '../utils/memberRoles';
import { isPrimaryOwnerUnavailable } from '../utils/primaryOwnership';

export function GroupInviteMemberSheet({
    open,
    groupName,
    groupRoles,
    members,
    apiRoles,
    applicationRoles,
    lockApiRole,
    lockApplicationRole,
    canOverrideLocks,
    apiPrimaryOwnerMode,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    groupName: string;
    groupRoles: Record<string, string> | undefined;
    members: GroupMember[];
    apiRoles: GroupRole[];
    applicationRoles: GroupRole[];
    lockApiRole: boolean;
    lockApplicationRole: boolean;
    canOverrideLocks: boolean;
    apiPrimaryOwnerMode?: string;
    onClose: () => void;
    onSubmit: (values: { email: string; apiRole: string; applicationRole: string }) => void;
    isSaving: boolean;
}>) {
    const [email, setEmail] = useState('');
    const [apiRole, setApiRole] = useState('');
    const [applicationRole, setApplicationRole] = useState('');

    useEffect(() => {
        if (open) {
            setEmail('');
            setApiRole(groupRoles?.API ?? 'USER');
            setApplicationRole(groupRoles?.APPLICATION ?? 'USER');
        }
    }, [open, groupRoles]);

    const apiPrimaryOwnerDisabled =
        isPrimaryOwnerUnavailable(apiPrimaryOwnerMode) || members.some(m => m.roles?.API === PRIMARY_OWNER_ROLE);
    const roleLocks = getMemberRoleLockFlags({ lockApiRole, lockApiProductRole: false, lockApplicationRole }, canOverrideLocks);

    const canSubmit = email.trim().length > 0;

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && !isSaving && onClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Email invitation</SheetTitle>
                    <SheetDescription>Invite a new user to {groupName} and assign their default roles.</SheetDescription>
                </SheetHeader>

                <div className="space-y-6 px-4">
                    <div className="space-y-1.5">
                        <Label htmlFor="invite-email" className="text-sm font-medium">
                            Email <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="invite-email"
                            type="email"
                            placeholder="user@example.com"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            disabled={isSaving}
                        />
                    </div>

                    <GroupRoleSelect
                        id="invite-api-role"
                        label="Default API role"
                        roles={apiRoles}
                        value={apiRole}
                        onChange={setApiRole}
                        disabled={roleLocks.api || isSaving}
                        disabledOptionNames={apiPrimaryOwnerDisabled ? new Set([PRIMARY_OWNER_ROLE]) : undefined}
                        disableSystemRoles="except-primary-owner"
                    />

                    <GroupRoleSelect
                        id="invite-application-role"
                        label="Default application role"
                        roles={applicationRoles}
                        value={applicationRole}
                        onChange={setApplicationRole}
                        disabled={roleLocks.application || isSaving}
                        disableSystemRoles="all"
                    />
                </div>

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button
                        type="button"
                        onClick={() => onSubmit({ email: email.trim(), apiRole, applicationRole })}
                        disabled={!canSubmit || isSaving}
                    >
                        {isSaving ? 'Sending…' : 'Send invitation'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
