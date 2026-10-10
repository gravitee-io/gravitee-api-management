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

import { Skeleton } from '@gravitee/graphene-core';
import { useEffect } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import { IntegrationDiscoveryPreview } from '../features/integrations/components/IntegrationDiscoveryPreview';
import { useIngestIntegrationApis } from '../features/integrations/hooks/useIngestIntegrationApis';
import { useIntegration } from '../features/integrations/hooks/useIntegration';
import { useIntegrationDiscovery } from '../features/integrations/hooks/useIntegrationDiscovery';
import type { IngestionScope } from '../features/integrations/types/integration';
import { ingestionErrorMessage } from '../features/integrations/utils/ingestionErrorMessage';
import { integrationErrorMessage } from '../features/integrations/utils/integrationErrorMessage';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../features/shared/hooks/useDetailBasePath';
import { notify } from '../shared/notify';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

const AGENT_DISCONNECTED_MESSAGE = 'Agent is DISCONNECTED, make sure your Agent is CONNECTED';
const INGESTION_IN_PROGRESS_MESSAGE =
    'API ingestion is in progress. The process should only take a few minute to complete. Come back shortly!';

export function IntegrationDiscoveryPreviewPage() {
    const { integrationId = '' } = useParams<{ integrationId: string }>();
    const overviewHref = useDetailBasePath('integrations', integrationId);
    const integrationsListHref = resolveListHrefFromDetailBasePath(overviewHref);
    const navigate = useNavigate();
    const ingest = useIngestIntegrationApis();

    const integrationQuery = useIntegration(integrationId);
    // A copy cached by an earlier overview visit may carry a stale agent status, so only a fetch from this visit decides.
    const freshIntegration = integrationQuery.isFetchedAfterMount ? integrationQuery.data : undefined;
    const agentStatus = freshIntegration?.agentStatus;
    const isAgentDisconnected = agentStatus === 'DISCONNECTED';
    const hasNoAgent = Boolean(freshIntegration) && !agentStatus;

    const discoveryQuery = useIntegrationDiscovery(integrationId, agentStatus === 'CONNECTED');

    useEffect(() => {
        if (isAgentDisconnected) notify.error(AGENT_DISCONNECTED_MESSAGE);
    }, [isAgentDisconnected]);

    useEffect(() => {
        if (hasNoAgent) console.warn(`Integration ${integrationId} has no agent to run discovery; redirecting to its overview`);
    }, [hasNoAgent, integrationId]);

    const discoveryError = discoveryQuery.isError ? discoveryQuery.error : undefined;
    useEffect(() => {
        if (!discoveryError) return;
        notify.error(integrationErrorMessage(discoveryError));
        console.warn(`Discovery of integration ${integrationId} failed; redirecting to its overview`, discoveryError);
    }, [discoveryError, integrationId]);

    const handleProceed = async (scope: IngestionScope) => {
        notify.success(INGESTION_IN_PROGRESS_MESSAGE);
        try {
            await ingest.mutateAsync({ integrationId, scope });
        } catch (error) {
            notify.error(ingestionErrorMessage(error));
            console.warn(`Ingestion of APIs for integration ${integrationId} failed; redirecting to its overview`, error);
        }
        navigate(overviewHref);
    };

    if (isForbiddenApiError(integrationQuery.isError, integrationQuery.error)) return <Navigate to={integrationsListHref} replace />;
    if (integrationQuery.isError || isAgentDisconnected || hasNoAgent || discoveryError) return <Navigate to={overviewHref} replace />;

    return (
        <div className="space-y-6" data-testid="integration-discovery-preview-page">
            <h1 className="text-2xl font-semibold tracking-tight">Discovery Preview</h1>
            {discoveryQuery.data ? (
                <IntegrationDiscoveryPreview preview={discoveryQuery.data} onProceed={handleProceed} isProceeding={ingest.isPending} />
            ) : (
                <div className="space-y-2">
                    <Skeleton className="h-8 w-64" />
                    <p className="text-sm text-muted-foreground">We&apos;re gathering your data</p>
                </div>
            )}
        </div>
    );
}
