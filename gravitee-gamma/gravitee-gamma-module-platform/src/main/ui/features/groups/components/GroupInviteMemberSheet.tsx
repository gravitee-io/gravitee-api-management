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
    Button,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import type { GroupRole } from '../types/group';

// Radix Select's controlled `value` can't be an empty string, so unset state is represented by this
// sentinel internally — but classic's invite-member-dialog.component.html has no "None" mat-option (both
// default role controls always start from a real value, see initializeForm()), so it isn't rendered below.
const NO_ROLE_VALUE = '__none__';

// Backend only accepts api_role/application_role on an invitation — no api_product/integration/cluster,
// unlike direct membership roles (GroupInvitationPayload in types/group.ts documents this).
export function GroupInviteMemberSheet({
    open,
    groupName,
    groupRoles,
    apiRoles,
    applicationRoles,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    groupName: string;
    /** The group's own configured default roles (`GroupEntity.roles`) — pre-fills API/Application below,
     *  mirroring classic InviteMemberDialogComponent's `group.roles['API'] ?? 'USER'` fallback. */
    groupRoles: Record<string, string> | undefined;
    apiRoles: GroupRole[];
    applicationRoles: GroupRole[];
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

    function handleClose() {
        onClose();
    }

    const canSubmit = email.trim().length > 0;

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && handleClose()}>
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
                        />
                    </div>

                    <div className="space-y-1.5">
                        <Label className="text-sm font-medium">Default API role</Label>
                        <Select value={apiRole || NO_ROLE_VALUE} onValueChange={v => setApiRole(v === NO_ROLE_VALUE ? '' : v)}>
                            <SelectTrigger className="w-full">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {apiRoles.map(role => (
                                    <SelectItem key={role.name} value={role.name}>
                                        {role.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <div className="space-y-1.5">
                        <Label className="text-sm font-medium">Default application role</Label>
                        <Select
                            value={applicationRole || NO_ROLE_VALUE}
                            onValueChange={v => setApplicationRole(v === NO_ROLE_VALUE ? '' : v)}
                        >
                            <SelectTrigger className="w-full">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {applicationRoles.map(role => (
                                    <SelectItem key={role.name} value={role.name}>
                                        {role.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                </div>

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isSaving}>
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
