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
import { useActionState, useState } from 'react';

import { GroupRoleSelect } from './GroupRoleSelect';
import { FormActionSubmitButton } from '../../../shared/components/FormActionSubmitButton';
import { useOpenRemountKey } from '../../../shared/hooks/useOpenRemountKey';
import { STANDARD_SHEET_WIDTH } from '../../../shared/layout/sheetLayout';
import { isValidEmail } from '../../../shared/utils/email';
import type { GroupMember, GroupRole } from '../types/group';
import { PRIMARY_OWNER_ROLE } from '../types/group';
import { getMemberRoleLockFlags } from '../utils/memberRoles';
import { isPrimaryOwnerUnavailable } from '../utils/primaryOwnership';

type InvitationValues = { email: string; apiRole: string; applicationRole: string };
const PRIMARY_OWNER_DISABLED_OPTIONS = new Set([PRIMARY_OWNER_ROLE]);

type GroupInviteMemberSheetProps = Readonly<{
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
    onSubmit: (values: InvitationValues) => Promise<void>;
}>;

export function GroupInviteMemberSheet(props: GroupInviteMemberSheetProps) {
    const resetKey = useOpenRemountKey(props.open, props.groupName);
    return <GroupInviteMemberSheetContent key={resetKey} {...props} />;
}

function GroupInviteMemberSheetContent({
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
}: GroupInviteMemberSheetProps) {
    const [email, setEmail] = useState('');
    const [apiRole, setApiRole] = useState(() => groupRoles?.API ?? 'USER');
    const [applicationRole, setApplicationRole] = useState(() => groupRoles?.APPLICATION ?? 'USER');
    const [, submitInvitation, isPending] = useActionState<null, FormData>(async () => {
        if (!isValidEmail(email)) return null;
        await onSubmit({ email: email.trim(), apiRole, applicationRole });
        return null;
    }, null);

    const apiPrimaryOwnerDisabled =
        isPrimaryOwnerUnavailable(apiPrimaryOwnerMode) || members.some(m => m.roles?.API === PRIMARY_OWNER_ROLE);
    const roleLocks = getMemberRoleLockFlags({ lockApiRole, lockApiProductRole: false, lockApplicationRole }, canOverrideLocks);

    const canSubmit = isValidEmail(email);
    const emailInvalid = email.trim().length > 0 && !canSubmit;

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && !isPending && onClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>Email invitation</SheetTitle>
                    <SheetDescription>Invite a new user to {groupName} and assign their default roles.</SheetDescription>
                </SheetHeader>

                <form action={submitInvitation} className="flex min-h-0 flex-1 flex-col">
                    <div className="flex-1 space-y-6 px-4">
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
                                disabled={isPending}
                                aria-invalid={emailInvalid}
                                aria-describedby={emailInvalid ? 'invite-email-error' : undefined}
                            />
                            {emailInvalid && (
                                <p id="invite-email-error" className="text-sm text-destructive">
                                    Enter a valid email
                                </p>
                            )}
                        </div>

                        <GroupRoleSelect
                            id="invite-api-role"
                            label="Default API role"
                            roles={apiRoles}
                            value={apiRole}
                            onChange={setApiRole}
                            disabled={roleLocks.api || isPending}
                            disabledOptionNames={apiPrimaryOwnerDisabled ? PRIMARY_OWNER_DISABLED_OPTIONS : undefined}
                            disableSystemRoles="except-primary-owner"
                        />

                        <GroupRoleSelect
                            id="invite-application-role"
                            label="Default application role"
                            roles={applicationRoles}
                            value={applicationRole}
                            onChange={setApplicationRole}
                            disabled={roleLocks.application || isPending}
                            disableSystemRoles="all"
                        />
                    </div>

                    <SheetFooter className="shrink-0 flex-row justify-end border-t">
                        <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
                            Cancel
                        </Button>
                        <FormActionSubmitButton disabled={!canSubmit} label="Send invitation" pendingLabel="Sending…" />
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    );
}
