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
    Alert,
    AlertDescription,
    Button,
    Checkbox,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';

import { GroupScopedRoleSelects } from './GroupScopedRoleSelects';
import { MemberSuccessorCombobox } from './MemberSuccessorCombobox';
import { useGroupEditMemberForm } from '../hooks/useGroupEditMemberForm';
import type { GroupMember, GroupMembershipPayload, GroupRole } from '../types/group';

export function GroupEditMemberSheet({
    open,
    groupName,
    member,
    members,
    apiRoles,
    applicationRoles,
    apiProductRoles,
    integrationRoles,
    clusterRoles,
    lockApiRole,
    lockApiProductRole,
    lockApplicationRole,
    canOverrideLocks,
    groupAllowsGroupAdmin,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    groupName: string;
    member: GroupMember | undefined;
    members: GroupMember[];
    apiRoles: GroupRole[];
    applicationRoles: GroupRole[];
    apiProductRoles: GroupRole[];
    integrationRoles: GroupRole[];
    clusterRoles: GroupRole[];
    lockApiRole: boolean;
    lockApiProductRole: boolean;
    lockApplicationRole: boolean;
    canOverrideLocks: boolean;
    groupAllowsGroupAdmin: boolean;
    onClose: () => void;
    onSubmit: (memberships: GroupMembershipPayload[]) => void;
    isSaving: boolean;
}>) {
    const form = useGroupEditMemberForm({
        open,
        member,
        members,
        lockApiRole,
        lockApiProductRole,
        lockApplicationRole,
        canOverrideLocks,
        onSubmit,
    });

    if (!member) return null;

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Edit roles</SheetTitle>
                    <SheetDescription>
                        Update {member.displayName}&rsquo;s roles in {groupName}.
                    </SheetDescription>
                </SheetHeader>

                <div className="space-y-6 px-4 pb-4">
                    <GroupScopedRoleSelects
                        roles={{
                            api: apiRoles,
                            apiProduct: apiProductRoles,
                            application: applicationRoles,
                            integration: integrationRoles,
                            cluster: clusterRoles,
                        }}
                        values={form.roleValues}
                        onChange={form.handleRoleChange}
                        locks={form.roleLocks}
                    />

                    <div className="space-y-1.5">
                        <label
                            htmlFor="edit-member-group-admin"
                            className="flex items-center gap-2.5 cursor-pointer aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
                            aria-disabled={!groupAllowsGroupAdmin}
                        >
                            <Checkbox
                                id="edit-member-group-admin"
                                checked={form.groupAdmin}
                                disabled={!groupAllowsGroupAdmin}
                                onCheckedChange={checked => form.setGroupAdmin(checked === true)}
                            />
                            <span className="text-sm select-none">Group admin</span>
                        </label>
                        <p className="text-xs text-muted-foreground">
                            {groupAllowsGroupAdmin
                                ? 'Lets this member manage the group’s membership themselves.'
                                : 'Enable "Allow adding members via user search" on this group to grant group admin access.'}
                        </p>
                    </div>

                    {form.transfer?.needsSuccessor && (
                        <MemberSuccessorCombobox
                            id="edit-member-successor"
                            candidates={form.successorCandidates}
                            value={form.selectedSuccessor}
                            onChange={picked => form.setSelectedSuccessorId(picked?.id ?? null)}
                            hint="Select a member to transfer primary ownership."
                        />
                    )}

                    {form.transferMessage && (
                        <Alert variant="default">
                            <InfoIcon className="size-4" aria-hidden />
                            <AlertDescription>{form.transferMessage}</AlertDescription>
                        </Alert>
                    )}
                </div>

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={form.handleSubmit} disabled={isSaving || !form.canSubmit}>
                        {isSaving ? 'Saving…' : 'Save'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
