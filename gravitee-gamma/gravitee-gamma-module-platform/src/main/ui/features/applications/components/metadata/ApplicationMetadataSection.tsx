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
import { Alert, AlertDescription, Button, Card, CardContent, CardDescription, CardHeader, CardTitle } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { AddMetadataSheet } from './AddMetadataSheet';
import { DeleteMetadataDialog } from './DeleteMetadataDialog';
import { EditMetadataSheet } from './EditMetadataSheet';
import { MetadataTable } from './MetadataTable';
import type { ApplicationMetadata, NewApplicationMetadata, UpdateApplicationMetadata } from '../../types/applicationNotification';

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
    const [addOpen, setAddOpen] = useState(false);
    const [metadataToEdit, setMetadataToEdit] = useState<ApplicationMetadata | null>(null);
    const [metadataToDelete, setMetadataToDelete] = useState<ApplicationMetadata | null>(null);

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
                <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                    <div className="space-y-1.5">
                        <CardTitle className="text-base">Metadata</CardTitle>
                        <CardDescription>
                            Custom metadata available in notification templates. Key is assigned when metadata is created.
                        </CardDescription>
                    </div>
                    {canCreate ? (
                        <Button type="button" size="sm" className="shrink-0" onClick={() => setAddOpen(true)}>
                            <PlusIcon className="size-4" aria-hidden />
                            Add metadata
                        </Button>
                    ) : null}
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

            {canCreate ? <AddMetadataSheet open={addOpen} onOpenChange={setAddOpen} isCreating={isCreating} onCreate={onCreate} /> : null}

            <DeleteMetadataDialog
                metadata={metadataToDelete}
                isDeleting={isDeleting}
                onCancel={() => setMetadataToDelete(null)}
                onConfirm={handleDeleteMetadataConfirm}
            />
            <EditMetadataSheet
                metadata={metadataToEdit}
                isSaving={isUpdating}
                onCancel={() => setMetadataToEdit(null)}
                onSave={handleUpdateMetadata}
            />
        </>
    );
}
