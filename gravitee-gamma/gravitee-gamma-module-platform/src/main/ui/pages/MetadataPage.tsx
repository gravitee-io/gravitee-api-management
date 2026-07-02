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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Button, Skeleton } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { MetadataDeleteSheet } from '../features/metadata/components/MetadataDeleteSheet';
import { MetadataSheet } from '../features/metadata/components/MetadataSheet';
import { MetadataTable } from '../features/metadata/components/MetadataTable';
import { useEnvironmentMetadata } from '../features/metadata/hooks/useEnvironmentMetadata';
import { useCreateMetadata, useDeleteMetadata, useUpdateMetadata } from '../features/metadata/hooks/useMetadataMutations';
import type { Metadata, NewMetadataPayload, UpdateMetadataPayload } from '../features/metadata/types/metadata';
import { notify } from '../shared/notify';

type SheetState = { type: 'closed' } | { type: 'create' } | { type: 'edit'; metadata: Metadata } | { type: 'delete'; metadata: Metadata };

export function MetadataPage() {
    const canCreate = useHasPermission({ anyOf: ['environment-metadata-c'] });
    const canEdit = useHasPermission({ anyOf: ['environment-metadata-u'] });
    const canDelete = useHasPermission({ anyOf: ['environment-metadata-d'] });

    const { data: metadata = [], isLoading, isError } = useEnvironmentMetadata();
    const createMutation = useCreateMetadata();
    const updateMutation = useUpdateMetadata();
    const deleteMutation = useDeleteMetadata();

    const [sheet, setSheet] = useState<SheetState>({ type: 'closed' });

    function closeSheet() {
        setSheet({ type: 'closed' });
    }

    async function handleCreate(data: NewMetadataPayload | UpdateMetadataPayload) {
        try {
            await createMutation.mutateAsync(data as NewMetadataPayload);
            notify.success('Metadata created successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to create metadata');
        }
    }

    async function handleUpdate(data: NewMetadataPayload | UpdateMetadataPayload) {
        try {
            await updateMutation.mutateAsync(data as UpdateMetadataPayload);
            notify.success('Metadata updated successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to update metadata');
        }
    }

    async function handleDelete() {
        if (sheet.type !== 'delete') return;
        try {
            await deleteMutation.mutateAsync(sheet.metadata.key);
            notify.success('Metadata deleted successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to delete metadata');
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Metadata</h1>
                    <p className="text-sm text-muted-foreground">
                        Create global metadata to retrieve custom information about your APIs across the environment.
                    </p>
                </div>
                {canCreate && (
                    <Button className="shrink-0" onClick={() => setSheet({ type: 'create' })}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add Global Metadata
                    </Button>
                )}
            </div>

            {isLoading ? (
                <div className="space-y-2">
                    {Array.from({ length: 4 }).map((_, i) => (
                        <Skeleton key={i} className="h-12 w-full rounded-md" />
                    ))}
                </div>
            ) : isError ? (
                <div className="flex items-center justify-center p-8">
                    <p className="text-sm text-muted-foreground">Failed to load metadata. Please refresh and try again.</p>
                </div>
            ) : (
                <MetadataTable
                    metadata={metadata}
                    canEdit={canEdit}
                    canDelete={canDelete}
                    onEdit={m => setSheet({ type: 'edit', metadata: m })}
                    onDelete={m => setSheet({ type: 'delete', metadata: m })}
                />
            )}

            <MetadataSheet
                open={sheet.type === 'create' || sheet.type === 'edit'}
                mode={sheet.type === 'edit' ? 'edit' : 'create'}
                metadata={sheet.type === 'edit' ? sheet.metadata : undefined}
                onClose={closeSheet}
                onSubmit={sheet.type === 'edit' ? handleUpdate : handleCreate}
                isSaving={createMutation.isPending || updateMutation.isPending}
            />

            <MetadataDeleteSheet
                open={sheet.type === 'delete'}
                metadata={sheet.type === 'delete' ? sheet.metadata : undefined}
                onClose={closeSheet}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />
        </div>
    );
}
