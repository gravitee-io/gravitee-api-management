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
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { PathMappingsEmptyState } from './PathMappingsEmptyState';
import { PathMappingSheet } from './PathMappingSheet';
import { PathMappingsTable } from './PathMappingsTable';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { ConfirmDialog } from '../../../../../shared/components/ConfirmDialog';
import { useClientFilteredPagination } from '../../../../../shared/hooks/useClientFilteredPagination';
import { notify } from '../../../../../shared/notify';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { importApiPathMappings, updateApiPathMappings } from '../../../services/apis';
import type { ApiDetailDto, HttpListener } from '../../../types';
import { mergeApiDetailCache } from '../../../utils/apiDetailCache';
import { hasTcpListeners } from '../../../utils/apiHttpProxy';
import {
    PathMappingsConflictError,
    replacePathMapping,
    samePathMappings,
    sortedPathMappings,
    storedPathMappings,
} from '../../../utils/pathMappings';
import { apiDetailKeys } from '../../../utils/queryKeys';
import { KubernetesManagedReadOnlyAlert } from '../response-templates/KubernetesManagedReadOnlyAlert';
import { TcpProxyUnavailableNotice } from '../response-templates/TcpProxyUnavailableNotice';

function firstHttpListener(api: ApiDetailDto | undefined): HttpListener | undefined {
    return api?.listeners?.find((listener): listener is HttpListener => listener.type === 'HTTP');
}

