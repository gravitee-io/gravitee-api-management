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
import { useCallback, useEffect, useState, type FormEvent } from 'react';

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

interface MetadataForm {
    key: string;
    name: string;
    format: MetadataFormat;
    value: string;
}

const EMPTY_FORM: MetadataForm = { key: '', name: '', format: 'STRING', value: '' };

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

    useEffect(() => {
        if (!open) return;
        if (mode === 'edit' && metadata) {
            setForm({ key: metadata.key, name: metadata.name, format: metadata.format, value: metadata.value ?? '' });
        } else {
            setForm(EMPTY_FORM);
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

    const isValid = form.key.trim() !== '' && form.name.trim() !== '';

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!isValid) return;
        onSubmit({ key: form.key.trim(), name: form.name.trim(), format: form.format, value: form.value.trim() });
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>{mode === 'create' ? 'Add Global Metadata' : 'Edit Metadata'}</SheetTitle>
                    <SheetDescription>
                        {mode === 'create'
                            ? 'Define a new metadata key that will be inherited by all APIs in this environment.'
                            : 'Update the metadata value. The key cannot be changed after creation.'}
                    </SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    <form id="metadata-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-1 py-4">
                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="metadata-key">
                                Key{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <Input
                                id="metadata-key"
                                value={form.key}
                                onChange={e => setField('key', e.target.value)}
                                placeholder="e.g. support-email"
                                disabled={mode === 'edit' || isSaving}
                                required
                            />
                            {mode === 'edit' && <p className="text-xs text-muted-foreground">The key is fixed and cannot be changed.</p>}
                        </Field>

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
                            <FieldLabel htmlFor="metadata-format">Format</FieldLabel>
                            <Select
                                value={form.format}
                                onValueChange={val => setField('format', val as MetadataFormat)}
                                disabled={isSaving}
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
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="metadata-value">Default Value</FieldLabel>
                            <Input
                                id="metadata-value"
                                value={form.value}
                                onChange={e => setField('value', e.target.value)}
                                placeholder="Default value (optional)"
                                disabled={isSaving}
                            />
                            <p className="text-xs text-muted-foreground">APIs can override this. Leave blank to have no default.</p>
                        </Field>
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="metadata-form" disabled={!isValid || isSaving}>
                        {isSaving ? (mode === 'create' ? 'Adding…' : 'Updating…') : mode === 'create' ? 'Add' : 'Update'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
