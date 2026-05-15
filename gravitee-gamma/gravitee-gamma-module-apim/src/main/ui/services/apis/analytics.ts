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
import { apimFetchJsonV2 } from '../../shared/api/apimClient';

/** Shape returned by GET /apis/{id}/analytics?type=STATS */
export interface ApiStatsAnalytics {
    analyticsType?: string;
    avg?: number;
    min?: number;
    max?: number;
    sum?: number;
    count?: number;
    rps?: number;
    rpm?: number;
    rph?: number;
}

export async function getApiAnalyticsStats(
    environmentId: string,
    apiId: string,
    from: number,
    to: number,
    interval: number,
): Promise<ApiStatsAnalytics> {
    return apimFetchJsonV2<ApiStatsAnalytics>(
        environmentId,
        `/apis/${encodeURIComponent(apiId)}/analytics?type=STATS&field=gateway-response-time-ms&from=${from}&to=${to}&interval=${interval}`,
    );
}
