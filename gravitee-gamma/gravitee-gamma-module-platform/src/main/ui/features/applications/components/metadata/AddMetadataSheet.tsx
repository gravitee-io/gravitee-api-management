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
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react';

import { METADATA_FORMATS } from './metadataConstants';
import { deriveMetadataKey } from './metadataHelpers';
import { MetadataValueField } from './MetadataValueField';
import type { ApplicationMetadataFormat, NewApplicationMetadata } from '../../types/applicationNotification';
import { RequiredLabel } from '../notification-settings/RequiredLabel';

function defaultFormState() {
    return {
        name: '',
        format: 'STRING' as ApplicationMetadataFormat,
        value: '',
    };
}

export function AddMetadataSheet({
    open,
    onOpenChange,
    isCreating,
    onCreate,
}: Readonly<{
    open: boolean;
    onOpenChange: (open: boolean) => void;
    isCreating: boolean;
    onCreate: (payload: NewApplicationMetadata) => Promise<void>;
}>) {
    const [name, setName] = useState('');
    const [format, setFormat] = useState<ApplicationMetadataFormat>('STRING');
    const [value, setValue] = useState('');

    const metadataKey = useMemo(() => deriveMetadataKey(name), [name]);
    const canSubmit = metadataKey.length > 0 && name.trim().length > 0 && format.length > 0 && value.length > 0;

    const resetForm = useCallback(() => {
        const defaults = defaultFormState();
        setName(defaults.name);
        setFormat(defaults.format);
        setValue(defaults.value);
    }, []);

    useEffect(() => {
        if (!open) {
            resetForm();
        }
    }, [open, resetForm]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            onOpenChange(isOpen);
        },
        [onOpenChange],
    );

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!canSubmit || isCreating) {
            return;
        }
        try {
            await onCreate({ name: name.trim(), format, value });
            resetForm();
            onOpenChange(false);
        } catch {
            // Error surfaced via mutationErrorMessage from the page
        }
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Add metadata</SheetTitle>
                    <SheetDescription>
                        Create custom metadata for notification templates. The key is derived from the name when saved.
                    </SheetDescription>
                </SheetHeader>

                <form id="add-metadata-form" className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit}>
                    <ScrollArea className="min-h-0 flex-1">
                        <div className="space-y-4 px-4 pb-4">
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="add-metadata-name">Name</RequiredLabel>
                                <Input
                                    id="add-metadata-name"
                                    value={name}
                                    onChange={event => setName(event.target.value)}
                                    placeholder="Department"
                                    disabled={isCreating}
                                    required
                                    aria-required="true"
                                />
                            </div>
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="add-metadata-format">Format</RequiredLabel>
                                <Select
                                    value={format}
                                    onValueChange={nextValue => {
                                        const nextFormat = nextValue as ApplicationMetadataFormat;
                                        setFormat(nextFormat);
                                        setValue(nextFormat === 'BOOLEAN' ? 'false' : '');
                                    }}
                                    disabled={isCreating}
                                    required
                                >
                                    <SelectTrigger id="add-metadata-format" className="w-full" aria-required="true">
                                        <SelectValue placeholder="Select a format" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {METADATA_FORMATS.map(item => (
                                            <SelectItem key={item} value={item}>
                                                {item.toLowerCase()}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="add-metadata-value">Value</RequiredLabel>
                                <MetadataValueField
                                    id="add-metadata-value"
                                    format={format}
                                    value={value}
                                    disabled={isCreating}
                                    onChange={setValue}
                                />
                            </div>
                        </div>
                    </ScrollArea>
                </form>

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isCreating}>
                        Cancel
                    </Button>
                    <Button type="submit" form="add-metadata-form" disabled={!canSubmit || isCreating}>
                        {isCreating ? 'Adding…' : 'Add metadata'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
