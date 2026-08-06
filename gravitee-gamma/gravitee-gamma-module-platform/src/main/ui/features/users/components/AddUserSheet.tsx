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
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import { BotIcon, UserIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useState, type FormEvent } from 'react';

import { STANDARD_SHEET_WIDTH } from '../../applications/components/sheetLayout';
import { useIdentityProviders } from '../hooks/useOrganizationUsers';
import type { NewPreRegisterUserPayload, UserType } from '../types/user';
import { GRAVITEE_IDP } from '../types/user';
import { sanitizeTextInput } from '../utils/userDisplay';
import { applyUserTypeChange, isAddUserFormValid, resolvePreRegisterUserSource } from '../utils/userFormValidation';

interface UserFormState {
    type: UserType;
    firstName: string;
    lastName: string;
    email: string;
    source: string;
    sourceId: string;
}

const EMPTY_FORM: UserFormState = {
    type: 'EXTERNAL_USER',
    firstName: '',
    lastName: '',
    email: '',
    source: GRAVITEE_IDP.id,
    sourceId: '',
};

interface AddUserSheetProps {
    readonly open: boolean;
    readonly onClose: () => void;
    readonly onSubmit: (payload: NewPreRegisterUserPayload) => void;
    readonly isPending: boolean;
}

export function AddUserSheet({ open, onClose, onSubmit, isPending }: AddUserSheetProps) {
    const { data: identityProviders = [GRAVITEE_IDP], isLoading: idpLoading } = useIdentityProviders();
    const [form, setForm] = useState<UserFormState>(EMPTY_FORM);

    useEffect(() => {
        if (!open) return;
        setForm(EMPTY_FORM);
    }, [open]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    function setField<K extends keyof UserFormState>(key: K, value: UserFormState[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
    }

    const showIdentityProviderFields = identityProviders.length > 1;
    const isServiceAccount = form.type === 'SERVICE_ACCOUNT';
    const identityProvidersReady = !idpLoading;

    const isValid = isAddUserFormValid(form, { showIdentityProviderFields, identityProvidersReady });

    function buildPayload(): NewPreRegisterUserPayload {
        const email = form.email.trim();
        const source = resolvePreRegisterUserSource(isServiceAccount, showIdentityProviderFields, form.source);
        return {
            firstname: isServiceAccount ? null : sanitizeTextInput(form.firstName),
            lastname: sanitizeTextInput(form.lastName),
            email: email || undefined,
            source,
            sourceId: !isServiceAccount && form.source !== GRAVITEE_IDP.id ? form.sourceId.trim() : '',
            service: isServiceAccount,
        };
    }

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!isValid || isPending) return;
        onSubmit(buildPayload());
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>Add User</SheetTitle>
                    <SheetDescription>
                        Pre-register a new user or service account. They will receive an email to set their password.
                    </SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    <form id="add-user-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-1 py-4">
                        <Field orientation="vertical" className="gap-2">
                            <FieldLabel>User type</FieldLabel>
                            <ToggleGroup
                                type="single"
                                value={form.type}
                                onValueChange={value => {
                                    if (value) setForm(prev => applyUserTypeChange(prev, value as UserType));
                                }}
                                className="grid grid-cols-2 gap-2"
                                disabled={isPending}
                            >
                                <ToggleGroupItem
                                    value="EXTERNAL_USER"
                                    className="h-auto flex-col gap-2 py-4 data-[state=on]:border-primary"
                                >
                                    <UserIcon className="size-5" aria-hidden />
                                    <span>External User</span>
                                </ToggleGroupItem>
                                <ToggleGroupItem
                                    value="SERVICE_ACCOUNT"
                                    className="h-auto flex-col gap-2 py-4 data-[state=on]:border-primary"
                                >
                                    <BotIcon className="size-5" aria-hidden />
                                    <span>Service Account</span>
                                </ToggleGroupItem>
                            </ToggleGroup>
                        </Field>

                        {!isServiceAccount ? (
                            <>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="user-first-name">
                                        First Name{' '}
                                        <span className="text-destructive" aria-hidden>
                                            *
                                        </span>
                                    </FieldLabel>
                                    <Input
                                        id="user-first-name"
                                        value={form.firstName}
                                        onChange={e => setField('firstName', e.target.value)}
                                        placeholder="Jane"
                                        disabled={isPending}
                                        required
                                    />
                                </Field>

                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="user-last-name">
                                        Last Name{' '}
                                        <span className="text-destructive" aria-hidden>
                                            *
                                        </span>
                                    </FieldLabel>
                                    <Input
                                        id="user-last-name"
                                        value={form.lastName}
                                        onChange={e => setField('lastName', e.target.value)}
                                        placeholder="Doe"
                                        disabled={isPending}
                                        required
                                    />
                                </Field>

                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="user-email">
                                        Email{' '}
                                        <span className="text-destructive" aria-hidden>
                                            *
                                        </span>
                                    </FieldLabel>
                                    <Input
                                        id="user-email"
                                        type="email"
                                        value={form.email}
                                        onChange={e => setField('email', e.target.value)}
                                        placeholder="jane@company.com"
                                        disabled={isPending}
                                        required
                                    />
                                </Field>
                            </>
                        ) : (
                            <>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="service-name">
                                        Service Name{' '}
                                        <span className="text-destructive" aria-hidden>
                                            *
                                        </span>
                                    </FieldLabel>
                                    <Input
                                        id="service-name"
                                        value={form.lastName}
                                        onChange={e => setField('lastName', e.target.value)}
                                        disabled={isPending}
                                        required
                                    />
                                    <p className="text-xs text-muted-foreground">Choose a meaningful name for your service account.</p>
                                </Field>

                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="service-email">Email</FieldLabel>
                                    <Input
                                        id="service-email"
                                        type="email"
                                        value={form.email}
                                        onChange={e => setField('email', e.target.value)}
                                        disabled={isPending}
                                    />
                                </Field>
                            </>
                        )}

                        {showIdentityProviderFields && !isServiceAccount ? (
                            <>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="user-source">Identity Provider</FieldLabel>
                                    <Select
                                        value={form.source}
                                        onValueChange={value => setField('source', value)}
                                        disabled={isPending || idpLoading}
                                    >
                                        <SelectTrigger id="user-source">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {identityProviders.map(idp => (
                                                <SelectItem key={idp.id} value={idp.id}>
                                                    {idp.name}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </Field>

                                {form.source !== GRAVITEE_IDP.id ? (
                                    <Field orientation="vertical" className="gap-1.5">
                                        <FieldLabel htmlFor="user-source-id">
                                            Identifier{' '}
                                            <span className="text-destructive" aria-hidden>
                                                *
                                            </span>
                                        </FieldLabel>
                                        <Input
                                            id="user-source-id"
                                            value={form.sourceId}
                                            onChange={e => setField('sourceId', e.target.value)}
                                            disabled={isPending}
                                            required
                                        />
                                    </Field>
                                ) : null}
                            </>
                        ) : null}
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button type="submit" form="add-user-form" disabled={!isValid || isPending}>
                        {isPending ? 'Adding…' : 'Add User'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
