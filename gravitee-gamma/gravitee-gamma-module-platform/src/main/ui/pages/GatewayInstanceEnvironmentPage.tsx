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
import { useMemo } from 'react';
import { useParams } from 'react-router-dom';

import { GatewayInstanceInformationTable } from '../features/gateway-instances/components/GatewayInstanceInformationTable';
import { GatewayInstancePluginsTable } from '../features/gateway-instances/components/GatewayInstancePluginsTable';
import { GatewayInstanceSystemPropertiesTable } from '../features/gateway-instances/components/GatewayInstanceSystemPropertiesTable';
import { useGatewayInstanceDetail } from '../features/gateway-instances/hooks/useGatewayInstanceDetail';
import { buildInformationRows } from '../features/gateway-instances/utils/buildInformationRows';

/**
 * FOUND-34 — Classic Environment tab: Information / Plugins / System properties.
 * Observe dashboard link from Jira AC is not in Classic — deferred for product confirmation.
 */
export function GatewayInstanceEnvironmentPage() {
    const { instanceId } = useParams<{ instanceId: string }>();
    const { data: instance, isLoading, isError } = useGatewayInstanceDetail(instanceId);

    const informationRows = useMemo(() => (instance ? buildInformationRows(instance) : []), [instance]);
    const plugins = useMemo(() => instance?.plugins ?? [], [instance?.plugins]);
    const systemProperties = useMemo(() => {
        if (!instance?.systemProperties) return [];
        return Object.entries(instance.systemProperties).map(([name, value]) => ({ name, value }));
    }, [instance?.systemProperties]);

    if (isLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-4 w-72" />
                <Skeleton className="h-48 w-full" />
                <Skeleton className="h-48 w-full" />
            </div>
        );
    }

    if (isError || !instance) {
        return <p className="text-sm text-muted-foreground">Failed to load environment details.</p>;
    }

    return (
        <div className="space-y-6" data-testid="gateway-instance-environment">
            <GatewayInstanceInformationTable rows={informationRows} />
            <GatewayInstancePluginsTable plugins={plugins} />
            <GatewayInstanceSystemPropertiesTable properties={systemProperties} />
        </div>
    );
}
