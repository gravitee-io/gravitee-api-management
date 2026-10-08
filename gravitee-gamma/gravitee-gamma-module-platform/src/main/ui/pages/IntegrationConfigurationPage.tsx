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
import { Navigate, useMatch, useNavigate, useParams } from 'react-router-dom';

import { IntegrationDangerZone } from '../features/integrations/components/IntegrationDangerZone';
import { IntegrationDirectMembers } from '../features/integrations/components/IntegrationDirectMembers';
import { IntegrationGeneralInformationForm } from '../features/integrations/components/IntegrationGeneralInformationForm';
import { IntegrationGroupInheritedMembers } from '../features/integrations/components/IntegrationGroupInheritedMembers';
import { useIntegration } from '../features/integrations/hooks/useIntegration';
import { INTEGRATION_LOAD_ERROR_MESSAGE, useIntegrationLoadFailure } from '../features/integrations/hooks/useIntegrationLoadFailure';
import { useIntegrationPermissions } from '../features/integrations/hooks/useIntegrationPermissions';
import { isA2aIntegration } from '../features/integrations/utils/integrationKind';
import {
    INTEGRATION_DEFINITION_DELETE_PERMISSION,
    INTEGRATION_DEFINITION_UPDATE_PERMISSION,
    INTEGRATION_MEMBER_READ_PERMISSION,
} from '../features/integrations/utils/integrationPermissions';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../features/shared/hooks/useDetailBasePath';

function IntegrationLoadError() {
    return (
        <div className="flex items-center justify-center p-8">
            <p className="text-sm text-muted-foreground">{INTEGRATION_LOAD_ERROR_MESSAGE}</p>
        </div>
    );
}

export function IntegrationConfigurationPage() {
    const { integrationId = '' } = useParams<{ integrationId: string }>();
    const basePath = useDetailBasePath('integrations', integrationId);
    const integrationsListHref = resolveListHrefFromDetailBasePath(basePath);
    const configurationHref = `${basePath}/configuration`;
    const membersHref = `${configurationHref}/members`;
    const isMembersTabActive = useMatch(membersHref) !== null;
    const navigate = useNavigate();
    const { data: integration, isError, error } = useIntegration(integrationId);
    const isForbidden = useIntegrationLoadFailure(integrationId, isError, error);
    const { data: permissions } = useIntegrationPermissions(integrationId);
    const canUpdate = Boolean(permissions?.includes(INTEGRATION_DEFINITION_UPDATE_PERMISSION));
    const canDelete = Boolean(permissions?.includes(INTEGRATION_DEFINITION_DELETE_PERMISSION));
    const canReadMembers = Boolean(permissions?.includes(INTEGRATION_MEMBER_READ_PERMISSION));
    const canSeeGeneral = !permissions || canUpdate || canDelete;

    function renderGeneralContent() {
        if (isError) return <IntegrationLoadError />;

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

    function renderGroupInheritedMembers() {
        if (isError && !integration) return <IntegrationLoadError />;
        if (!integration) return <Skeleton className="h-24 rounded-lg" />;
        return <IntegrationGroupInheritedMembers integrationId={integrationId} groupIds={integration.groups ?? []} />;
    }

    const shouldRedirect = isForbidden || (integration && isA2aIntegration(integration));

    if (shouldRedirect) return <Navigate to={integrationsListHref} replace />;
    if (permissions && isMembersTabActive && !canReadMembers) return <Navigate to={configurationHref} replace />;
    if (!isMembersTabActive && !canSeeGeneral && canReadMembers) return <Navigate to={membersHref} replace />;

    return (
        <Tabs
            value={isMembersTabActive ? 'members' : 'general'}
            onValueChange={value => navigate(value === 'members' ? membersHref : configurationHref)}
            data-testid="integration-configuration-page"
        >
            <TabsList variant="line">
                {canSeeGeneral ? <TabsTrigger value="general">General</TabsTrigger> : null}
                {canReadMembers ? <TabsTrigger value="members">User Permissions</TabsTrigger> : null}
            </TabsList>
            {canSeeGeneral ? (
                <TabsContent value="general">
                    <div className="space-y-6">{renderGeneralContent()}</div>
                </TabsContent>
            ) : null}
            {canReadMembers ? (
                <TabsContent value="members">
                    <div className="space-y-6">
                        <IntegrationDirectMembers integrationId={integrationId} />
                        {renderGroupInheritedMembers()}
                    </div>
                </TabsContent>
            ) : null}
        </Tabs>
    );
}
