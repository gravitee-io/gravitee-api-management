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
import { useParams } from 'react-router-dom';

import { GatewayInstanceMonitoringView } from '../features/gateway-instances/components/GatewayInstanceMonitoringView';
import { useGatewayInstanceDetail } from '../features/gateway-instances/hooks/useGatewayInstanceDetail';
import { useGatewayInstanceMonitoring } from '../features/gateway-instances/hooks/useGatewayInstanceMonitoring';

function MonitoringSkeleton() {
    return (
        <div className="space-y-6" data-testid="gateway-instance-monitoring-skeleton">
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <Skeleton className="h-36 w-full" />
                <Skeleton className="h-36 w-full" />
                <Skeleton className="h-36 w-full" />
                <Skeleton className="h-36 w-full" />
            </div>
            <Skeleton className="h-96 w-full" />
            <div className="grid gap-4 md:grid-cols-2">
                <Skeleton className="h-48 w-full" />
                <Skeleton className="h-48 w-full" />
                <Skeleton className="h-48 w-full" />
                <Skeleton className="h-48 w-full" />
            </div>
        </div>
    );
}

export function GatewayInstanceMonitoringPage() {
    const { instanceId } = useParams<{ instanceId: string }>();
    const { data: instance, isLoading: isDetailLoading, isError: isDetailError } = useGatewayInstanceDetail(instanceId);
    const isStarted = instance?.state === 'STARTED';
    const {
        data: monitoringData,
        isLoading: isMonitoringLoading,
        isError: isMonitoringError,
    } = useGatewayInstanceMonitoring({
        instanceId,
        gatewayId: instance?.id,
        enabled: isStarted,
    });

    if (isDetailLoading) {
        return <MonitoringSkeleton />;
    }

    if (isDetailError || !instance) {
        return <p className="text-sm text-muted-foreground">Failed to load monitoring details.</p>;
    }

    if (!isStarted) {
        return (
            <p className="text-sm text-muted-foreground" data-testid="gateway-instance-monitoring-no-data">
                There is no data for stopped gateway instance
            </p>
        );
    }

    if (isMonitoringLoading && !monitoringData) {
        return <MonitoringSkeleton />;
    }

    if (isMonitoringError && !monitoringData) {
        return (
            <p className="text-sm text-muted-foreground" data-testid="gateway-instance-monitoring-error">
                Failed to load monitoring data. Please refresh and try again.
            </p>
        );
    }

    if (!monitoringData) {
        return (
            <p className="text-sm text-muted-foreground" data-testid="gateway-instance-monitoring-empty">
                There is no monitoring data for this gateway instance yet
            </p>
        );
    }

    return <GatewayInstanceMonitoringView data={monitoringData} />;
}
