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
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { type FormEvent, useEffect, useState } from 'react';

import { METADATA_FORMATS } from './metadataConstants';
import { metadataFormat, metadataValue } from './metadataHelpers';
import { MetadataValueField } from './MetadataValueField';
import type { ApplicationMetadata, UpdateApplicationMetadata } from '../../types/applicationNotification';
import { RequiredLabel } from '../notification-settings/RequiredLabel';

export function EditMetadataDialog({
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
            value,
            defaultValue: metadata.defaultValue,
        });
    }

    return (
        <Dialog open={metadata !== null} onOpenChange={open => !open && onCancel()}>
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Update Application metadata</DialogTitle>
                </DialogHeader>
                <form className="space-y-4" onSubmit={handleSubmit}>
                    <div className="grid gap-3 md:grid-cols-2">
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
                    <DialogFooter className="border-t px-6 py-4 gap-2">
                        <Button type="button" variant="outline" onClick={onCancel} disabled={isSaving}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!canSave || isSaving}>
                            {isSaving ? 'Saving…' : 'Save'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
