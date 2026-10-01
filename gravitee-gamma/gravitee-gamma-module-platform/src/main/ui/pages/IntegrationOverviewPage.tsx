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
import { Navigate, useParams } from 'react-router-dom';

import { IntegrationAgentConnection } from '../features/integrations/components/IntegrationAgentConnection';
import { IntegrationId } from '../features/integrations/components/IntegrationId';
import { IntegrationIngestionInProgress } from '../features/integrations/components/IntegrationIngestionInProgress';
import { IntegrationProviderLabel } from '../features/integrations/components/IntegrationProviderLabel';
import { useIntegration } from '../features/integrations/hooks/useIntegration';
import { isIngestionInProgress } from '../features/integrations/utils/ingestion';
import { isA2aIntegration } from '../features/integrations/utils/integrationKind';
import { notify } from '../shared/notify';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

const LOAD_ERROR_MESSAGE = 'Integration could not be loaded. Please refresh and try again.';

export function IntegrationOverviewPage() {
    const { integrationId = '' } = useParams<{ integrationId: string }>();
    const { data: integration, isError, error } = useIntegration(integrationId);
    const isForbidden = isForbiddenApiError(isError, error);

    useEffect(() => {
        if (!isError || isForbidden) return;
        notify.error(error, LOAD_ERROR_MESSAGE);
    }, [error, isError, isForbidden]);

    useEffect(() => {
        if (!isForbidden) return;
        console.warn(
            `Integration ${integrationId} was denied (403) although its permissions grant definition read; redirecting to the Integrations list`,
            error,
        );
    }, [isForbidden, error, integrationId]);

    function renderContent() {
        if (isError) {
            return (
                <div className="flex items-center justify-center p-8">
                    <p className="text-sm text-muted-foreground">{LOAD_ERROR_MESSAGE}</p>
                </div>
            );
        }

        if (!integration) {
            return <Skeleton className="h-8 w-64" />;
        }

        return (
            <>
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">{integration.name}</h1>
                    <IntegrationProviderLabel provider={integration.provider} />
                </div>
                {!isA2aIntegration(integration) && (
                    <>
                        <IntegrationAgentConnection agentStatus={integration.agentStatus} />
                        <IntegrationId integrationId={integration.id} />
                    </>
                )}
                {isIngestionInProgress(integration) && <IntegrationIngestionInProgress />}
            </>
        );
    }

    if (isForbidden) return <Navigate to=".." replace />;

    return (
        <div className="space-y-6" data-testid="integration-overview-page">
            {renderContent()}
        </div>
    );
}
