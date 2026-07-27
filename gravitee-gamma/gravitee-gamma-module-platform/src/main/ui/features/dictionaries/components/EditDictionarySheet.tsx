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
    Skeleton,
    Textarea,
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';

import {
    DictionaryHttpProviderFields,
    HTTP_METHOD_OPTIONS,
    isHttpProviderFormValid,
    toProviderHeaders,
    type DictionaryHttpMethod,
    type DictionaryHttpProviderFormValue,
} from './DictionaryHttpProviderFields';
import { DictionaryTriggerFields, isTriggerFormValid, type DictionaryTriggerFormValue } from './DictionaryTriggerFields';
import { extractErrorMessage } from '../../../shared/notify/extractErrorMessage';
import type { Dictionary, DictionaryType, UpdateDictionaryPayload } from '../types/dictionary';
import {
    DEFAULT_JOLT_SPECIFICATION,
    DICTIONARY_NAME_MAX,
    getDictionaryNameError,
    isDictionaryNameValid,
} from '../utils/dictionaryFormValidation';

interface EditDictionaryForm {
    name: string;
    description: string;
    type: DictionaryType;
    trigger: DictionaryTriggerFormValue;
    provider: DictionaryHttpProviderFormValue;
}

function asHttpProviderConfig(configuration: unknown): Record<string, unknown> {
    if (configuration && typeof configuration === 'object') {
        return configuration as Record<string, unknown>;
    }
    return {};
}

function toHttpMethod(method: unknown): DictionaryHttpMethod {
    const value = typeof method === 'string' ? method.toUpperCase() : 'GET';
    return (HTTP_METHOD_OPTIONS as readonly string[]).includes(value) ? (value as DictionaryHttpMethod) : 'GET';
}

function dictionaryToForm(dictionary: Dictionary): EditDictionaryForm {
    const config = asHttpProviderConfig(dictionary.provider?.configuration);
    const headers = Array.isArray(config.headers) ? config.headers : [];

    return {
        name: dictionary.name ?? '',
        description: dictionary.description ?? '',
        type: dictionary.type,
        trigger: {
            rate: String(dictionary.trigger?.rate ?? 1),
            unit: dictionary.trigger?.unit ?? 'MINUTES',
        },
        provider: {
            url: typeof config.url === 'string' ? config.url : '',
            method: toHttpMethod(config.method),
            body: typeof config.body === 'string' ? config.body : '',
            headers: headers.map(header => {
                const row = header && typeof header === 'object' ? (header as { name?: string; value?: string }) : {};
                return {
                    id: crypto.randomUUID(),
                    name: row.name ?? '',
                    value: row.value ?? '',
                };
            }),
            specification:
                typeof config.specification === 'string' && config.specification.trim() ? config.specification : DEFAULT_JOLT_SPECIFICATION,
            useSystemProxy: Boolean(config.useSystemProxy),
        },
    };
}

