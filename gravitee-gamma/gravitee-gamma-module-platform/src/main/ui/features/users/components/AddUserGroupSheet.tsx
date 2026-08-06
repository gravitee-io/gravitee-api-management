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
    Checkbox,
    Field,
    FieldLabel,
    ScrollArea,
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
import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';

import { GroupMembershipRoleSelect } from './GroupMembershipRoleSelect';
import { STANDARD_SHEET_WIDTH } from '../../applications/components/sheetLayout';
import { useEnvironmentGroups, useGroupMembershipRoleCatalog } from '../hooks/useOrganizationUser';
import type { AddUserGroupMembershipPayload, GroupMembershipRoleCatalogScope, OrganizationRole } from '../types/user';
import {
    EMPTY_ADD_USER_GROUP_FORM,
    hasAtLeastOneGroupRole,
    isAddUserGroupFormValid,
    type AddUserGroupFormState,
} from '../utils/userGroupMembership';

const GROUP_NONE_VALUE = '__none__';

interface AddUserGroupSheetProps {
    readonly open: boolean;
    readonly environmentId: string;
    readonly existingGroupIds: readonly string[];
    readonly onClose: () => void;
    readonly onSubmit: (payload: AddUserGroupMembershipPayload) => void;
    readonly isPending: boolean;
}

function RoleSelectField({
    id,
    label,
    value,
    roles,
    disabled,
    onChange,
}: Readonly<{
    id: string;
    label: string;
    value?: string;
    roles: OrganizationRole[];
    disabled: boolean;
    onChange: (value: string | undefined) => void;
}>) {
    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel htmlFor={id}>{label}</FieldLabel>
            <GroupMembershipRoleSelect id={id} ariaLabel={label} value={value} roles={roles} disabled={disabled} onChange={onChange} />
        </Field>
    );
}

function useRoleOptions(scope: GroupMembershipRoleCatalogScope) {
    const { data = [] } = useGroupMembershipRoleCatalog(scope);
    return data;
}

export function AddUserGroupSheet({ open, environmentId, existingGroupIds, onClose, onSubmit, isPending }: AddUserGroupSheetProps) {
    const [form, setForm] = useState<AddUserGroupFormState>(EMPTY_ADD_USER_GROUP_FORM);
    const [showRoleError, setShowRoleError] = useState(false);

    const { data: groupsResponse, isLoading: groupsLoading } = useEnvironmentGroups(environmentId, open);
    const apiRoles = useRoleOptions('API');
    const apiProductRoles = useRoleOptions('API_PRODUCT');
    const applicationRoles = useRoleOptions('APPLICATION');
    const integrationRoles = useRoleOptions('INTEGRATION');

    const availableGroups = useMemo(() => {
        const existing = new Set(existingGroupIds);
        return (groupsResponse?.data ?? []).filter(group => !existing.has(group.id));
    }, [existingGroupIds, groupsResponse?.data]);

    useEffect(() => {
        if (!open) {
            return;
        }
        setForm(EMPTY_ADD_USER_GROUP_FORM);
        setShowRoleError(false);
    }, [open]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    function setField<K extends keyof AddUserGroupFormState>(key: K, value: AddUserGroupFormState[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
        if (key !== 'groupId') {
            setShowRoleError(false);
        }
    }

    const isValid = isAddUserGroupFormValid(form);
    const allGroupsAdded = !groupsLoading && availableGroups.length === 0;

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (isPending || allGroupsAdded) {
            return;
        }
        if (!form.groupId) {
            return;
        }
        if (!hasAtLeastOneGroupRole(form)) {
            setShowRoleError(true);
            return;
        }
        onSubmit({
            groupId: form.groupId,
            isGroupAdmin: form.isGroupAdmin,
            apiRole: form.apiRole,
            apiProductRole: form.apiProductRole,
            applicationRole: form.applicationRole,
            integrationRole: form.integrationRole,
        });
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>Add a group with roles</SheetTitle>
                    <SheetDescription>Assign this user to a group and configure their roles within it.</SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    {allGroupsAdded ? (
                        <p className="px-1 py-4 text-sm text-muted-foreground">All groups are already added.</p>
                    ) : (
                        <form id="add-user-group-form" onSubmit={handleSubmit} className="flex flex-col gap-4 px-1 py-4">
                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="add-user-group-id">
                                    Group{' '}
                                    <span className="text-destructive" aria-hidden>
                                        *
                                    </span>
                                </FieldLabel>
                                <Select
                                    value={form.groupId || GROUP_NONE_VALUE}
                                    onValueChange={value => setField('groupId', value === GROUP_NONE_VALUE ? '' : value)}
                                    disabled={isPending || groupsLoading}
                                >
                                    <SelectTrigger id="add-user-group-id" className="w-full">
                                        <SelectValue placeholder="Select a group" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value={GROUP_NONE_VALUE} disabled>
                                            Select a group
                                        </SelectItem>
                                        {availableGroups.map(group => (
                                            <SelectItem key={group.id} value={group.id}>
                                                {group.name ?? group.id}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </Field>

                            <Field orientation="vertical" className="gap-2">
                                <div className="flex items-center gap-2">
                                    <Checkbox
                                        id="add-user-group-admin"
                                        checked={form.isGroupAdmin}
                                        onCheckedChange={checked => setField('isGroupAdmin', checked === true)}
                                        disabled={isPending}
                                    />
                                    <FieldLabel htmlFor="add-user-group-admin" className="font-normal">
                                        Group admin role
                                    </FieldLabel>
                                </div>
                            </Field>

                            <RoleSelectField
                                id="add-user-group-api-role"
                                label="API Role"
                                value={form.apiRole}
                                roles={apiRoles}
                                disabled={isPending}
                                onChange={value => setField('apiRole', value)}
                            />
                            <RoleSelectField
                                id="add-user-group-api-product-role"
                                label="API Product Role"
                                value={form.apiProductRole}
                                roles={apiProductRoles}
                                disabled={isPending}
                                onChange={value => setField('apiProductRole', value)}
                            />
                            <RoleSelectField
                                id="add-user-group-application-role"
                                label="Application Role"
                                value={form.applicationRole}
                                roles={applicationRoles}
                                disabled={isPending}
                                onChange={value => setField('applicationRole', value)}
                            />
                            <RoleSelectField
                                id="add-user-group-integration-role"
                                label="Integration Role"
                                value={form.integrationRole}
                                roles={integrationRoles}
                                disabled={isPending}
                                onChange={value => setField('integrationRole', value)}
                            />

                            {showRoleError ? (
                                <p className="text-sm text-destructive" role="alert">
                                    At least one role is mandatory.
                                </p>
                            ) : null}
                        </form>
                    )}
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
                        Cancel
                    </Button>
                    {!allGroupsAdded ? (
                        <Button type="submit" form="add-user-group-form" disabled={!isValid || isPending || groupsLoading}>
                            {isPending ? 'Saving…' : 'Save'}
                        </Button>
                    ) : null}
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
