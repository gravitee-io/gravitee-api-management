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

import { Alert, AlertDescription, Card, CardContent, CardHeader, Skeleton } from '@gravitee/graphene-core';
import { Navigate, useParams } from 'react-router-dom';

import { IntegrationAgentConnection } from '../features/integrations/components/IntegrationAgentConnection';
import { IntegrationFederatedApisSection } from '../features/integrations/components/IntegrationFederatedApisSection';
import { IntegrationId } from '../features/integrations/components/IntegrationId';
import { IntegrationProviderField } from '../features/integrations/components/IntegrationProviderField';
import { useIntegration } from '../features/integrations/hooks/useIntegration';
import { INTEGRATION_LOAD_ERROR_MESSAGE, useIntegrationLoadFailure } from '../features/integrations/hooks/useIntegrationLoadFailure';
import { isA2aIntegration } from '../features/integrations/utils/integrationKind';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../features/shared/hooks/useDetailBasePath';

const DISCONNECTED_GUIDANCE = 'Check your agent status and ensure connectivity with the provider to start importing your APIs in Gravitee.';

export function IntegrationOverviewPage() {
    const { integrationId = '' } = useParams<{ integrationId: string }>();
    const integrationsListHref = resolveListHrefFromDetailBasePath(useDetailBasePath('integrations', integrationId));
    const { data: integration, isError, error } = useIntegration(integrationId);
    const isForbidden = useIntegrationLoadFailure(integrationId, isError, error);

    function renderContent() {
        if (isError) {
            return (
                <div className="flex items-center justify-center p-8">
                    <p className="text-sm text-muted-foreground">{INTEGRATION_LOAD_ERROR_MESSAGE}</p>
                </div>
            );
        }

        if (!integration) {
            return <Skeleton className="h-8 w-64" />;
        }

        return (
            <>
                <Card data-testid="integration-overview-summary">
                    <CardHeader className="gap-1">
                        <h1 className="text-xl font-semibold tracking-tight">{integration.name}</h1>
                        {integration.description ? <p className="text-sm text-muted-foreground">{integration.description}</p> : null}
                    </CardHeader>
                    <CardContent className="space-y-5">
                        <div className="flex flex-col gap-5 rounded-lg border p-5 sm:flex-row sm:gap-0">
                            <div className="min-w-0 flex-1 sm:border-r sm:pr-5">
                                <IntegrationProviderField provider={integration.provider} />
                            </div>
                            <div className="min-w-0 flex-1 sm:border-r sm:px-5">
                                <IntegrationAgentConnection agentStatus={integration.agentStatus} />
                            </div>
                            <div className="min-w-0 flex-1 sm:pl-5">
                                <IntegrationId integrationId={integration.id} />
                            </div>
                        </div>
                        {integration.agentStatus === 'DISCONNECTED' && (
                            <Alert variant="destructive" data-testid="integration-agent-disconnected-banner">
                                <AlertDescription>{DISCONNECTED_GUIDANCE}</AlertDescription>
                            </Alert>
                        )}
                    </CardContent>
                </Card>
                <IntegrationFederatedApisSection integration={integration} />
            </>
        );
    }

    if (isForbidden || (integration && isA2aIntegration(integration))) return <Navigate to={integrationsListHref} replace />;

    return (
        <div className="space-y-6" data-testid="integration-overview-page">
            {renderContent()}
        </div>
    );
}
