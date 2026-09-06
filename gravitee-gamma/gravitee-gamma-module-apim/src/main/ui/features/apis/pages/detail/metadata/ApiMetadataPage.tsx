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

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Alert, AlertDescription, Button } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { ApiMetadataDeleteDialog } from './ApiMetadataDeleteDialog';
import { ApiMetadataSheet } from './ApiMetadataSheet';
import { ApiMetadataTable } from './ApiMetadataTable';
import { notify } from '../../../../../shared/notify';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiMetadata, useCreateApiMetadata, useDeleteApiMetadata, useUpdateApiMetadata } from '../../../hooks/useApiMetadata';
import type { ApiMetadata, MetadataSource, NewApiMetadataPayload, UpdateApiMetadataPayload } from '../../../types/metadata';
import { DEFAULT_METADATA_PAGE_SIZE, toMetadataSortBy } from '../../../utils/apiMetadata';

type SheetState =
    | { type: 'closed' }
    | { type: 'create' }
    | { type: 'edit'; metadata: ApiMetadata }
    | { type: 'delete'; metadata: ApiMetadata };
type TableSortingState = { id: string; desc: boolean }[];

export function ApiMetadataPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const { api, permissionsReady } = useApiDetailContext();

    const canRead = useHasPermission({ anyOf: ['api-metadata-r'] });
    const canCreate = useHasPermission({ anyOf: ['api-metadata-c'] });
    const canEdit = useHasPermission({ anyOf: ['api-metadata-u'] });
    const canDelete = useHasPermission({ anyOf: ['api-metadata-d'] });

    const isKubernetesManaged = api?.definitionContext?.origin === 'KUBERNETES';
    const readOnly = isKubernetesManaged;

    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_METADATA_PAGE_SIZE);
    const [sorting, setSorting] = useState<TableSortingState>([]);
    const [source, setSource] = useState<MetadataSource | undefined>(undefined);
    const [sheet, setSheet] = useState<SheetState>({ type: 'closed' });

    const searchParams = useMemo(
        () => ({
            page,
            perPage: pageSize,
            source,
            sortBy: toMetadataSortBy(sorting),
        }),
        [page, pageSize, source, sorting],
    );

    const { data, isLoading, isError } = useApiMetadata(apiId, searchParams, permissionsReady && canRead);
    const createMutation = useCreateApiMetadata(apiId ?? '');
    const updateMutation = useUpdateApiMetadata(apiId ?? '');
    const deleteMutation = useDeleteApiMetadata(apiId ?? '');

    const metadata = data?.data ?? [];
    const totalCount = data?.pagination?.totalCount ?? metadata.length;
    const isFirstUse = !isLoading && !isError && totalCount === 0 && source === undefined;
    const showAddButton = canCreate && !readOnly && !isFirstUse;

    function closeSheet() {
        setSheet({ type: 'closed' });
    }

    function handleSourceChange(next: MetadataSource | undefined) {
        setSource(next);
        setPage(1);
    }

    function handleResetFilters() {
        setSource(undefined);
        setSorting([]);
        setPage(1);
    }

    function handleSortingChange(updater: TableSortingState | ((prev: TableSortingState) => TableSortingState)) {
        setSorting(prev => (typeof updater === 'function' ? updater(prev) : updater));
        setPage(1);
    }

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    async function handleCreate(payload: NewApiMetadataPayload | UpdateApiMetadataPayload) {
        try {
            await createMutation.mutateAsync(payload as NewApiMetadataPayload);
            notify.success('Metadata created successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to create metadata');
        }
    }

    async function handleUpdate(payload: NewApiMetadataPayload | UpdateApiMetadataPayload) {
        try {
            await updateMutation.mutateAsync(payload as UpdateApiMetadataPayload);
            notify.success(`'${payload.name}' updated successfully`);
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to update metadata');
        }
    }

    async function handleDelete() {
        if (sheet.type !== 'delete') return;
        try {
            await deleteMutation.mutateAsync(sheet.metadata.key);
            notify.success(`'${sheet.metadata.name}' deleted successfully`);
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to delete metadata');
        }
    }

    if (!env || !apiId) {
        return null;
    }

    if (!permissionsReady) {
        return null;
    }

    if (!canRead) {
        return (
            <div className="space-y-6">
                <h1 className="text-2xl font-semibold tracking-tight">API metadata</h1>
                <p className="text-sm text-muted-foreground">You don&apos;t have permission to view API metadata.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">API metadata</h1>
                    <p className="text-sm text-muted-foreground">
                        Set metadata information on the API that can be easily accessed through Markdown templating.
                    </p>
                </div>
                {showAddButton ? (
                    <Button className="shrink-0" onClick={() => setSheet({ type: 'create' })}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add API Metadata
                    </Button>
                ) : null}
            </div>

            {isKubernetesManaged ? (
                <Alert>
                    <AlertDescription>This API is managed by the Kubernetes operator. Metadata is read-only.</AlertDescription>
                </Alert>
            ) : null}

            {isError ? (
                <Alert variant="destructive">
                    <AlertDescription>Failed to load metadata. Please refresh the page.</AlertDescription>
                </Alert>
            ) : (
                <ApiMetadataTable
                    metadata={metadata}
                    totalCount={totalCount}
                    page={page}
                    pageSize={pageSize}
                    sorting={sorting}
                    source={source}
                    isLoading={isLoading}
                    canCreate={canCreate}
                    canEdit={canEdit}
                    canDelete={canDelete}
                    readOnly={readOnly}
                    isMutating={createMutation.isPending || updateMutation.isPending || deleteMutation.isPending}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                    onSortingChange={handleSortingChange}
                    onSourceChange={handleSourceChange}
                    onResetFilters={handleResetFilters}
                    onCreate={() => setSheet({ type: 'create' })}
                    onEdit={m => setSheet({ type: 'edit', metadata: m })}
                    onDelete={m => setSheet({ type: 'delete', metadata: m })}
                />
            )}

            <ApiMetadataSheet
                open={sheet.type === 'create' || sheet.type === 'edit'}
                mode={sheet.type === 'edit' ? 'edit' : 'create'}
                metadata={sheet.type === 'edit' ? sheet.metadata : undefined}
                readOnly={readOnly}
                onClose={closeSheet}
                onSubmit={sheet.type === 'edit' ? handleUpdate : handleCreate}
                isSaving={createMutation.isPending || updateMutation.isPending}
            />

            <ApiMetadataDeleteDialog
                open={sheet.type === 'delete'}
                metadata={sheet.type === 'delete' ? sheet.metadata : undefined}
                onClose={closeSheet}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />
        </div>
    );
}
