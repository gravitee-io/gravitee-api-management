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

import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { Instance, InstanceSearchResult, MonitoringData } from '../types/instance';

/**
 * Classic: InstanceService.search(includeStopped, from, to, page, size)
 * GET .../instances/?includeStopped=&from=&to=&page=&size=
 * `page` is 0-based (classic passes filters.pagination.index - 1).
 */
export async function listGatewayInstances(
    environmentId: string,
    {
        includeStopped = true,
        from = 0,
        to = 0,
        page = 0,
        size = 10,
    }: {
        includeStopped?: boolean;
        from?: number;
        to?: number;
        page?: number;
        size?: number;
    } = {},
): Promise<InstanceSearchResult> {
    const params = new URLSearchParams({
        includeStopped: String(includeStopped),
        from: String(from),
        to: String(to),
        page: String(page),
        size: String(size),
    });
    return apimFetchJsonV1Env<InstanceSearchResult>(environmentId, `/instances/?${params}`);
}

/** Classic: InstanceService.get(id) — `id` is the event id from the list. */
export async function getGatewayInstance(environmentId: string, instanceId: string): Promise<Instance> {
    return apimFetchJsonV1Env<Instance>(environmentId, `/instances/${encodeURIComponent(instanceId)}`);
}

/**
 * Classic: InstanceService.getMonitoringData(id, gatewayId)
 * `id` = event id (route param); `gatewayId` = Instance.id from the detail payload.
 */
export async function getGatewayInstanceMonitoring(environmentId: string, instanceId: string, gatewayId: string): Promise<MonitoringData> {
    return apimFetchJsonV1Env<MonitoringData>(
        environmentId,
        `/instances/${encodeURIComponent(instanceId)}/monitoring/${encodeURIComponent(gatewayId)}`,
    );
}
