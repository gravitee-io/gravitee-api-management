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
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useState, type FormEvent } from 'react';

import { extractErrorMessage } from '../../../shared/notify/extractErrorMessage';
import type { NewDictionaryPayload } from '../types/dictionary';

const NAME_MIN = 3;
const NAME_MAX = 50;

interface CreateDictionaryForm {
    name: string;
    description: string;
}

const EMPTY_FORM: CreateDictionaryForm = {
    name: '',
    description: '',
};

function getNameError(name: string): string | null {
    const trimmed = name.trim();
    if (!trimmed) return null;
    if (trimmed.length < NAME_MIN) return `Name must be at least ${NAME_MIN} characters`;
    if (trimmed.length > NAME_MAX) return `Name must be at most ${NAME_MAX} characters`;
    return null;
}

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

    const nameError = getNameError(form.name);
    const isNameValid = form.name.trim().length >= NAME_MIN && form.name.trim().length <= NAME_MAX;
    const isValid = isNameValid && nameError === null;

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!isValid || isSaving) return;

        const payload: NewDictionaryPayload = {
            name: form.name.trim(),
            description: form.description.trim() || undefined,
            type: 'MANUAL',
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
                    <SheetDescription>Define a new manual dictionary with a name and description.</SheetDescription>
                </SheetHeader>

                <ScrollArea className="min-h-0 flex-1">
                    <form id="create-dictionary-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-1 py-4">
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
                                minLength={NAME_MIN}
                                maxLength={NAME_MAX}
                                aria-invalid={nameError !== null}
                                aria-describedby={nameError !== null ? 'dictionary-name-error' : 'dictionary-name-hint'}
                            />
                            {nameError ? (
                                <p id="dictionary-name-error" className="text-sm text-destructive" role="alert">
                                    {nameError}
                                </p>
                            ) : (
                                <p id="dictionary-name-hint" className="text-xs text-muted-foreground">
                                    Max {NAME_MAX} characters.
                                </p>
                            )}
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="dictionary-description">Description</FieldLabel>
                            <Input
                                id="dictionary-description"
                                value={form.description}
                                onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))}
                                placeholder="Provide a description of the dictionary"
                                disabled={isSaving}
                            />
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel>Type</FieldLabel>
                            <p id="dictionary-type" className="text-sm text-foreground">
                                Manual
                            </p>
                            <p className="text-xs text-muted-foreground">Dynamic dictionaries will be available in a later story.</p>
                        </Field>

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
