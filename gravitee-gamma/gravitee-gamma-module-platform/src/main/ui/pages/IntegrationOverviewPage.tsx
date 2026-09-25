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
import { useParams } from 'react-router-dom';

import { IntegrationProviderLabel } from '../features/integrations/components/IntegrationProviderLabel';
import { useIntegration } from '../features/integrations/hooks/useIntegration';
import { notify } from '../shared/notify';

const LOAD_ERROR_MESSAGE = 'Integration could not be loaded. Please refresh and try again.';

export function IntegrationOverviewPage() {
    const { integrationId = '' } = useParams<{ integrationId: string }>();
    const { data: integration, isError, error } = useIntegration(integrationId);

    useEffect(() => {
        if (!isError) return;
        notify.error(error, LOAD_ERROR_MESSAGE);
    }, [error, isError]);

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
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">{integration.name}</h1>
                <IntegrationProviderLabel provider={integration.provider} />
            </div>
        );
    }

    return (
        <div className="space-y-6" data-testid="integration-overview-page">
            {renderContent()}
        </div>
    );
}