const EMPTY_FORM: EditDictionaryForm = {
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

export function EditDictionarySheet({
    open,
    dictionary,
    isLoading = false,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    dictionary: Dictionary | undefined;
    isLoading?: boolean;
    onClose: () => void;
    onSubmit: (data: UpdateDictionaryPayload) => Promise<void>;
    isSaving: boolean;
}>) {
    const [form, setForm] = useState<EditDictionaryForm>(EMPTY_FORM);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const dictionaryId = dictionary?.id;
    /** Seed once per open session when detail data first arrives (list may open before cache is warm). */
    const seededDictionaryIdRef = useRef<string | null>(null);

    useEffect(() => {
        if (!open) {
            seededDictionaryIdRef.current = null;
            return;
        }
        if (!dictionary || !dictionaryId) {
            if (seededDictionaryIdRef.current === null) {
                setForm(EMPTY_FORM);
                setSubmitError(null);
            }
            return;
        }
        if (seededDictionaryIdRef.current === dictionaryId) {
            return;
        }
        seededDictionaryIdRef.current = dictionaryId;
        setForm(dictionaryToForm(dictionary));
        setSubmitError(null);
    }, [open, dictionaryId, dictionary]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    const nameError = getDictionaryNameError(form.name);
    const isNameValid = isDictionaryNameValid(form.name);
    const isDynamicValid = form.type === 'MANUAL' || (isTriggerFormValid(form.trigger) && isHttpProviderFormValid(form.provider));
    const isValid = Boolean(dictionary) && isNameValid && nameError === null && isDynamicValid;

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!dictionary || !isValid || isSaving) return;

        // Send "" so clearing description is persisted (JSON.stringify drops undefined).
        const description = form.description.trim();
        const payload: UpdateDictionaryPayload =
            form.type === 'MANUAL'
                ? {
                      name: form.name.trim(),
                      description,
                      type: 'MANUAL',
                      // Properties are managed on the detail page; preserve existing values.
                      properties: dictionary.properties ?? {},
                  }
                : {
                      name: form.name.trim(),
                      description,
                      type: 'DYNAMIC',
                      // Preserve existing properties; ?? {} so they are never omitted from the PUT.
                      properties: dictionary.properties ?? {},
                      trigger: {
                          rate: Number(form.trigger.rate),
                          unit: form.trigger.unit,
                      },
                      provider: {
                          type: dictionary.provider?.type || 'HTTP',
                          configuration: {
                              url: form.provider.url.trim(),
                              method: form.provider.method,
                              // Send "" so clearing body is persisted (JSON.stringify drops undefined).
                              body: form.provider.body.trim(),
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
            setSubmitError(extractErrorMessage(error, 'Failed to update dictionary'));
        }
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '560px' }}>
                <SheetHeader>
                    <SheetTitle>Edit Dictionary</SheetTitle>
                    <SheetDescription>Update dictionary details and configuration.</SheetDescription>
                </SheetHeader>

                <ScrollArea className="min-h-0 flex-1">
                    {isLoading || !dictionary ? (
                        <div className="flex flex-col gap-4 px-4 py-4">
                            <Skeleton className="h-10 w-full" />
                            <Skeleton className="h-20 w-full" />
                            <Skeleton className="h-10 w-full" />
                        </div>
                    ) : (
                        <form id="edit-dictionary-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-4 py-4">
                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="edit-dictionary-name">
                                    Name{' '}
                                    <span className="text-destructive" aria-hidden>
                                        *
                                    </span>
                                </FieldLabel>
                                <Input
                                    id="edit-dictionary-name"
                                    value={form.name}
                                    onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))}
                                    placeholder="e.g. Airport IATA Codes"
                                    disabled={isSaving}
                                    required
                                    minLength={3}
                                    maxLength={DICTIONARY_NAME_MAX}
                                    aria-invalid={nameError !== null}
                                    aria-describedby={nameError !== null ? 'edit-dictionary-name-error' : 'edit-dictionary-name-hint'}
                                />
                                {nameError ? (
                                    <p id="edit-dictionary-name-error" className="text-sm text-destructive" role="alert">
                                        {nameError}
                                    </p>
                                ) : (
                                    <p id="edit-dictionary-name-hint" className="text-xs text-muted-foreground">
                                        Max {DICTIONARY_NAME_MAX} characters.
                                    </p>
                                )}
                            </Field>

                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="edit-dictionary-description">Description</FieldLabel>
                                <Textarea
                                    id="edit-dictionary-description"
                                    value={form.description}
                                    onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))}
                                    placeholder="Provide a description of the dictionary"
                                    disabled={isSaving}
                                    rows={3}
                                />
                            </Field>

                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="edit-dictionary-type">
                                    Type{' '}
                                    <span className="text-destructive" aria-hidden>
                                        *
                                    </span>
                                </FieldLabel>
                                <Select value={form.type} disabled>
                                    <SelectTrigger id="edit-dictionary-type" aria-readonly="true">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="MANUAL">Manual</SelectItem>
                                        <SelectItem value="DYNAMIC">Dynamic</SelectItem>
                                    </SelectContent>
                                </Select>
                                <p className="text-xs text-muted-foreground">Type cannot be changed after creation.</p>
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
                    )}
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-col gap-2 border-t pt-4 sm:flex-col">
                    <Button type="button" variant="outline" className="w-full" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button
                        type="submit"
                        form="edit-dictionary-form"
                        className="w-full"
                        disabled={!isValid || isSaving || isLoading || !dictionary}
                    >
                        {isSaving ? 'Saving…' : 'Save Changes'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
