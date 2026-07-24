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
    Textarea,
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useState, type FormEvent } from 'react';

import { extractErrorMessage } from '../../../shared/notify/extractErrorMessage';
import type { DictionaryType, NewDictionaryPayload } from '../types/dictionary';
import {
    DEFAULT_JOLT_SPECIFICATION,
    DICTIONARY_NAME_MAX,
    getDictionaryNameError,
    isDictionaryNameValid,
} from '../utils/dictionaryFormValidation';
import {
    DictionaryHttpProviderFields,
    isHttpProviderFormValid,
    toProviderHeaders,
    type DictionaryHttpProviderFormValue,
} from './DictionaryHttpProviderFields';
import { DictionaryTriggerFields, isTriggerFormValid, type DictionaryTriggerFormValue } from './DictionaryTriggerFields';

interface CreateDictionaryForm {
    name: string;
    description: string;
    type: DictionaryType;
    trigger: DictionaryTriggerFormValue;
    provider: DictionaryHttpProviderFormValue;
}

const EMPTY_FORM: CreateDictionaryForm = {
    name: '',
    description: '',
    type: 'MANUAL',
    trigger: { rate: '1', unit: 'MINUTES' },
    provider: {
        url: '',
        method: 'GET',
        body: '',
        headers: [],
        specification: DEFAULT_JOLT_SPECIFICATION,
        useSystemProxy: false,
    },
};

export function CreateDictionarySheet({
    open,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    onClose: () => void;
    onSubmit: (data: NewDictionaryPayload) => Promise<void>;
    isSaving: boolean;
}>) {
    const [form, setForm] = useState<CreateDictionaryForm>(EMPTY_FORM);
    const [submitError, setSubmitError] = useState<string | null>(null);

    useEffect(() => {
        if (!open) return;
        setForm(EMPTY_FORM);
        setSubmitError(null);
    }, [open]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    const nameError = getDictionaryNameError(form.name);
    const isNameValid = isDictionaryNameValid(form.name);
    const isDynamicValid = form.type === 'MANUAL' || (isTriggerFormValid(form.trigger) && isHttpProviderFormValid(form.provider));
    const isValid = isNameValid && nameError === null && isDynamicValid;

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!isValid || isSaving) return;

        const payload: NewDictionaryPayload =
            form.type === 'MANUAL'
                ? {
                      name: form.name.trim(),
                      description: form.description.trim() || undefined,
                      type: 'MANUAL',
                  }
                : {
                      name: form.name.trim(),
                      description: form.description.trim() || undefined,
                      type: 'DYNAMIC',
                      trigger: {
                          rate: Number(form.trigger.rate),
                          unit: form.trigger.unit,
                      },
                      provider: {
                          type: 'HTTP',
                          configuration: {
                              url: form.provider.url.trim(),
                              method: form.provider.method,
                              body: form.provider.body.trim() || undefined,
                              headers: toProviderHeaders(form.provider.headers),
                              specification: form.provider.specification.trim(),
                              useSystemProxy: form.provider.useSystemProxy,
                          },
                      },
                  };

        try {
            setSubmitError(null);
            await onSubmit(payload);
        } catch (error) {
            setSubmitError(extractErrorMessage(error, 'Failed to create dictionary'));
        }
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '560px' }}>
                <SheetHeader>
                    <SheetTitle>Create Dictionary</SheetTitle>
                    <SheetDescription>
                        Define a new dictionary. For dynamic dictionaries, configure the trigger and HTTP provider.
                    </SheetDescription>
                </SheetHeader>

                <ScrollArea className="min-h-0 flex-1">
                    <form id="create-dictionary-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-4 py-4">
                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="dictionary-name">
                                Name{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <Input
                                id="dictionary-name"
                                value={form.name}
                                onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))}
                                placeholder="e.g. Airport IATA Codes"
                                disabled={isSaving}
                                required
                                minLength={3}
                                maxLength={DICTIONARY_NAME_MAX}
                                aria-invalid={nameError !== null}
                                aria-describedby={nameError !== null ? 'dictionary-name-error' : 'dictionary-name-hint'}
                            />
                            {nameError ? (
                                <p id="dictionary-name-error" className="text-sm text-destructive" role="alert">
                                    {nameError}
                                </p>
                            ) : (
                                <p id="dictionary-name-hint" className="text-xs text-muted-foreground">
                                    Max {DICTIONARY_NAME_MAX} characters.
                                </p>
                            )}
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="dictionary-description">Description</FieldLabel>
                            <Textarea
                                id="dictionary-description"
                                value={form.description}
                                onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))}
                                placeholder="Provide a description of the dictionary"
                                disabled={isSaving}
                                rows={3}
                            />
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="dictionary-type">
                                Type{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <Select
                                value={form.type}
                                onValueChange={type => setForm(prev => ({ ...prev, type: type as DictionaryType }))}
                                disabled={isSaving}
                            >
                                <SelectTrigger id="dictionary-type">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="MANUAL">Manual</SelectItem>
                                    <SelectItem value="DYNAMIC">Dynamic</SelectItem>
                                </SelectContent>
                            </Select>
                        </Field>

                        {form.type === 'DYNAMIC' ? (
                            <>
                                <DictionaryTriggerFields
                                    value={form.trigger}
                                    onChange={trigger => setForm(prev => ({ ...prev, trigger }))}
                                    disabled={isSaving}
                                />
                                <DictionaryHttpProviderFields
                                    value={form.provider}
                                    onChange={provider => setForm(prev => ({ ...prev, provider }))}
                                    disabled={isSaving}
                                />
                            </>
                        ) : null}

                        {submitError ? (
                            <p className="text-sm text-destructive" role="alert">
                                {submitError}
                            </p>
                        ) : null}
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-col gap-2 border-t pt-4 sm:flex-col">
                    <Button type="button" variant="outline" className="w-full" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="create-dictionary-form" className="w-full" disabled={!isValid || isSaving}>
                        {isSaving ? 'Creating…' : 'Create'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
