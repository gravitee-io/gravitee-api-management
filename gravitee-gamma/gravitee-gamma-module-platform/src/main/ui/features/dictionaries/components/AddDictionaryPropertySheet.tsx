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

export function AddDictionaryPropertySheet({
    open,
    onClose,
    onSubmit,
    isSaving,
    existingKeys,
}: Readonly<{
    open: boolean;
    onClose: () => void;
    onSubmit: (property: { key: string; value: string }) => Promise<void>;
    isSaving: boolean;
    existingKeys: string[];
}>) {
    const [key, setKey] = useState('');
    const [value, setValue] = useState('');

    useEffect(() => {
        if (!open) return;
        setKey('');
        setValue('');
    }, [open]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    const trimmedKey = key.trim();
    const canSubmit = trimmedKey.length > 0 && !isSaving;

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!canSubmit) return;
        if (existingKeys.includes(trimmedKey)) {
            notify.error(`Property key "${trimmedKey}" already exists`);
            return;
        }
        await onSubmit({ key: trimmedKey, value });
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Add Property</SheetTitle>
                    <SheetDescription>Add a key/value entry to this dictionary.</SheetDescription>
                </SheetHeader>

                <form id="add-dictionary-property-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-1 py-4">
                    <Field orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor="property-key">
                            Key{' '}
                            <span className="text-destructive" aria-hidden>
                                *
                            </span>
                        </FieldLabel>
                        <Input
                            id="property-key"
                            value={key}
                            onChange={e => setKey(e.target.value)}
                            placeholder="e.g. FRA"
                            disabled={isSaving}
                            required
                            className="font-mono text-sm"
                        />
                    </Field>
                    <Field orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor="property-value">Value</FieldLabel>
                        <Input
                            id="property-value"
                            value={value}
                            onChange={e => setValue(e.target.value)}
                            placeholder="e.g. Frankfurt"
                            disabled={isSaving}
                        />
                    </Field>
                </form>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="add-dictionary-property-form" disabled={!canSubmit}>
                        {isSaving ? 'Adding…' : 'Add'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
