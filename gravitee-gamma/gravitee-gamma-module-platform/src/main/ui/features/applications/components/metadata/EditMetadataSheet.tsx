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
import { type FormEvent, useCallback, useEffect, useState } from 'react';

import { METADATA_FORMATS } from './metadataConstants';
import { metadataFormat, metadataValue } from './metadataHelpers';
import { MetadataValueField } from './MetadataValueField';
import type { ApplicationMetadata, UpdateApplicationMetadata } from '../../types/applicationNotification';
import { RequiredLabel } from '../notification-settings/RequiredLabel';

export function EditMetadataSheet({
    metadata,
    isSaving,
    onCancel,
    onSave,
}: Readonly<{
    metadata: ApplicationMetadata | null;
    isSaving: boolean;
    onCancel: () => void;
    onSave: (metadata: UpdateApplicationMetadata) => void;
}>) {
    const [name, setName] = useState('');
    const [value, setValue] = useState('');

    useEffect(() => {
        setName(metadata?.name ?? '');
        setValue(metadataValue(metadata));
    }, [metadata]);

    const format = metadataFormat(metadata);
    const initialName = metadata?.name ?? '';
    const initialValue = metadataValue(metadata);
    const hasChange = name !== initialName || value !== initialValue;
    const canSave = Boolean(metadata && name.trim().length > 0 && value.trim().length > 0 && hasChange);

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!metadata || !canSave) {
            return;
        }
        onSave({
            key: metadata.key,
            name: name.trim(),
            format,
            value: value.trim(),
            defaultValue: metadata.defaultValue,
        });
    }

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onCancel();
        },
        [onCancel],
    );

    return (
        <Sheet open={metadata !== null} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Update Application metadata</SheetTitle>
                    <SheetDescription>Update the display name and value for this metadata entry.</SheetDescription>
                </SheetHeader>

                <form id="edit-metadata-form" className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit}>
                    <ScrollArea className="min-h-0 flex-1">
                        <div className="space-y-4 px-4 pb-4">
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="edit-metadata-key">Key</RequiredLabel>
                                <Input id="edit-metadata-key" value={metadata?.key ?? ''} disabled required aria-required="true" />
                            </div>
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="edit-metadata-name">Name</RequiredLabel>
                                <Input
                                    id="edit-metadata-name"
                                    value={name}
                                    onChange={event => setName(event.target.value)}
                                    disabled={isSaving}
                                    required
                                    aria-required="true"
                                />
                            </div>
                            <div className="space-y-2">
                                <RequiredLabel htmlFor="edit-metadata-format">Format</RequiredLabel>
                                <Select value={format} disabled required>
                                    <SelectTrigger id="edit-metadata-format" className="w-full" aria-required="true">
                                        <SelectValue />
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
                                <RequiredLabel htmlFor="edit-metadata-value">Value</RequiredLabel>
                                <MetadataValueField
                                    id="edit-metadata-value"
                                    format={format}
                                    value={value}
                                    disabled={isSaving}
                                    onChange={setValue}
                                />
                            </div>
                        </div>
                    </ScrollArea>
                </form>

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onCancel} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="edit-metadata-form" disabled={!canSave || isSaving}>
                        {isSaving ? 'Saving…' : 'Save'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
