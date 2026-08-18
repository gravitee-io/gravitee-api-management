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
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Textarea,
} from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';

import { ApimApiError } from '../../../shared/api/apimClient';
import { extractErrorMessage } from '../../../shared/notify/extractErrorMessage';
import { STANDARD_SHEET_WIDTH } from '../../applications/components/sheetLayout';
import type { NewTenantPayload, Tenant, UpdateTenantPayload } from '../types/tenant';
import {
    findDuplicateTenantKey,
    getTenantDescriptionError,
    getTenantNameError,
    isTenantDescriptionValid,
    isTenantKeyValid,
    isTenantNameValid,
    slugifyTenantKeyBase,
    slugifyTenantKeyFinal,
    TENANT_DESCRIPTION_MAX,
    TENANT_KEY_MAX,
    TENANT_NAME_MAX,
    tenantKeyFromName,
} from '../utils/tenantFormValidation';

interface TenantForm {
    name: string;
    key: string;
    description: string;
}

const EMPTY_FORM: TenantForm = {
    name: '',
    key: '',
    description: '',
};

const NAME_REQUIRED_MESSAGE = 'Name is required.';
const KEY_REQUIRED_MESSAGE = 'Key is required and must contain at least one letter or number.';

function tenantToForm(tenant: Tenant): TenantForm {
    return {
        name: tenant.name,
        key: tenant.key,
        description: tenant.description ?? '',
    };
}

function isDuplicateTenantKeyApiError(error: unknown): boolean {
    if (!(error instanceof ApimApiError) || error.status !== 400) return false;
    const technicalCode = (error.body as { technicalCode?: string } | undefined)?.technicalCode;
    if (technicalCode === 'tenant.exists') return true;
    return /already exists/i.test(error.message);
}

type TenantFormSheetBaseProps = {
    open: boolean;
    tenant?: Tenant | null;
    existingTenants: Tenant[];
    onClose: () => void;
    isSaving: boolean;
};

type TenantFormSheetProps =
    | (TenantFormSheetBaseProps & {
          mode: 'create';
          onSubmit: (data: NewTenantPayload) => Promise<void>;
      })
    | (TenantFormSheetBaseProps & {
          mode: 'edit';
          onSubmit: (data: UpdateTenantPayload) => Promise<void>;
      });