export function ApiPathMappingsPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const { permissionsReady } = useApiDetailContext();

    const canEdit = useHasPermission({ anyOf: ['api-definition-u'] });
    const { data: api, isLoading, isError } = useApiDetail(apiId);

    const isKubernetesManaged = api?.definitionContext?.origin === 'KUBERNETES';
    const readOnly = isKubernetesManaged || !canEdit;
    const isTcp = hasTcpListeners(api);
    const httpListener = useMemo(() => firstHttpListener(api), [api]);

    const allPaths = useMemo(() => sortedPathMappings(httpListener?.pathMappings), [httpListener]);
    const { page, pageSize, totalCount, pageData, setPage, handlePageSizeChange } = useClientFilteredPagination(allPaths);

    const [addOpen, setAddOpen] = useState(false);
    const [editingPath, setEditingPath] = useState<string | null>(null);
    const [deletingPath, setDeletingPath] = useState<string | null>(null);

    const isFirstUse = !isLoading && !isError && !!httpListener && allPaths.length === 0;
    const showAddButton = canEdit && !readOnly && !isLoading && !isError && !!httpListener && allPaths.length > 0;

    const mutation = useMutation({
        mutationFn: (updater: (current: string[]) => string[]) => updateApiPathMappings(env!.id, apiId!, updater),
        onSuccess: (updatedApi: ApiDetailDto) => {
            queryClient.setQueryData(apiDetailKeys.detail(env?.id ?? '', apiId ?? ''), (prev: ApiDetailDto | undefined) =>
                mergeApiDetailCache(prev, updatedApi),
            );
        },
    });

    const importMutation = useMutation({
        mutationFn: ({ pageId, overwrite }: { pageId: string; overwrite: boolean }) =>
            importApiPathMappings(env!.id, apiId!, { pageId, overwrite }),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
        },
    });

    const isMutating = mutation.isPending || importMutation.isPending;

    const saveMappings = useCallback(
        async (change: (mappings: string[]) => string[], successMessage: string, targetPath?: string) => {
            if (!env || !apiId) return;
            try {
                await mutation.mutateAsync(current => {
                    if (targetPath !== undefined && !current.includes(targetPath)) {
                        throw new PathMappingsConflictError('missing-mapping', 'The path mapping no longer exists.');
                    }
                    const next = change(current);
                    if (samePathMappings(current, next)) {
                        return current;
                    }
                    return next;
                });
                notify.success(successMessage);
                setAddOpen(false);
                setEditingPath(null);
                setDeletingPath(null);
            } catch (error) {
                if (error instanceof PathMappingsConflictError) {
                    notify.error(error, error.message);
                    await queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env.id, apiId) });
                    setEditingPath(null);
                    setDeletingPath(null);
                    return;
                }
                if (error instanceof ApimApiError && error.status === 412) {
                    await queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env.id, apiId) });
                }
                notify.error(error, 'Failed to update path mappings');
            }
        },
        [apiId, env, mutation, queryClient],
    );

    const handleAdd = useCallback(
        (path: string) => {
            void saveMappings(mappings => [...mappings, path], 'The path mapping has been successfully added!');
        },
        [saveMappings],
    );

    const handleImport = useCallback(
        async (pageId: string, overwrite: boolean) => {
            if (!env || !apiId) return;
            try {
                await importMutation.mutateAsync({ pageId, overwrite });
                notify.success('The path mappings have been successfully imported!');
                setAddOpen(false);
            } catch (error) {
                notify.error(error, 'Failed to import path mappings');
            }
        },
        [apiId, env, importMutation],
    );

    const handleEdit = useCallback(
        (path: string) => {
            if (!editingPath) return;
            void saveMappings(
                mappings => replacePathMapping(mappings, editingPath, path),
                'The path mapping has been successfully updated!',
                editingPath,
            );
        },
        [editingPath, saveMappings],
    );

    const handleDeleteConfirm = useCallback(() => {
        if (!deletingPath) return;
        void saveMappings(
            mappings => mappings.filter(mapping => mapping !== deletingPath),
            `The path mapping ${deletingPath} has been successfully deleted!`,
            deletingPath,
        );
    }, [deletingPath, saveMappings]);

    if (!env || !apiId) {
        return null;
    }

    if (!permissionsReady) {
        return null;
    }

    if (isTcp) {
        return (
            <TcpProxyUnavailableNotice
                feature="Path Mappings"
                detail="TCP Proxy APIs forward raw traffic and do not support HTTP path mapping analytics."
            />
        );
    }

    return (
        <div className="space-y-4">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-lg font-semibold">Path mappings</h1>
                    <p className="text-sm text-muted-foreground">
                        Declare the paths analytics should group. The list applies the next time you deploy this API.
                    </p>
                </div>
                {showAddButton ? (
                    <Button size="sm" className="shrink-0" onClick={() => setAddOpen(true)} data-testid="add-path-mapping">
                        <PlusIcon className="size-4" aria-hidden />
                        Add path mapping
                    </Button>
                ) : null}
            </div>

            {isKubernetesManaged ? <KubernetesManagedReadOnlyAlert resource="Path mappings" /> : null}

            {isError ? (
                <Alert variant="destructive">
                    <AlertDescription>Failed to load path mappings. Please refresh the page.</AlertDescription>
                </Alert>
            ) : !isLoading && !httpListener ? (
                <Alert variant="destructive">
                    <AlertDescription>This API has no HTTP listener to store path mappings.</AlertDescription>
                </Alert>
            ) : isFirstUse ? (
                <PathMappingsEmptyState
                    action={
                        canEdit && !readOnly ? (
                            <Button type="button" onClick={() => setAddOpen(true)} data-testid="add-path-mapping">
                                <PlusIcon className="size-4" aria-hidden />
                                Add path mapping
                            </Button>
                        ) : undefined
                    }
                />
            ) : (
                <PathMappingsTable
                    paths={pageData}
                    page={page}
                    pageSize={pageSize}
                    totalCount={totalCount}
                    readOnly={readOnly}
                    isMutating={isMutating}
                    isLoading={isLoading}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                    onEdit={setEditingPath}
                    onDelete={setDeletingPath}
                />
            )}

            <PathMappingSheet
                open={addOpen}
                mode="add"
                existing={storedPathMappings(httpListener?.pathMappings)}
                isPending={isMutating}
                environmentId={env.id}
                apiId={apiId}
                onOpenChange={setAddOpen}
                onSubmit={handleAdd}
                onImport={handleImport}
            />

            <PathMappingSheet
                open={editingPath !== null}
                mode="edit"
                existing={storedPathMappings(httpListener?.pathMappings)}
                initialPath={editingPath ?? ''}
                isPending={isMutating}
                onOpenChange={open => {
                    if (!open) setEditingPath(null);
                }}
                onSubmit={handleEdit}
            />

            <ConfirmDialog
                open={deletingPath !== null}
                onOpenChange={open => {
                    if (!open) setDeletingPath(null);
                }}
                title="Delete path mapping"
                description={deletingPath ? `Are you sure you want to delete the path mapping ${deletingPath}?` : undefined}
                confirmLabel="Delete"
                destructive
                isPending={isMutating}
                onConfirm={handleDeleteConfirm}
            />
        </div>
    );
}
