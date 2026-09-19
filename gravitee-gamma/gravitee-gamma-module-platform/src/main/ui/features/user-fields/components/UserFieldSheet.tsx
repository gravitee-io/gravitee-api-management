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
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    ScrollArea,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Switch,
} from '@gravitee/graphene-core';
import { useId } from 'react';

import { ChipInput } from '../../shared/components';
import { useUserFieldForm, type UserFieldFormMode, type UserFieldFormState } from '../hooks/useUserFieldForm';
import type { UserField, UserFieldPayload } from '../types/userField';
import { USER_FIELD_KEY_MAX_LENGTH, USER_FIELD_KEY_SUGGESTIONS, USER_FIELD_LABEL_MAX_LENGTH } from '../utils/userFieldForm';

export type UserFieldSheetMode = UserFieldFormMode;

function UserFieldFormFields({
    form,
    setValue,
    keyError,
    labelError,
    isEdit,
    isSaving,
}: Readonly<{
    form: UserFieldFormState;
    setValue: <K extends keyof UserFieldFormState>(name: K, value: UserFieldFormState[K]) => void;
    keyError: string | null;
    labelError: string | null;
    isEdit: boolean;
    isSaving: boolean;
}>) {
    const keyListId = useId();
    const keyErrorId = useId();
    const labelErrorId = useId();

    return (
        <>
            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="user-field-key">
                    Key{' '}
                    <span className="text-destructive" aria-hidden>
                        *
                    </span>
                </FieldLabel>
                <Input
                    id="user-field-key"
                    list={isEdit ? undefined : keyListId}
                    value={form.key}
                    maxLength={USER_FIELD_KEY_MAX_LENGTH}
                    onChange={e => setValue('key', e.target.value)}
                    placeholder="e.g. job_position"
                    disabled={isEdit || isSaving}
                    readOnly={isEdit}
                    autoComplete="off"
                    aria-invalid={keyError ? true : undefined}
                    aria-describedby={keyError ? keyErrorId : undefined}
                />
                {isEdit ? null : (
                    <datalist id={keyListId}>
                        {USER_FIELD_KEY_SUGGESTIONS.map(suggestion => (
                            <option key={suggestion} value={suggestion} />
                        ))}
                    </datalist>
                )}
                {isEdit ? null : (
                    <FieldDescription>
                        {form.key.length}/{USER_FIELD_KEY_MAX_LENGTH}
                    </FieldDescription>
                )}
                {keyError ? <FieldError id={keyErrorId}>{keyError}</FieldError> : null}
            </Field>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="user-field-label">
                    Label{' '}
                    <span className="text-destructive" aria-hidden>
                        *
                    </span>
                </FieldLabel>
                <Input
                    id="user-field-label"
                    value={form.label}
                    maxLength={USER_FIELD_LABEL_MAX_LENGTH}
                    onChange={e => setValue('label', e.target.value)}
                    placeholder="e.g. Job position"
                    disabled={isSaving}
                    aria-invalid={labelError ? true : undefined}
                    aria-describedby={labelError ? labelErrorId : undefined}
                />
                <FieldDescription>
                    {form.label.length}/{USER_FIELD_LABEL_MAX_LENGTH}
                </FieldDescription>
                {labelError ? <FieldError id={labelErrorId}>{labelError}</FieldError> : null}
            </Field>

            <Field orientation="horizontal">
                <div className="min-w-0 flex-1 space-y-1">
                    <FieldLabel htmlFor="user-field-required">Required</FieldLabel>
                    <FieldDescription>Registration cannot be completed without an answer.</FieldDescription>
                </div>
                <Switch
                    id="user-field-required"
                    checked={form.required}
                    disabled={isSaving}
                    onCheckedChange={required => setValue('required', required)}
                />
            </Field>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="user-field-values">Values</FieldLabel>
                <ChipInput
                    id="user-field-values"
                    values={form.values}
                    onChange={values => setValue('values', values)}
                    placeholder="Type value and confirm with enter."
                    disabled={isSaving}
                    addOnComma
                />
                <FieldDescription>Optional. Leave empty for free text, or add values to constrain the answers.</FieldDescription>
            </Field>
        </>
    );
}

export function UserFieldSheet({
    open,
    mode,
    field,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    mode: UserFieldSheetMode;
    field?: UserField;
    onClose: () => void;
    onSubmit: (payload: UserFieldPayload) => void;
    isSaving: boolean;
}>) {
    const isEdit = mode === 'edit';
    const { form, setValue, hasChanged, handleSubmit, keyError, labelError } = useUserFieldForm({ open, mode, field, onSubmit });

    return (
        <Sheet
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen && !isSaving) onClose();
            }}
        >
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>{isEdit ? 'Update user field' : 'Create user field'}</SheetTitle>
                    <SheetDescription>
                        {isEdit
                            ? 'Change the label, whether the field is required, or the allowed values. The key is already stored on user profiles and cannot be renamed.'
                            : 'This field is asked when someone signs up in the APIM console or the developer portal. Email, first name, and last name are already included.'}
                    </SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    <form id="user-field-form" onSubmit={handleSubmit} noValidate className="flex flex-col gap-5 px-1 py-4">
                        <UserFieldFormFields
                            form={form}
                            setValue={setValue}
                            keyError={keyError}
                            labelError={labelError}
                            isEdit={isEdit}
                            isSaving={isSaving}
                        />
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="user-field-form" disabled={isSaving || (isEdit && !hasChanged)}>
                        {isSaving ? 'Saving…' : isEdit ? 'Save changes' : 'Create field'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
