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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type {
    ApiAvailability,
    ApiAverageResponseTime,
    ApiHealthResponseTimeOvertime,
    HealthCheckLogsRequestParams,
    HealthCheckLogsResponse,
    HealthField,
} from '../types/healthCheck';

const healthPath = (apiId: string) => `/apis/${encodeURIComponent(apiId)}/health`;

export async function getResponseTimeOvertime(
    envId: string,
    apiId: string,
    from: number,
    to: number,
): Promise<ApiHealthResponseTimeOvertime> {
    return apimFetchJsonV2<ApiHealthResponseTimeOvertime>(
        envId,
        `${healthPath(apiId)}/average-response-time-overtime?from=${from}&to=${to}`,
    );
}

export async function getAvailability(
    envId: string,
    apiId: string,
    from: number,
    to: number,
    field: HealthField,
): Promise<ApiAvailability> {
    return apimFetchJsonV2<ApiAvailability>(envId, `${healthPath(apiId)}/availability?from=${from}&to=${to}&field=${field}`);
}

export async function getAverageResponseTime(
    envId: string,
    apiId: string,
    from: number,
    to: number,
    field: HealthField,
): Promise<ApiAverageResponseTime> {
    return apimFetchJsonV2<ApiAverageResponseTime>(
        envId,
        `${healthPath(apiId)}/average-response-time?from=${from}&to=${to}&field=${field}`,
    );
}

export async function getHealthCheckLogs(
    envId: string,
    apiId: string,
    params: HealthCheckLogsRequestParams,
): Promise<HealthCheckLogsResponse> {
    const query = new URLSearchParams({
        from: String(params.from),
        to: String(params.to),
        page: String(params.page),
        perPage: String(params.perPage),
        success: String(params.success),
    });
    return apimFetchJsonV2<HealthCheckLogsResponse>(envId, `${healthPath(apiId)}/logs?${query.toString()}`);
}