export function TenantFormSheet({
    open,
    mode,
    tenant = null,
    existingTenants,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<TenantFormSheetProps>) {
    const [form, setForm] = useState<TenantForm>(EMPTY_FORM);
    const [nameTouched, setNameTouched] = useState(false);
    const [keyTouched, setKeyTouched] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [duplicateKeyError, setDuplicateKeyError] = useState<string | null>(null);
    const seededTenantIdRef = useRef<string | null>(null);

    useEffect(() => {
        if (!open) {
            seededTenantIdRef.current = null;
            return;
        }

        if (mode === 'create') {
            setForm(EMPTY_FORM);
            setNameTouched(false);
            setKeyTouched(false);
            setSubmitError(null);
            setDuplicateKeyError(null);
            seededTenantIdRef.current = null;
            return;
        }

        if (!tenant) {
            if (seededTenantIdRef.current === null) {
                setForm(EMPTY_FORM);
                setNameTouched(false);
                setSubmitError(null);
                setDuplicateKeyError(null);
            }
            return;
        }

        if (seededTenantIdRef.current === tenant.id) return;
        seededTenantIdRef.current = tenant.id;
        setForm(tenantToForm(tenant));
        setNameTouched(false);
        setKeyTouched(false);
        setSubmitError(null);
        setDuplicateKeyError(null);
    }, [open, mode, tenant]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen && !isSaving) onClose();
        },
        [onClose, isSaving],
    );

    const nameError = getTenantNameError(form.name);
    const descriptionError = getTenantDescriptionError(form.description);
    const isNameValid = isTenantNameValid(form.name);
    const isKeyValid = isTenantKeyValid(form.key);
    const isValid =
        (mode === 'create' || Boolean(tenant)) &&
        isNameValid &&
        nameError === null &&
        isKeyValid &&
        isTenantDescriptionValid(form.description) &&
        duplicateKeyError === null;
    const keyFieldInvalid = mode === 'create' && (duplicateKeyError !== null || !isKeyValid);
    /** Scopes element ids per mode so create and edit field ids stay unique. */
    const idPrefix = mode === 'create' ? 'tenant-create' : 'tenant-edit';
    const formId = `${idPrefix}-form`;

    function clearDuplicateKeyError() {
        if (duplicateKeyError) setDuplicateKeyError(null);
    }

    function handleNameChange(value: string) {
        clearDuplicateKeyError();
        setNameTouched(true);
        setForm(prev => ({
            ...prev,
            name: value,
            key: mode === 'create' && !keyTouched ? tenantKeyFromName(value) : prev.key,
        }));
    }

    function handleKeyChange(value: string) {
        clearDuplicateKeyError();
        setKeyTouched(true);
        setForm(prev => ({ ...prev, key: slugifyTenantKeyBase(value).slice(0, TENANT_KEY_MAX) }));
    }

    function handleKeyBlur() {
        setForm(prev => {
            const finalized = slugifyTenantKeyFinal(prev.key).slice(0, TENANT_KEY_MAX);
            return finalized === prev.key ? prev : { ...prev, key: finalized };
        });
    }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!isValid || isSaving) return;

        const name = form.name.trim();
        const key = mode === 'create' ? slugifyTenantKeyFinal(form.key) : form.key;
        // Only the key is unique server-side; two tenants may legitimately share a name.
        if (mode === 'create' && findDuplicateTenantKey(existingTenants, key)) {
            setDuplicateKeyError('The tenant key already exists.');
            return;
        }

        try {
            setSubmitError(null);
            setDuplicateKeyError(null);
            const description = form.description.trim() || undefined;
            if (mode === 'create') {
                await (onSubmit as (data: NewTenantPayload) => Promise<void>)({ name, key, description });
            } else {
                await (onSubmit as (data: UpdateTenantPayload) => Promise<void>)({ key, name, description });
            }
        } catch (error) {
            const fallback = mode === 'create' ? 'Failed to create tenant' : 'Failed to update tenant';
            const message = extractErrorMessage(error, fallback);
            if (mode === 'create' && isDuplicateTenantKeyApiError(error)) {
                setDuplicateKeyError(message);
            } else {
                setSubmitError(message);
            }
        }
    }

    const title = mode === 'create' ? 'Create a tenant' : 'Edit a tenant';
    const submitLabel = mode === 'create' ? (isSaving ? 'Creating...' : 'Create tenant') : isSaving ? 'Saving...' : 'Save';
    const nameFieldError = nameError ?? (nameTouched && !isNameValid ? NAME_REQUIRED_MESSAGE : null);
    const keyFieldError = duplicateKeyError ?? (mode === 'create' && keyTouched && !isKeyValid ? KEY_REQUIRED_MESSAGE : null);
    const sheetDescription =
        mode === 'create'
            ? 'The key is what you paste into gravitee.yml. It is generated from the name unless you type one.'
            : 'Change the name or description. The key is already in use on gateways and cannot be renamed.';

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent
                side="right"
                showCloseButton={false}
                className="flex max-h-full flex-col"
                style={{ maxWidth: STANDARD_SHEET_WIDTH }}
            >
                <SheetHeader className="flex-row items-start justify-between gap-3 space-y-0">
                    <div className="space-y-1.5 text-left">
                        <SheetTitle>{title}</SheetTitle>
                        <SheetDescription>{sheetDescription}</SheetDescription>
                    </div>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8 shrink-0"
                        aria-label="Close"
                        onClick={() => handleOpenChange(false)}
                        disabled={isSaving}
                    >
                        <XIcon className="size-4" aria-hidden />
                    </Button>
                </SheetHeader>

                <ScrollArea className="min-h-0 flex-1">
                    <form id={formId} onSubmit={handleSubmit} className="flex flex-col gap-5 px-4 py-4">
                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor={`${idPrefix}-name`}>
                                Name{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <Input
                                id={`${idPrefix}-name`}
                                value={form.name}
                                onChange={e => handleNameChange(e.target.value)}
                                placeholder="e.g. US East"
                                disabled={isSaving}
                                required
                                maxLength={TENANT_NAME_MAX}
                                aria-invalid={nameFieldError !== null}
                                aria-describedby={nameFieldError !== null ? `${idPrefix}-name-error` : `${idPrefix}-name-count`}
                            />
                            {nameFieldError ? (
                                <p id={`${idPrefix}-name-error`} className="text-sm text-destructive" role="alert">
                                    {nameFieldError}
                                </p>
                            ) : (
                                <p id={`${idPrefix}-name-count`} className="text-xs text-muted-foreground">
                                    {form.name.length}/{TENANT_NAME_MAX}
                                </p>
                            )}
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor={`${idPrefix}-key`}>
                                Key{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            {mode === 'create' ? (
                                <Input
                                    id={`${idPrefix}-key`}
                                    value={form.key}
                                    onChange={e => handleKeyChange(e.target.value)}
                                    onBlur={handleKeyBlur}
                                    placeholder="e.g. us-east"
                                    disabled={isSaving}
                                    required
                                    aria-required
                                    maxLength={TENANT_KEY_MAX}
                                    aria-invalid={keyFieldInvalid}
                                    aria-describedby={keyFieldError !== null ? `${idPrefix}-key-error` : `${idPrefix}-key-count`}
                                />
                            ) : (
                                <Input
                                    id={`${idPrefix}-key`}
                                    value={form.key}
                                    readOnly
                                    disabled
                                    placeholder="e.g. us-east"
                                    className="bg-muted"
                                    aria-describedby={`${idPrefix}-key-count`}
                                />
                            )}
                            {keyFieldError ? (
                                <p id={`${idPrefix}-key-error`} className="text-sm text-destructive" role="alert">
                                    {keyFieldError}
                                </p>
                            ) : (
                                <p id={`${idPrefix}-key-count`} className="text-xs text-muted-foreground">
                                    {form.key.length}/{TENANT_KEY_MAX}
                                </p>
                            )}
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor={`${idPrefix}-description`}>Description</FieldLabel>
                            <Textarea
                                id={`${idPrefix}-description`}
                                value={form.description}
                                onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))}
                                placeholder="Optional details"
                                disabled={isSaving}
                                maxLength={TENANT_DESCRIPTION_MAX}
                                aria-invalid={descriptionError !== null}
                                aria-describedby={
                                    descriptionError !== null ? `${idPrefix}-description-error` : `${idPrefix}-description-count`
                                }
                            />
                            {descriptionError ? (
                                <p id={`${idPrefix}-description-error`} className="text-sm text-destructive" role="alert">
                                    {descriptionError}
                                </p>
                            ) : (
                                <p id={`${idPrefix}-description-count`} className="text-xs text-muted-foreground">
                                    {form.description.length}/{TENANT_DESCRIPTION_MAX}
                                </p>
                            )}
                        </Field>

                        {submitError ? (
                            <p className="text-sm text-destructive" role="alert">
                                {submitError}
                            </p>
                        ) : null}
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form={formId} disabled={!isValid || isSaving}>
                        {submitLabel}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
