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

import { Skeleton, Tabs, TabsContent, TabsList, TabsTrigger } from '@gravitee/graphene-core';
import { Navigate, useParams } from 'react-router-dom';

import { IntegrationDangerZone } from '../features/integrations/components/IntegrationDangerZone';
import { IntegrationGeneralInformationForm } from '../features/integrations/components/IntegrationGeneralInformationForm';
import { useIntegration } from '../features/integrations/hooks/useIntegration';
import { INTEGRATION_LOAD_ERROR_MESSAGE, useIntegrationLoadFailure } from '../features/integrations/hooks/useIntegrationLoadFailure';
import { useIntegrationPermissions } from '../features/integrations/hooks/useIntegrationPermissions';
import { isA2aIntegration } from '../features/integrations/utils/integrationKind';
import {
    INTEGRATION_DEFINITION_DELETE_PERMISSION,
    INTEGRATION_DEFINITION_UPDATE_PERMISSION,
} from '../features/integrations/utils/integrationPermissions';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../features/shared/hooks/useDetailBasePath';

export function IntegrationConfigurationPage() {
    const { integrationId = '' } = useParams<{ integrationId: string }>();
    const integrationsListHref = resolveListHrefFromDetailBasePath(useDetailBasePath('integrations', integrationId));
    const { data: integration, isError, error } = useIntegration(integrationId);
    const isForbidden = useIntegrationLoadFailure(integrationId, isError, error);
    const { data: permissions } = useIntegrationPermissions(integrationId);
    const canUpdate = Boolean(permissions?.includes(INTEGRATION_DEFINITION_UPDATE_PERMISSION));
    const canDelete = Boolean(permissions?.includes(INTEGRATION_DEFINITION_DELETE_PERMISSION));

    function renderGeneralContent() {
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
                {canUpdate ? <IntegrationGeneralInformationForm key={integration.id} integration={integration} /> : null}
                {canDelete ? <IntegrationDangerZone integrationId={integrationId} /> : null}
            </>
        );
    }

    const shouldRedirect = isForbidden || (integration && isA2aIntegration(integration));

    if (shouldRedirect) return <Navigate to={integrationsListHref} replace />;

    return (
        <Tabs defaultValue="general" data-testid="integration-configuration-page">
            <TabsList variant="line">
                <TabsTrigger value="general">General</TabsTrigger>
            </TabsList>
            <TabsContent value="general">
                <div className="space-y-6">{renderGeneralContent()}</div>
            </TabsContent>
        </Tabs>
    );
}
