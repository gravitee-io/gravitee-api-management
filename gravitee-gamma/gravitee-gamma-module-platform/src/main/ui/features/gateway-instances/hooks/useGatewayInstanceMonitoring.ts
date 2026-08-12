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

import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useQuery } from '@tanstack/react-query';

import { getGatewayInstanceMonitoring } from '../services/instances';
import { gatewayInstanceKeys } from '../utils/queryKeys';

/** Classic InstanceService.getMonitoringData — poll only while STARTED (enabled). */
export function useGatewayInstanceMonitoring({
    instanceId,
    gatewayId,
    enabled,
}: {
    instanceId: string | undefined;
    gatewayId: string | undefined;
    enabled: boolean;
}) {
    const env = useEnvironment();

    return useQuery({
        queryKey: gatewayInstanceKeys.monitoring(env?.id ?? '', instanceId ?? '', gatewayId ?? ''),
        queryFn: () => getGatewayInstanceMonitoring(env!.id, instanceId!, gatewayId!),
        enabled: Boolean(env?.id && instanceId && gatewayId && enabled),
        refetchInterval: enabled ? 5_000 : false,
    });
}
