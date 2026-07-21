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
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';

import type { Metadata, MetadataFormat, NewMetadataPayload, UpdateMetadataPayload } from '../types/metadata';

const METADATA_FORMATS: MetadataFormat[] = ['STRING', 'NUMERIC', 'BOOLEAN', 'DATE', 'MAIL', 'URL'];

const FORMAT_LABELS: Record<MetadataFormat, string> = {
    STRING: 'String',
    NUMERIC: 'Numeric',
    BOOLEAN: 'Boolean',
    DATE: 'Date',
    MAIL: 'Mail',
    URL: 'URL',
};

const MAIL_REGEX =
    /^((\${.+})|(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,})))$/;
const URL_REGEX = /^((\$\{.+\})|(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})\b([-a-zA-Z0-9()@:%_+.~#?&//=!]*))$/;

interface MetadataForm {
    name: string;
    format: MetadataFormat;
    value: string;
}

const EMPTY_FORM: MetadataForm = { name: '', format: 'STRING', value: '' };

function isValueValid(format: MetadataFormat, value: string): boolean {
    if (format === 'BOOLEAN') return value === 'true' || value === 'false';
    if (!value.trim()) return false;
    if (format === 'NUMERIC') return !isNaN(Number(value));
    if (format === 'MAIL') return MAIL_REGEX.test(value);
    if (format === 'URL') return URL_REGEX.test(value);
    return true;
}

/** Empty values are not flagged so users can clear the field without seeing a format error. */
function getValueFormatError(format: MetadataFormat, value: string): string | null {
    if (!value.trim()) return null;
    if (format === 'MAIL' && !MAIL_REGEX.test(value)) return 'Invalid email';
    if (format === 'URL' && !URL_REGEX.test(value)) return 'Invalid URL';
    return null;
}

function getValueInputType(format: MetadataFormat): 'number' | 'date' | 'email' | 'url' | 'text' {
    if (format === 'NUMERIC') return 'number';
    if (format === 'DATE') return 'date';
    if (format === 'MAIL') return 'email';
    if (format === 'URL') return 'url';
    return 'text';
}

function getValuePlaceholder(format: MetadataFormat): string | undefined {
    if (format === 'NUMERIC') return 'e.g. 123';
    if (format === 'MAIL') return 'e.g. john@doe.com';
    if (format === 'URL') return 'e.g. https://gravitee.io';
    return undefined;
}

export type MetadataSheetMode = 'create' | 'edit';

export function MetadataSheet({
    open,
    mode,
    metadata,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    mode: MetadataSheetMode;
    metadata?: Metadata;
    onClose: () => void;
    onSubmit: (data: NewMetadataPayload | UpdateMetadataPayload) => void;
    isSaving: boolean;
}>) {
    const [form, setForm] = useState<MetadataForm>(EMPTY_FORM);
    const [initialForm, setInitialForm] = useState<MetadataForm | null>(null);

    useEffect(() => {
        if (!open) return;
        if (mode === 'edit' && metadata) {
            const initial = { name: metadata.name, format: metadata.format, value: metadata.value ?? '' };
            setForm(initial);
            setInitialForm(initial);
        } else {
            setForm(EMPTY_FORM);
            setInitialForm(null);
        }
    }, [open, mode, metadata]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    function setField<K extends keyof MetadataForm>(key: K, value: MetadataForm[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
    }

    function handleFormatChange(newFormat: MetadataFormat) {
        // Reset value on format change; BOOLEAN defaults to 'false'
        setForm(prev => ({ ...prev, format: newFormat, value: newFormat === 'BOOLEAN' ? 'false' : '' }));
    }

    const isValid = form.name.trim() !== '' && isValueValid(form.format, form.value);
    const valueFormatError = getValueFormatError(form.format, form.value);

    const hasChanged = useMemo(() => {
        if (mode === 'create') return true;
        if (!initialForm) return false;
        return form.name !== initialForm.name || form.value !== initialForm.value;
    }, [mode, form, initialForm]);

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!isValid || !hasChanged) return;
        const base = { name: form.name.trim(), format: form.format, value: form.value.trim() };
        if (mode === 'edit' && metadata) {
            onSubmit({ ...base, key: metadata.key } satisfies UpdateMetadataPayload);
        } else {
            onSubmit(base satisfies NewMetadataPayload);
        }
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>{mode === 'create' ? 'Add Global Metadata' : 'Edit Metadata'}</SheetTitle>
                    <SheetDescription>
                        {mode === 'create'
                            ? 'Define a new metadata key that will be inherited by all APIs in this environment.'
                            : 'Update the metadata value. Format cannot be changed after creation.'}
                    </SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    <form id="metadata-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-1 py-4">
                        {mode === 'edit' && metadata && (
                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="metadata-key">Key</FieldLabel>
                                <Input id="metadata-key" value={metadata.key} disabled readOnly />
                            </Field>
                        )}

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="metadata-name">
                                Name{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <Input
                                id="metadata-name"
                                value={form.name}
                                onChange={e => setField('name', e.target.value)}
                                placeholder="e.g. Support Email"
                                disabled={isSaving}
                                required
                            />
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="metadata-format">
                                Format{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <Select
                                value={form.format}
                                onValueChange={val => handleFormatChange(val as MetadataFormat)}
                                disabled={mode === 'edit' || isSaving}
                            >
                                <SelectTrigger id="metadata-format">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {METADATA_FORMATS.map(fmt => (
                                        <SelectItem key={fmt} value={fmt}>
                                            {FORMAT_LABELS[fmt]}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            {mode === 'edit' && <p className="text-xs text-muted-foreground">Format cannot be changed after creation.</p>}
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="metadata-value">
                                Value{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            {form.format === 'BOOLEAN' ? (
                                <Select value={form.value} onValueChange={val => setField('value', val)} disabled={isSaving}>
                                    <SelectTrigger id="metadata-value">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="true">true</SelectItem>
                                        <SelectItem value="false">false</SelectItem>
                                    </SelectContent>
                                </Select>
                            ) : (
                                <Input
                                    id="metadata-value"
                                    value={form.value}
                                    onChange={e => setField('value', e.target.value)}
                                    type={getValueInputType(form.format)}
                                    placeholder={getValuePlaceholder(form.format)}
                                    disabled={isSaving}
                                    required
                                    aria-invalid={valueFormatError != null}
                                    aria-describedby={valueFormatError ? 'metadata-value-error' : undefined}
                                />
                            )}
                            {valueFormatError ? (
                                <p id="metadata-value-error" className="text-sm text-destructive" role="alert">
                                    {valueFormatError}
                                </p>
                            ) : null}
                        </Field>
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="metadata-form" disabled={!isValid || !hasChanged || isSaving}>
                        {isSaving ? (mode === 'create' ? 'Adding…' : 'Updating…') : mode === 'create' ? 'Add' : 'Update'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
