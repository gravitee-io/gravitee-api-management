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
    Alert,
    AlertDescription,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { type FormEvent, useMemo, useState } from 'react';

import { DeleteMetadataDialog } from './DeleteMetadataDialog';
import { EditMetadataDialog } from './EditMetadataDialog';
import { METADATA_FORMATS } from './metadataConstants';
import { deriveMetadataKey } from './metadataHelpers';
import { MetadataTable } from './MetadataTable';
import { MetadataValueField } from './MetadataValueField';
import type {
    ApplicationMetadata,
    ApplicationMetadataFormat,
    NewApplicationMetadata,
    UpdateApplicationMetadata,
} from '../../types/applicationNotification';
import { RequiredLabel } from '../notification-settings/RequiredLabel';

export function ApplicationMetadataSection({
    metadata,
    isLoading,
    isError,
    canCreate,
    canUpdate,
    canDelete,
    isCreating,
    isUpdating,
    isDeleting,
    mutationErrorMessage,
    onCreate,
    onUpdate,
    onDelete,
}: Readonly<{
    metadata: ApplicationMetadata[];
    isLoading: boolean;
    isError: boolean;
    canCreate: boolean;
    canUpdate: boolean;
    canDelete: boolean;
    isCreating: boolean;
    isUpdating: boolean;
    isDeleting: boolean;
    mutationErrorMessage: string | null;
    onCreate: (payload: NewApplicationMetadata) => Promise<void>;
    onUpdate: (payload: UpdateApplicationMetadata) => Promise<void>;
    onDelete: (metadataKey: string) => Promise<void>;
}>) {
    const [metadataName, setMetadataName] = useState('');
    const [metadataFormat, setMetadataFormat] = useState<ApplicationMetadataFormat>('STRING');
    const [metadataValue, setMetadataValue] = useState('');
    const [metadataToEdit, setMetadataToEdit] = useState<ApplicationMetadata | null>(null);
    const [metadataToDelete, setMetadataToDelete] = useState<ApplicationMetadata | null>(null);

    const metadataKey = useMemo(() => deriveMetadataKey(metadataName), [metadataName]);
    const canSubmitMetadata =
        canCreate && metadataKey.length > 0 && metadataName.trim().length > 0 && metadataFormat.length > 0 && metadataValue.length > 0;

    async function handleAddMetadata(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!canSubmitMetadata) {
            return;
        }
        try {
            await onCreate({ name: metadataName.trim(), format: metadataFormat, value: metadataValue });
            setMetadataName('');
            setMetadataFormat('STRING');
            setMetadataValue('');
        } catch {
            // Error surfaced via mutationErrorMessage from the page
        }
    }

    async function handleDeleteMetadataConfirm() {
        if (!metadataToDelete) {
            return;
        }
        try {
            await onDelete(metadataToDelete.key);
            setMetadataToDelete(null);
        } catch {
            // Error surfaced via mutationErrorMessage from the page
        }
    }

    async function handleUpdateMetadata(updatedMetadata: UpdateApplicationMetadata) {
        try {
            await onUpdate(updatedMetadata);
            setMetadataToEdit(null);
        } catch {
            // Error surfaced via mutationErrorMessage from the page
        }
    }

    return (
        <>
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Metadata</CardTitle>
                    <CardDescription>
                        Custom metadata available in notification templates. Key is assigned when metadata is created.
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                    {isError ? (
                        <Alert variant="destructive">
                            <AlertDescription>Failed to load metadata. Please refresh the page.</AlertDescription>
                        </Alert>
                    ) : null}
                    {mutationErrorMessage ? (
                        <Alert variant="destructive">
                            <AlertDescription>{mutationErrorMessage}</AlertDescription>
                        </Alert>
                    ) : null}

                    {canCreate ? (
                        <form className="space-y-3" onSubmit={handleAddMetadata}>
                            <div className="grid gap-3 md:grid-cols-3">
                                <div className="space-y-2">
                                    <RequiredLabel htmlFor="metadata-name">Name</RequiredLabel>
                                    <Input
                                        id="metadata-name"
                                        value={metadataName}
                                        onChange={event => setMetadataName(event.target.value)}
                                        placeholder="Department"
                                        disabled={isCreating}
                                        required
                                        aria-required="true"
                                    />
                                </div>
                                <div className="space-y-2">
                                    <RequiredLabel htmlFor="metadata-format">Format</RequiredLabel>
                                    <Select
                                        value={metadataFormat}
                                        onValueChange={value => {
                                            const nextFormat = value as ApplicationMetadataFormat;
                                            setMetadataFormat(nextFormat);
                                            setMetadataValue(nextFormat === 'BOOLEAN' ? 'false' : '');
                                        }}
                                        disabled={isCreating}
                                        required
                                    >
                                        <SelectTrigger id="metadata-format" className="w-full" aria-required="true">
                                            <SelectValue placeholder="Select a format" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {METADATA_FORMATS.map(format => (
                                                <SelectItem key={format} value={format}>
                                                    {format.toLowerCase()}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="space-y-2">
                                    <RequiredLabel htmlFor="metadata-value">Value</RequiredLabel>
                                    <MetadataValueField
                                        id="metadata-value"
                                        format={metadataFormat}
                                        value={metadataValue}
                                        disabled={isCreating}
                                        onChange={setMetadataValue}
                                    />
                                </div>
                            </div>
                            <Button type="submit" size="sm" disabled={!canSubmitMetadata || isCreating}>
                                <PlusIcon className="size-4" aria-hidden />
                                {isCreating ? 'Adding…' : 'Add metadata'}
                            </Button>
                        </form>
                    ) : null}

                    <MetadataTable
                        metadata={metadata}
                        isLoading={isLoading}
                        canUpdate={canUpdate}
                        canDelete={canDelete}
                        isMutating={isUpdating || isDeleting}
                        onEdit={setMetadataToEdit}
                        onDelete={setMetadataToDelete}
                    />
                </CardContent>
            </Card>
            <DeleteMetadataDialog
                metadata={metadataToDelete}
                isDeleting={isDeleting}
                onCancel={() => setMetadataToDelete(null)}
                onConfirm={handleDeleteMetadataConfirm}
            />
            <EditMetadataDialog
                metadata={metadataToEdit}
                isSaving={isUpdating}
                onCancel={() => setMetadataToEdit(null)}
                onSave={handleUpdateMetadata}
            />
        </>
    );
}
