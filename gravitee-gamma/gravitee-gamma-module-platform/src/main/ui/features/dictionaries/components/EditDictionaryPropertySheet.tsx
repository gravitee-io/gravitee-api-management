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
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useState, type FormEvent } from 'react';

import { notify } from '../../../shared/notify';

export function EditDictionaryPropertySheet({
    open,
    property,
    onClose,
    onSubmit,
    isSaving,
    existingKeys,
}: Readonly<{
    open: boolean;
    property: { key: string; value: string } | undefined;
    onClose: () => void;
    onSubmit: (next: { originalKey: string; key: string; value: string }) => Promise<void>;
    isSaving: boolean;
    existingKeys: string[];
}>) {
    const [key, setKey] = useState('');
    const [value, setValue] = useState('');

    useEffect(() => {
        if (!open || !property) return;
        setKey(property.key);
        setValue(property.value);
    }, [open, property]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    const originalKey = property?.key ?? '';
    const trimmedKey = key.trim();
    const canSubmit = Boolean(property) && trimmedKey.length > 0 && !isSaving;

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!property || !canSubmit) return;
        if (trimmedKey !== originalKey && existingKeys.includes(trimmedKey)) {
            notify.error(`Property key "${trimmedKey}" already exists`);
            return;
        }
        await onSubmit({ originalKey, key: trimmedKey, value });
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Edit Property</SheetTitle>
                    <SheetDescription>Property keys must be unique within a dictionary.</SheetDescription>
                </SheetHeader>

                <form id="edit-dictionary-property-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-4 py-4">
                    <Field orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor="edit-property-key">
                            Key <span className="text-destructive">*</span>
                        </FieldLabel>
                        <Input
                            id="edit-property-key"
                            value={key}
                            onChange={e => setKey(e.target.value)}
                            placeholder="e.g. MUC"
                            disabled={isSaving}
                            required
                            className="font-mono text-sm"
                        />
                    </Field>
                    <Field orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor="edit-property-value">Value</FieldLabel>
                        <Input
                            id="edit-property-value"
                            value={value}
                            onChange={e => setValue(e.target.value)}
                            placeholder="e.g. Munich"
                            disabled={isSaving}
                        />
                    </Field>
                </form>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="edit-dictionary-property-form" disabled={!canSubmit}>
                        {isSaving ? 'Updating…' : 'Update'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
