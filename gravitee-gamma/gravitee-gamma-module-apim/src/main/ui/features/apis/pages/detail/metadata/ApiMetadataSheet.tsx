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

import type { ApiMetadata, MetadataFormat, NewApiMetadataPayload, UpdateApiMetadataPayload } from '../../../types/metadata';
import {
    editMetadataValue,
    getMetadataValueFormatError,
    getMetadataValueInputType,
    getMetadataValuePlaceholder,
    isMetadataValueValid,
    METADATA_FORMAT_LABELS,
    METADATA_FORMATS,
} from '../../../utils/apiMetadata';

interface MetadataForm {
    name: string;
    format: MetadataFormat;
    value: string;
}

const EMPTY_FORM: MetadataForm = { name: '', format: 'STRING', value: '' };

export type ApiMetadataSheetMode = 'create' | 'edit';

export function ApiMetadataSheet({
    open,
    mode,
    metadata,
    readOnly = false,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    mode: ApiMetadataSheetMode;
    metadata?: ApiMetadata;
    readOnly?: boolean;
    onClose: () => void;
    onSubmit: (data: NewApiMetadataPayload | UpdateApiMetadataPayload) => void;
    isSaving: boolean;
}>) {
    const [form, setForm] = useState<MetadataForm>(EMPTY_FORM);
    const [initialForm, setInitialForm] = useState<MetadataForm | null>(null);

    useEffect(() => {
        if (!open) return;
        if (mode === 'edit' && metadata) {
            const initial = { name: metadata.name, format: metadata.format, value: editMetadataValue(metadata) };
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
        setForm(prev => ({ ...prev, format: newFormat, value: newFormat === 'BOOLEAN' ? 'false' : '' }));
    }

    const fieldsDisabled = isSaving || readOnly;
    const isValid = form.name.trim() !== '' && isMetadataValueValid(form.format, form.value);
    const valueFormatError = getMetadataValueFormatError(form.format, form.value);

    const hasChanged = useMemo(() => {
        if (mode === 'create') return true;
        if (!initialForm) return false;
        return form.name !== initialForm.name || form.value !== initialForm.value;
    }, [mode, form, initialForm]);

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (readOnly || !isValid || !hasChanged) return;
        const base = { name: form.name.trim(), format: form.format, value: form.value.trim() };
        if (mode === 'edit' && metadata) {
            onSubmit({ ...base, key: metadata.key, defaultValue: metadata.defaultValue } satisfies UpdateApiMetadataPayload);
        } else {
            onSubmit(base satisfies NewApiMetadataPayload);
        }
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>{mode === 'create' ? 'Add API Metadata' : 'Edit Metadata'}</SheetTitle>
                    <SheetDescription>
                        {mode === 'create'
                            ? 'Define metadata on this API that can be accessed through Markdown templating.'
                            : 'Update the metadata value. Format cannot be changed after creation.'}
                    </SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    <form id="api-metadata-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-1 py-4">
                        {mode === 'edit' && metadata && (
                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="api-metadata-key">Key</FieldLabel>
                                <Input id="api-metadata-key" value={metadata.key} disabled readOnly />
                            </Field>
                        )}

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="api-metadata-name">
                                Name{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <Input
                                id="api-metadata-name"
                                value={form.name}
                                onChange={e => setField('name', e.target.value)}
                                placeholder="e.g. Support Email"
                                disabled={fieldsDisabled}
                                required
                            />
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="api-metadata-format">
                                Format{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <Select
                                value={form.format}
                                onValueChange={val => handleFormatChange(val as MetadataFormat)}
                                disabled={mode === 'edit' || fieldsDisabled}
                            >
                                <SelectTrigger id="api-metadata-format">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {METADATA_FORMATS.map(fmt => (
                                        <SelectItem key={fmt} value={fmt}>
                                            {METADATA_FORMAT_LABELS[fmt]}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            {mode === 'edit' && <p className="text-xs text-muted-foreground">Format cannot be changed after creation.</p>}
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="api-metadata-value">
                                Value{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            {form.format === 'BOOLEAN' ? (
                                <Select value={form.value} onValueChange={val => setField('value', val)} disabled={fieldsDisabled}>
                                    <SelectTrigger id="api-metadata-value">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="true">true</SelectItem>
                                        <SelectItem value="false">false</SelectItem>
                                    </SelectContent>
                                </Select>
                            ) : (
                                <Input
                                    id="api-metadata-value"
                                    value={form.value}
                                    onChange={e => setField('value', e.target.value)}
                                    type={getMetadataValueInputType(form.format)}
                                    placeholder={getMetadataValuePlaceholder(form.format)}
                                    disabled={fieldsDisabled}
                                    required
                                    aria-invalid={valueFormatError !== null}
                                    aria-describedby={valueFormatError ? 'api-metadata-value-error' : undefined}
                                />
                            )}
                            {valueFormatError ? (
                                <p id="api-metadata-value-error" className="text-sm text-destructive" role="alert">
                                    {valueFormatError}
                                </p>
                            ) : null}
                        </Field>
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        {readOnly ? 'Close' : 'Cancel'}
                    </Button>
                    {!readOnly && (
                        <Button type="submit" form="api-metadata-form" disabled={!isValid || !hasChanged || isSaving}>
                            {isSaving ? (mode === 'create' ? 'Adding…' : 'Updating…') : mode === 'create' ? 'Add' : 'Update'}
                        </Button>
                    )}
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
