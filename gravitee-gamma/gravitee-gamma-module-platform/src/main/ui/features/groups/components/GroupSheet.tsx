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
    Field,
    FieldLabel,
    Input,
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
    Switch,
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react';

import { GroupAssociateDialog } from './GroupAssociateDialog';
import { notify } from '../../../shared/notify';
import { STANDARD_SHEET_WIDTH } from '../../applications/components/sheetLayout';
import { useAssociateGroupToExisting } from '../hooks/useGroupMutations';
import type { Group, GroupMembershipType, GroupRole } from '../types/group';
import { ASSOCIATION_TYPE_LABELS } from '../utils/groupPayload';

export type GroupSheetMode = 'create' | 'edit';

export interface GroupFormValues {
    name: string;
    apiRole: string;
    applicationRole: string;
    apiProductRole: string;
    lockApiRole: boolean;
    lockApiProductRole: boolean;
    lockApplicationRole: boolean;
    defaultGroupForNewApis: boolean;
    defaultGroupForNewApiProducts: boolean;
    defaultGroupForNewApplications: boolean;
    maxInvitation: string;
    systemInvitation: boolean;
    emailInvitation: boolean;
    notifyOnMemberAdded: boolean;
}

const NO_ROLE_VALUE = '__none__';

function defaultRoleValue(roles: GroupRole[]): string {
    return roles.find(role => role.default)?.name ?? '';
}

function hasEventRule(group: Group, event: string): boolean {
    return (group.event_rules ?? []).some(rule => rule.event === event);
}

function buildEmptyForm(apiRoles: GroupRole[], applicationRoles: GroupRole[], apiProductRoles: GroupRole[]): GroupFormValues {
    return {
        name: '',
        apiRole: defaultRoleValue(apiRoles),
        applicationRole: defaultRoleValue(applicationRoles),
        apiProductRole: defaultRoleValue(apiProductRoles),
        lockApiRole: false,
        lockApiProductRole: false,
        lockApplicationRole: false,
        defaultGroupForNewApis: false,
        defaultGroupForNewApiProducts: false,
        defaultGroupForNewApplications: false,
        maxInvitation: '',
        systemInvitation: false,
        emailInvitation: false,
        notifyOnMemberAdded: true,
    };
}

function buildFormFromGroup(group: Group): GroupFormValues {
    return {
        name: group.name,
        apiRole: group.roles?.API ?? '',
        applicationRole: group.roles?.APPLICATION ?? '',
        apiProductRole: group.roles?.API_PRODUCT ?? '',
        lockApiRole: Boolean(group.lock_api_role),
        lockApiProductRole: Boolean(group.lock_api_product_role),
        lockApplicationRole: Boolean(group.lock_application_role),
        defaultGroupForNewApis: hasEventRule(group, 'API_CREATE'),
        defaultGroupForNewApiProducts: hasEventRule(group, 'API_PRODUCT_CREATE'),
        defaultGroupForNewApplications: hasEventRule(group, 'APPLICATION_CREATE'),
        maxInvitation: String(group.max_invitation ?? ''),
        systemInvitation: Boolean(group.system_invitation),
        emailInvitation: Boolean(group.email_invitation),
        notifyOnMemberAdded: !group.disable_membership_notifications,
    };
}

function ToggleRow({
    id,
    title,
    description,
    checked,
    onCheckedChange,
    disabled,
}: Readonly<{
    id: string;
    title: string;
    description: string;
    checked: boolean;
    onCheckedChange: (checked: boolean) => void;
    disabled?: boolean;
}>) {
    return (
        <div className="flex items-center justify-between gap-4 rounded-lg border px-3 py-2">
            <div className="space-y-0.5">
                <FieldLabel htmlFor={id}>{title}</FieldLabel>
                <p className="text-xs text-muted-foreground">{description}</p>
            </div>
            <Switch id={id} checked={checked} onCheckedChange={onCheckedChange} disabled={disabled} />
        </div>
    );
}

