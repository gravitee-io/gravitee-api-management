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
import { useParams } from 'react-router-dom';

import { ApiOverviewChecklist } from './overview/ApiOverviewChecklist';
import { ApiOverviewGatewayCards } from './overview/ApiOverviewGatewayCards';
import { ApiOverviewTrafficCards } from './overview/ApiOverviewTrafficCards';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import { useApiOverviewData } from '../../hooks/useApiOverviewData';

export function ApiDetailOverviewPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const { api } = useApiDetailContext();

    const {
        membersData,
        alertsData,
        exposedEntrypoints,
        analyticsStats,
        isLoadingTraffic,
        isLoadingMembers,
        isLoadingAlerts,
        isLoadingEntrypoints,
    } = useApiOverviewData(apiId);

    const gatewayUrl = exposedEntrypoints?.[0]?.value;
    const upstreamUrl = api?.endpointGroups?.[0]?.endpoints?.[0]?.configuration?.target;

    return (
        <div className="space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Overview</h1>
                <p className="text-sm text-muted-foreground">
                    Setup checklist, gateway endpoints, and traffic snapshot for {api?.name ?? '…'}.
                </p>
            </div>

            <ApiOverviewChecklist
                api={api}
                membersData={membersData}
                alertsData={alertsData}
                isLoadingMembers={isLoadingMembers}
                isLoadingAlerts={isLoadingAlerts}
            />

            <ApiOverviewGatewayCards gatewayUrl={gatewayUrl} upstreamUrl={upstreamUrl} isLoadingEntrypoints={isLoadingEntrypoints} />

            <ApiOverviewTrafficCards analyticsStats={analyticsStats} isLoadingTraffic={isLoadingTraffic} />
        </div>
    );
}
