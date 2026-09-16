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
import { Alert, AlertDescription, Button, DataTableEmptyState } from '@gravitee/graphene-core';
import { PlusIcon, ScrollTextIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ApiResponseTemplatesTable } from './ApiResponseTemplatesTable';
import { FeatureUnavailableNotice } from './FeatureUnavailableNotice';
import { KubernetesManagedReadOnlyAlert } from './KubernetesManagedReadOnlyAlert';
import { TcpProxyUnavailableNotice } from './TcpProxyUnavailableNotice';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { ConfirmDialog } from '../../../../../shared/components/ConfirmDialog';
import { useClientFilteredPagination } from '../../../../../shared/hooks/useClientFilteredPagination';
import { notify } from '../../../../../shared/notify';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiResponseTemplates } from '../../../services/apis';
import type { ResponseTemplateRow } from '../../../types/responseTemplate';
import { hasTcpListeners, supportsResponseTemplates } from '../../../utils/apiHttpProxy';
import { apiDetailKeys } from '../../../utils/queryKeys';
import {
    matchesResponseTemplateSearch,
    removeResponseTemplate,
    toResponseTemplatePath,
    toResponseTemplates,
} from '../../../utils/responseTemplates';

export function ApiResponseTemplatesPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const { permissionsReady } = useApiDetailContext();

    const canRead = useHasPermission({ anyOf: ['api-response_templates-r'] });
    const canCreate = useHasPermission({ anyOf: ['api-response_templates-c'] });
    const canEdit = useHasPermission({ anyOf: ['api-response_templates-u'] });
    const canDelete = useHasPermission({ anyOf: ['api-response_templates-d'] });

    const { data: api, isLoading, isError } = useApiDetail(apiId);

    const isKubernetesManaged = api?.definitionContext?.origin === 'KUBERNETES';
    const readOnly = isKubernetesManaged || !canEdit;
    const isTcp = hasTcpListeners(api);

    const templates = useMemo(() => toResponseTemplates(api?.responseTemplates), [api?.responseTemplates]);
    const { search, page, pageSize, totalCount, pageData, handleSearchChange, setPage, handlePageSizeChange } = useClientFilteredPagination(
        templates,
        matchesResponseTemplateSearch,
    );

    const [deleting, setDeleting] = useState<ResponseTemplateRow | null>(null);

    const isFirstUse = !isLoading && !isError && templates.length === 0 && search.trim() === '';
    const showAddButton = canCreate && !readOnly && !isFirstUse;

    const mutation = useMutation({
        mutationFn: (row: ResponseTemplateRow) =>
            updateApiResponseTemplates(env!.id, apiId!, current => removeResponseTemplate(current, row)),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
            setDeleting(null);
        },
    });

    const handleEdit = useCallback(
        (row: ResponseTemplateRow) => {
            navigate(toResponseTemplatePath(row.key, row.contentType));
        },
        [navigate],
    );

    const handleDeleteConfirm = useCallback(async () => {
        if (!deleting) return;
        try {
            await mutation.mutateAsync(deleting);
            notify.success(`Response Template ${deleting.key} - ${deleting.contentType} successfully deleted!`);
        } catch (error) {
            if (error instanceof ApimApiError && error.status === 412) {
                queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
            }
            notify.error(error, 'Failed to delete response template');
        }
    }, [apiId, deleting, env?.id, mutation, queryClient]);

    if (!env || !apiId) {
        return null;
    }

    if (!permissionsReady) {
        return null;
    }

    if (isTcp) {
        return <TcpProxyUnavailableNotice feature="Response Templates" />;
    }

    if (!supportsResponseTemplates(api)) {
        return (
            <FeatureUnavailableNotice
                heading="Response Templates are not available for MCP and LLM Proxy APIs"
                detail="MCP and LLM Proxy APIs do not support HTTP response template overrides."
            />
        );
    }

    if (!canRead) {
        return (
            <div className="space-y-6">
                <h1 className="text-2xl font-semibold tracking-tight">Response Templates</h1>
                <p className="text-sm text-muted-foreground">You don&apos;t have permission to view response templates.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Response Templates</h1>
                    <p className="text-sm text-muted-foreground">
                        Define your own response templates if you&apos;re looking for overriding default responses from the gateway.
                    </p>
                </div>
                {showAddButton ? (
                    <Button className="shrink-0" onClick={() => navigate('new')}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add new Response Template
                    </Button>
                ) : null}
            </div>

            {isKubernetesManaged ? <KubernetesManagedReadOnlyAlert resource="Response templates" /> : null}

            {isError ? (
                <Alert variant="destructive">
                    <AlertDescription>Failed to load response templates. Please refresh the page.</AlertDescription>
                </Alert>
            ) : isFirstUse ? (
                <div className="rounded-lg border">
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<ScrollTextIcon className="size-8" aria-hidden />}
                        title="No Response Templates"
                        description="Override the gateway’s default error payloads by matching a template key and Accept header, then returning a custom status, headers, and body."
                        primaryAction={
                            canCreate && !readOnly ? (
                                <Button type="button" onClick={() => navigate('new')}>
                                    <PlusIcon className="size-4" aria-hidden />
                                    Add new Response Template
                                </Button>
                            ) : undefined
                        }
                    />
                </div>
            ) : (
                <ApiResponseTemplatesTable
                    templates={pageData}
                    totalCount={totalCount}
                    page={page}
                    pageSize={pageSize}
                    search={search}
                    isLoading={isLoading}
                    canEdit={canEdit}
                    canDelete={canDelete}
                    readOnly={readOnly}
                    isMutating={mutation.isPending}
                    onSearchChange={handleSearchChange}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                    onEdit={handleEdit}
                    onDelete={setDeleting}
                    onClearSearch={() => handleSearchChange('')}
                />
            )}

            <ConfirmDialog
                open={deleting !== null}
                onOpenChange={open => !open && setDeleting(null)}
                title="Delete a Response Template"
                description={
                    <>
                        Are you sure you want to delete the Response Template{' '}
                        <strong>
                            {deleting?.key} - {deleting?.contentType}
                        </strong>
                        ?
                    </>
                }
                confirmLabel="Delete"
                pendingLabel="Deleting…"
                destructive
                isPending={mutation.isPending}
                onConfirm={handleDeleteConfirm}
            />
        </div>
    );
}