function RoleSelect({
    id,
    label,
    value,
    roles,
    disabled,
    onChange,
}: Readonly<{
    id: string;
    label: ReactNode;
    value: string;
    roles: GroupRole[];
    disabled: boolean;
    onChange: (value: string) => void;
}>) {
    const selectableRoles = roles.filter(role => !role.system || role.name === value);

    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel htmlFor={id}>{label}</FieldLabel>
            <Select value={value || NO_ROLE_VALUE} onValueChange={val => onChange(val === NO_ROLE_VALUE ? '' : val)} disabled={disabled}>
                <SelectTrigger id={id}>
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value={NO_ROLE_VALUE}>None</SelectItem>
                    {selectableRoles.map(role => (
                        <SelectItem key={role.name} value={role.name}>
                            {role.name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </Field>
    );
}

export function GroupSheet({
    open,
    mode,
    group,
    apiRoles,
    applicationRoles,
    apiProductRoles,
    rolesLoading,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    mode: GroupSheetMode;
    group?: Group;
    apiRoles: GroupRole[];
    applicationRoles: GroupRole[];
    apiProductRoles: GroupRole[];
    rolesLoading: boolean;
    onClose: () => void;
    onSubmit: (values: GroupFormValues) => void;
    isSaving: boolean;
}>) {
    const [form, setForm] = useState<GroupFormValues>(() => buildEmptyForm([], [], []));
    const [initialForm, setInitialForm] = useState<GroupFormValues | null>(null);
    const [maxInvitationError, setMaxInvitationError] = useState<string | null>(null);
    const [rolesTouched, setRolesTouched] = useState(false);
    const [associatingType, setAssociatingType] = useState<GroupMembershipType | null>(null);
    const associateMutation = useAssociateGroupToExisting();

    useEffect(() => {
        if (!open) return;
        if (mode === 'edit' && group) {
            const initial = buildFormFromGroup(group);
            setForm(initial);
            setInitialForm(initial);
        } else {
            const initial = buildEmptyForm(apiRoles, applicationRoles, apiProductRoles);
            setForm(initial);
            setInitialForm(null);
        }
        setMaxInvitationError(null);
        setRolesTouched(false);
        setAssociatingType(null);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, mode, group]);

    useEffect(() => {
        if (!open || mode !== 'create' || rolesLoading || rolesTouched) return;
        setForm(prev => ({
            ...prev,
            apiRole: prev.apiRole || defaultRoleValue(apiRoles),
            applicationRole: prev.applicationRole || defaultRoleValue(applicationRoles),
            apiProductRole: prev.apiProductRole || defaultRoleValue(apiProductRoles),
        }));
    }, [open, mode, rolesLoading, rolesTouched, apiRoles, applicationRoles, apiProductRoles]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    function setField<K extends keyof GroupFormValues>(key: K, value: GroupFormValues[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
    }

    function setRoleField(key: 'apiRole' | 'applicationRole' | 'apiProductRole', value: string) {
        setRolesTouched(true);
        setField(key, value);
    }

    function handleMaxInvitationChange(value: string) {
        setField('maxInvitation', value);
        if (!value.trim()) {
            setMaxInvitationError(null);
            return;
        }
        const parsed = Number(value);
        setMaxInvitationError(Number.isFinite(parsed) && parsed >= 1 ? null : 'Enter a positive number or leave blank');
    }

    const isValid = form.name.trim() !== '' && !maxInvitationError;

    const hasChanged = useMemo(() => {
        if (mode === 'create') return true;
        if (!initialForm) return false;
        return (Object.keys(form) as Array<keyof GroupFormValues>).some(key => form[key] !== initialForm[key]);
    }, [mode, form, initialForm]);

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!isValid || !hasChanged) return;
        onSubmit({ ...form, name: form.name.trim() });
    }

    async function handleAssociate() {
        if (!group || !associatingType) return;
        try {
            await associateMutation.mutateAsync({ groupId: group.id, type: associatingType });
            notify.success(`Successfully added the group to existing ${ASSOCIATION_TYPE_LABELS[associatingType]}`);
            setAssociatingType(null);
        } catch (error) {
            notify.error(error, `Failed to add the group to existing ${ASSOCIATION_TYPE_LABELS[associatingType]}`);
        }
    }

    return (
        <>
            <Sheet open={open} onOpenChange={handleOpenChange}>
                <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                    <SheetHeader>
                        <SheetTitle>{mode === 'create' ? 'Create group' : 'Edit group'}</SheetTitle>
                        <SheetDescription>
                            {mode === 'create'
                                ? 'New groups can receive default API and application roles, and be used in IdP mappings.'
                                : 'Update the group name, default roles, and association settings.'}
                        </SheetDescription>
                    </SheetHeader>

                    <ScrollArea className="flex-1 min-h-0">
                        <form id="group-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-4 py-4">
                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="group-name">
                                    Name{' '}
                                    <span className="text-destructive" aria-hidden>
                                        *
                                    </span>
                                </FieldLabel>
                                <Input
                                    id="group-name"
                                    value={form.name}
                                    onChange={e => setField('name', e.target.value)}
                                    placeholder="e.g. Support Team"
                                    disabled={isSaving}
                                    required
                                />
                            </Field>

                            <div className="grid grid-cols-2 gap-4">
                                <RoleSelect
                                    id="group-api-role"
                                    label="Default API role"
                                    value={form.apiRole}
                                    roles={apiRoles}
                                    disabled={isSaving || rolesLoading}
                                    onChange={val => setRoleField('apiRole', val)}
                                />
                                <RoleSelect
                                    id="group-application-role"
                                    label="Default application role"
                                    value={form.applicationRole}
                                    roles={applicationRoles}
                                    disabled={isSaving || rolesLoading}
                                    onChange={val => setRoleField('applicationRole', val)}
                                />
                                <div className="col-span-2">
                                    <RoleSelect
                                        id="group-api-product-role"
                                        label="Default API product role"
                                        value={form.apiProductRole}
                                        roles={apiProductRoles}
                                        disabled={isSaving || rolesLoading}
                                        onChange={val => setRoleField('apiProductRole', val)}
                                    />
                                </div>
                            </div>

                            <div className="flex flex-col gap-2">
                                <ToggleRow
                                    id="group-lock-api-role"
                                    title="Lock API role"
                                    description="Members can't change their API role in this group."
                                    checked={form.lockApiRole}
                                    onCheckedChange={val => setField('lockApiRole', val)}
                                    disabled={isSaving}
                                />
                                <ToggleRow
                                    id="group-lock-api-product-role"
                                    title="Lock API product role"
                                    description="Members can't change their API product role in this group."
                                    checked={form.lockApiProductRole}
                                    onCheckedChange={val => setField('lockApiProductRole', val)}
                                    disabled={isSaving}
                                />
                                <ToggleRow
                                    id="group-lock-application-role"
                                    title="Lock application role"
                                    description="Members can't change their application role in this group."
                                    checked={form.lockApplicationRole}
                                    onCheckedChange={val => setField('lockApplicationRole', val)}
                                    disabled={isSaving}
                                />
                                <ToggleRow
                                    id="group-add-new-apis"
                                    title="Associate with new APIs"
                                    description="Automatically add this group when APIs are created."
                                    checked={form.defaultGroupForNewApis}
                                    onCheckedChange={val => setField('defaultGroupForNewApis', val)}
                                    disabled={isSaving}
                                />
                                {mode === 'edit' && group && (
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        className="self-start"
                                        onClick={() => setAssociatingType('api')}
                                        disabled={isSaving}
                                    >
                                        Add to existing APIs
                                    </Button>
                                )}
                                <ToggleRow
                                    id="group-add-new-api-products"
                                    title="Associate with new API products"
                                    description="Automatically add this group when API products are created."
                                    checked={form.defaultGroupForNewApiProducts}
                                    onCheckedChange={val => setField('defaultGroupForNewApiProducts', val)}
                                    disabled={isSaving}
                                />
                                {mode === 'edit' && group && (
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        className="self-start"
                                        onClick={() => setAssociatingType('api_product')}
                                        disabled={isSaving}
                                    >
                                        Add to existing API products
                                    </Button>
                                )}
                                <ToggleRow
                                    id="group-add-new-applications"
                                    title="Associate with new applications"
                                    description="Automatically add this group when applications are created."
                                    checked={form.defaultGroupForNewApplications}
                                    onCheckedChange={val => setField('defaultGroupForNewApplications', val)}
                                    disabled={isSaving}
                                />
                                {mode === 'edit' && group && (
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        className="self-start"
                                        onClick={() => setAssociatingType('application')}
                                        disabled={isSaving}
                                    >
                                        Add to existing applications
                                    </Button>
                                )}
                            </div>

                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="group-max">Maximum members</FieldLabel>
                                <Input
                                    id="group-max"
                                    type="number"
                                    min={1}
                                    value={form.maxInvitation}
                                    onChange={e => handleMaxInvitationChange(e.target.value)}
                                    placeholder="Unlimited"
                                    disabled={isSaving}
                                    aria-invalid={Boolean(maxInvitationError)}
                                />
                                <p className={maxInvitationError ? 'text-xs text-destructive' : 'text-xs text-muted-foreground'}>
                                    {maxInvitationError ?? 'Leave blank for no limit.'}
                                </p>
                            </Field>

                            <div className="flex flex-col gap-2">
                                <ToggleRow
                                    id="group-system-invitation"
                                    title="Allow invitation via user search"
                                    description="Admins can search and add existing platform users."
                                    checked={form.systemInvitation}
                                    onCheckedChange={val => setField('systemInvitation', val)}
                                    disabled={isSaving}
                                />
                                <ToggleRow
                                    id="group-email-invitation"
                                    title="Allow invitation via email"
                                    description="Admins can invite people who are not yet registered."
                                    checked={form.emailInvitation}
                                    onCheckedChange={val => setField('emailInvitation', val)}
                                    disabled={isSaving}
                                />
                                <ToggleRow
                                    id="group-notify-on-member-added"
                                    title="Notify members when added"
                                    description="Send a notification when membership changes."
                                    checked={form.notifyOnMemberAdded}
                                    onCheckedChange={val => setField('notifyOnMemberAdded', val)}
                                    disabled={isSaving}
                                />
                            </div>
                        </form>
                    </ScrollArea>

                    <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                        <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                            Cancel
                        </Button>
                        <Button type="submit" form="group-form" disabled={!isValid || !hasChanged || isSaving}>
                            {isSaving ? (mode === 'create' ? 'Creating…' : 'Saving…') : mode === 'create' ? 'Create group' : 'Save'}
                        </Button>
                    </SheetFooter>
                </SheetContent>
            </Sheet>

            <GroupAssociateDialog
                open={associatingType !== null}
                typeLabel={associatingType ? ASSOCIATION_TYPE_LABELS[associatingType] : ''}
                onClose={() => setAssociatingType(null)}
                onConfirm={handleAssociate}
                isAssociating={associateMutation.isPending}
            />
        </>
    );
}
