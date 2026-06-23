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
import { DEFAULT_TIME_RANGE, encodeObservabilityState } from '@gravitee/gamma-lib-observability';

const HTTP_PROXY_DASHBOARD_ID = 'http-proxy-overview';

export function buildApiAnalyticsPath(apiId: string): string {
    const base = `../observe/dashboards/${HTTP_PROXY_DASHBOARD_ID}`;
    const encoded = encodeObservabilityState({
        conditions: [{ field: 'API', label: 'API', operator: 'in', value: [apiId] }],
        timeRange: DEFAULT_TIME_RANGE,
    });
    if (!encoded) return base;
    return `${base}?${new URLSearchParams({ q: encoded.q, v: encoded.v }).toString()}`;
}
