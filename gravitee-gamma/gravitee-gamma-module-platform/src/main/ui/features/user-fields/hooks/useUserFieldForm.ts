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
import { useEffect, useState, type FormEvent } from 'react';

import type { UserField, UserFieldPayload } from '../types/userField';
import { normalizeUserFieldKey, normalizeUserFieldValues, userFieldKeyError, userFieldLabelError } from '../utils/userFieldForm';

export type UserFieldFormMode = 'create' | 'edit';

export interface UserFieldFormState {
    key: string;
    label: string;
    required: boolean;
    values: string[];
}

const EMPTY_FORM: UserFieldFormState = { key: '', label: '', required: false, values: [] };

function formFromField(field: UserField): UserFieldFormState {
    return { key: field.key, label: field.label, required: field.required, values: [...(field.values ?? [])] };
}

function isSameForm(a: UserFieldFormState, b: UserFieldFormState): boolean {
    if (a.label !== b.label) return false;
    if (a.required !== b.required) return false;
    if (a.values.length !== b.values.length) return false;
    return a.values.every((value, index) => value === b.values[index]);
}

export function useUserFieldForm({
    open,
    mode,
    field,
    onSubmit,
}: {
    open: boolean;
    mode: UserFieldFormMode;
    field: UserField | undefined;
    onSubmit: (payload: UserFieldPayload) => void;
}) {
    const isEdit = mode === 'edit';
    const [form, setForm] = useState<UserFieldFormState>(EMPTY_FORM);
    const [initialForm, setInitialForm] = useState<UserFieldFormState | null>(null);
    const [showErrors, setShowErrors] = useState(false);

    useEffect(() => {
        if (!open) return;
        setShowErrors(false);
        if (isEdit && field) {
            const initial = formFromField(field);
            setForm(initial);
            setInitialForm(initial);
        } else {
            setForm(EMPTY_FORM);
            setInitialForm(null);
        }
    }, [open, isEdit, field]);

    function setValue<K extends keyof UserFieldFormState>(name: K, value: UserFieldFormState[K]) {
        setForm(prev => ({ ...prev, [name]: value }));
    }

    // The key is immutable once stored on user profiles, so edit mode never re-validates it.
    const keyError = isEdit ? null : userFieldKeyError(form.key.trim());
    const labelError = userFieldLabelError(form.label);
    const isValid = keyError === null && labelError === null;
    const hasChanged = !isEdit || initialForm === null || !isSameForm(form, initialForm);

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!isValid) {
            setShowErrors(true);
            return;
        }
        if (!hasChanged) return;
        onSubmit({
            key: isEdit ? form.key : normalizeUserFieldKey(form.key),
            label: form.label.trim(),
            required: form.required,
            values: normalizeUserFieldValues(form.values),
        });
    }

    return {
        form,
        setValue,
        hasChanged,
        handleSubmit,
        keyError: showErrors ? keyError : null,
        labelError: showErrors ? labelError : null,
    };
}
