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
    Label,
    ScrollArea,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { useActionState } from 'react';

import { GroupScopedRoleSelects } from './GroupScopedRoleSelects';
import { GroupUserSearchPicker } from './GroupUserSearchPicker';
import { FormActionSubmitButton } from '../../../shared/components/FormActionSubmitButton';
import { STANDARD_SHEET_WIDTH } from '../../../shared/layout/sheetLayout';
import { useGroupAddMembersForm } from '../hooks/useGroupAddMembersForm';
import type { GroupMember, GroupMembershipPayload, GroupRole } from '../types/group';

type GroupAddMembersSheetProps = Readonly<{
    open: boolean;
    groupName: string;
    groupRoles: Record<string, string> | undefined;
    members: GroupMember[];
    apiRoles: GroupRole[];
    applicationRoles: GroupRole[];
    apiProductRoles: GroupRole[];
    integrationRoles: GroupRole[];
    clusterRoles: GroupRole[];
    explorerRoles: GroupRole[];
    lockApiRole: boolean;
    lockApiProductRole: boolean;
    lockApplicationRole: boolean;
    canOverrideLocks: boolean;
    maxInvitation: number | null;
    apiPrimaryOwnerMode?: string;
    apiProductPrimaryOwnerMode?: string;
    initialSearch?: string;
    onClose: () => void;
    onSubmit: (memberships: GroupMembershipPayload[]) => Promise<void>;
}>;

export function GroupAddMembersSheet(props: GroupAddMembersSheetProps) {
    const resetKey = `${props.open ? 'open' : 'closed'}-${props.groupName}-${props.initialSearch ?? ''}`;
    return <GroupAddMembersSheetContent key={resetKey} {...props} />;
}

function GroupAddMembersSheetContent({
    open,
    groupName,
    groupRoles,
    members,
    apiRoles,
    applicationRoles,
    apiProductRoles,
    integrationRoles,
    clusterRoles,
    explorerRoles,
    lockApiRole,
    lockApiProductRole,
    lockApplicationRole,
    canOverrideLocks,
    maxInvitation,
    apiPrimaryOwnerMode,
    apiProductPrimaryOwnerMode,
    initialSearch,
    onClose,
    onSubmit,
}: GroupAddMembersSheetProps) {
    const form = useGroupAddMembersForm({
        open,
        groupRoles,
        members,
        lockApiRole,
        lockApiProductRole,
        lockApplicationRole,
        canOverrideLocks,
        maxInvitation,
        apiPrimaryOwnerMode,
        apiProductPrimaryOwnerMode,
        initialSearch,
        onSubmit,
    });
    const [, submitMembers, isPending] = useActionState<null, FormData>(async () => {
        if (!form.canSubmit) return null;
        await form.handleSubmit();
        return null;
    }, null);

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && !isPending && onClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>Add members</SheetTitle>
                    <SheetDescription>Search platform users and assign roles for membership in {groupName}.</SheetDescription>
                </SheetHeader>

                <form action={submitMembers} className="flex min-h-0 flex-1 flex-col">
                    <ScrollArea className="min-h-0 flex-1">
                        <div className="space-y-6 px-4 pb-4">
                            <div className="space-y-3">
                                <Label className="text-sm font-medium">Default roles for selected users</Label>
                                <GroupScopedRoleSelects
                                    idPrefix="add-members-role"
                                    roles={{
                                        api: apiRoles,
                                        apiProduct: apiProductRoles,
                                        application: applicationRoles,
                                        integration: integrationRoles,
                                        cluster: clusterRoles,
                                        explorer: explorerRoles,
                                    }}
                                    values={form.roleValues}
                                    onChange={form.handleRoleChange}
                                    locks={form.roleLocks}
                                    disabled={isPending}
                                    disabledOptionNames={form.disabledOptionNames}
                                />
                            </div>

                            {form.primaryOwnerSelected && (
                                <p className="text-xs text-muted-foreground">
                                    The primary owner role can be granted to a single member only.
                                </p>
                            )}

                            <GroupUserSearchPicker
                                search={form.search}
                                onSearchChange={form.setSearch}
                                debouncedQuery={form.debouncedQuery}
                                isFetching={form.isFetching}
                                candidates={form.candidates}
                                selected={form.selected}
                                onToggle={form.handleToggle}
                                invitationLimitReached={form.invitationLimitReached}
                                groupMemberCapReached={form.groupMemberCapReached}
                                disabled={isPending}
                            />

                            {form.selected.length > 0 && (
                                <p className="text-xs text-muted-foreground">
                                    {form.selected.length} user{form.selected.length !== 1 ? 's' : ''} selected
                                </p>
                            )}
                        </div>
                    </ScrollArea>

                    <SheetFooter className="shrink-0 flex-row justify-end border-t">
                        <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
                            Cancel
                        </Button>
                        <FormActionSubmitButton disabled={!form.canSubmit} label={form.submitLabel} pendingLabel="Adding…" />
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    );
}
