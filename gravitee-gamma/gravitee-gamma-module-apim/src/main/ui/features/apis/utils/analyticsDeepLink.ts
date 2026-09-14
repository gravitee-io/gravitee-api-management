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
    return withObservabilityState(`../observe/dashboards/${HTTP_PROXY_DASHBOARD_ID}`, apiId);
}

/**
 * Same board and filter as {@link buildApiAnalyticsPath}, but as a link the browser can follow on its
 * own: the API-detail sidebar's Observability group opens it in a new tab, so a relative
 * react-router path would not do.
 *
 * @param moduleRoot - the module's mount path (e.g. `/environments/:hrid/apim`), the segment the
 *   observability section hangs off.
 */
export function buildApiDashboardHref(moduleRoot: string, apiId: string): string {
    return withObservabilityState(`${moduleRoot}/observe/dashboards/${HTTP_PROXY_DASHBOARD_ID}`, apiId);
}

/** Logs explorer, pre-filtered on one API. Counterpart of {@link buildApiDashboardHref}. */
export function buildApiLogsHref(moduleRoot: string, apiId: string): string {
    return withObservabilityState(`${moduleRoot}/observe/logs`, apiId);
}

function withObservabilityState(path: string, apiId: string): string {
    const encoded = encodeObservabilityState({
        conditions: [{ field: 'API', label: 'API', operator: 'in', value: [apiId] }],
        timeRange: DEFAULT_TIME_RANGE,
    });
    if (!encoded) return path;
    return `${path}?${new URLSearchParams({ q: encoded.q, v: encoded.v }).toString()}`;
}
