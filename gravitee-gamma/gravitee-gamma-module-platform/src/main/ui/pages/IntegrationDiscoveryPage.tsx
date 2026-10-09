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
    Badge,
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Checkbox,
    Label,
    Skeleton,
} from '@gravitee/graphene-core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useIntegration } from '../features/integrations/hooks/useIntegration';
import { useIntegrationPreview } from '../features/integrations/hooks/useIntegrationPreview';
import { ingestIntegration } from '../features/integrations/services/integrationDetail';
import type { IntegrationPreviewApiState } from '../features/integrations/types/integration';
import { isIngestionInProgress } from '../features/integrations/utils/ingestion';
import { isA2aIntegration } from '../features/integrations/utils/integrationKind';
import { integrationKeys } from '../features/integrations/utils/queryKeys';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../features/shared/hooks/useDetailBasePath';
import { notify } from '../shared/notify';

const STATE_LABEL: Record<IntegrationPreviewApiState, string> = {
    NEW: 'New',
    UPDATE: 'Update',
};

export function IntegrationDiscoveryPage() {
    const { integrationId = '' } = useParams<{ integrationId: string }>();
    const env = useEnvironment();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const overviewHref = useDetailBasePath('integrations', integrationId);
    const integrationsListHref = resolveListHrefFromDetailBasePath(overviewHref);
    const { data: integration, isPending: integrationPending, isError: integrationError } = useIntegration(integrationId);
    const canPreview = Boolean(integration && !isA2aIntegration(integration) && integration.agentStatus === 'CONNECTED');
    const { data: preview, isPending: previewPending, isError: previewError } = useIntegrationPreview(integrationId, canPreview);
    const [includeNew, setIncludeNew] = useState(true);
    const [includeUpdate, setIncludeUpdate] = useState(true);
    const previewInitializedFor = useRef<string | null>(null);

    useEffect(() => {
        if (!preview || previewInitializedFor.current === integrationId) return;
        previewInitializedFor.current = integrationId;
        setIncludeNew(preview.newCount > 0);
        setIncludeUpdate(preview.updateCount > 0);
    }, [preview, integrationId]);

    const selectedApis = useMemo(() => {
        if (!preview) return [];
        return preview.apis.filter(api => {
            const isNewApi = api.state === 'NEW' && includeNew;
            const isUpdatedApi = api.state === 'UPDATE' && includeUpdate;
            return isNewApi || isUpdatedApi;
        });
    }, [preview, includeNew, includeUpdate]);

    const ingestMutation = useMutation({
        mutationFn: () => {
            const apiIds = preview?.isPartiallyDiscovered ? [] : selectedApis.map(api => api.id);
            return ingestIntegration(env!.id, integrationId, apiIds);
        },
        onSuccess: response => {
            if (response.status === 'ERROR') {
                notify.error(
                    new Error(response.message ?? 'Ingestion failed'),
                    'Ingestion failed. Please check your settings and try again.',
                );
                return;
            }
            notify.success(
                response.status === 'SUCCESS'
                    ? 'Ingestion complete! Your integration is now updated.'
                    : 'API ingestion is in progress. Come back shortly to see discovered APIs.',
            );
            void queryClient.invalidateQueries({ queryKey: integrationKeys.detail(env?.id ?? '', integrationId) });
            void queryClient.invalidateQueries({ queryKey: integrationKeys.federatedApis(env?.id ?? '', integrationId) });
            void queryClient.invalidateQueries({ queryKey: integrationKeys.preview(env?.id ?? '', integrationId) });
            navigate(overviewHref);
        },
        onError: error => {
            notify.error(error, 'Ingestion failed. Please check your settings and try again.');
        },
    });

    if (integrationPending) {
        return <Skeleton className="h-40 w-full" data-testid="integration-discovery-loading" />;
    }

    if (integrationError || !integration || isA2aIntegration(integration)) {
        return <Navigate to={integrationsListHref} replace />;
    }

    if (integration.agentStatus !== 'CONNECTED' || isIngestionInProgress(integration)) {
        return <Navigate to={overviewHref} replace />;
    }

    if (previewPending) {
        return <Skeleton className="h-40 w-full" data-testid="integration-discovery-loading" />;
    }

    if (previewError || !preview) {
        return (
            <div className="space-y-4" data-testid="integration-discovery-page">
                <p className="text-sm text-muted-foreground">Discovery preview could not be loaded. Please try again from the overview.</p>
                <Button asChild variant="outline">
                    <Link to={overviewHref}>Back to overview</Link>
                </Button>
            </div>
        );
    }

    const canProceed = preview.isPartiallyDiscovered || selectedApis.length > 0;

    return (
        <div className="space-y-6" data-testid="integration-discovery-page">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Discover</h1>
                <p className="text-sm text-muted-foreground">Review APIs from {integration.name} before importing them into Gravitee.</p>
            </div>

            {preview.isPartiallyDiscovered ? (
                <Alert variant="warning">
                    <AlertDescription>
                        Only a partial list of APIs was returned by the provider. Proceeding will ingest all discoverable APIs.
                    </AlertDescription>
                </Alert>
            ) : null}

            <Card>
                <CardHeader className="gap-4">
                    <CardTitle className="text-lg font-semibold">APIs to ingest</CardTitle>
                    <div className="flex flex-wrap gap-6">
                        <div className="flex items-center gap-2 text-sm">
                            <Checkbox
                                id="discover-include-new"
                                checked={includeNew}
                                disabled={preview.newCount <= 0}
                                onCheckedChange={checked => setIncludeNew(checked === true)}
                            />
                            <Label htmlFor="discover-include-new" className="flex items-center gap-2 font-normal">
                                New <Badge variant="secondary">{preview.newCount}</Badge>
                            </Label>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <Checkbox
                                id="discover-include-update"
                                checked={includeUpdate}
                                disabled={preview.updateCount <= 0}
                                onCheckedChange={checked => setIncludeUpdate(checked === true)}
                            />
                            <Label htmlFor="discover-include-update" className="flex items-center gap-2 font-normal">
                                Update <Badge variant="secondary">{preview.updateCount}</Badge>
                            </Label>
                        </div>
                    </div>
                </CardHeader>
                <CardContent className="space-y-3">
                    {selectedApis.length === 0 ? (
                        <p className="text-sm text-muted-foreground">Select at least one category to continue.</p>
                    ) : (
                        <ul className="divide-y rounded-lg border">
                            {selectedApis.map(api => (
                                <li key={api.id} className="flex items-center justify-between gap-3 px-4 py-3 text-sm">
                                    <span className="min-w-0 truncate font-medium">
                                        {api.name} ({api.version})
                                    </span>
                                    <Badge variant="outline">{STATE_LABEL[api.state]}</Badge>
                                </li>
                            ))}
                        </ul>
                    )}
                </CardContent>
            </Card>

            <div className="flex items-center justify-end gap-2">
                <Button asChild variant="outline" disabled={ingestMutation.isPending}>
                    <Link to={overviewHref}>Cancel</Link>
                </Button>
                <Button
                    type="button"
                    disabled={!canProceed || ingestMutation.isPending}
                    data-testid="discover-proceed-button"
                    onClick={() => ingestMutation.mutate()}
                >
                    {ingestMutation.isPending ? 'Ingesting…' : 'Ingest'}
                </Button>
            </div>
        </div>
    );
}
