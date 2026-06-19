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
import type { Dashboard } from '@gravitee/gamma-lib-observability';

import { scopeWidgets, TOP_BY_COUNT } from './shared';

export const HTTP_PROXY_DASHBOARD: Dashboard = {
    id: 'http-proxy-overview',
    title: 'HTTP Proxy — Overview',
    description:
        'HTTP Proxy traffic health: request volume, errors, latency percentiles, status distribution, and top APIs, applications, paths, and plans.',
    widgets: scopeWidgets('HTTP_PROXY', [
        // Row 0 — KPIs
        {
            id: 'http-proxy-kpi',
            title: 'Key Metrics',
            type: 'metric-group',
            items: [
                { metric: 'HTTP_REQUESTS', measure: 'COUNT', label: 'Total Requests', sentiment: 'neutral' },
                { metric: 'HTTP_ERROR_RATE', measure: 'PERCENTAGE', label: 'Error Rate', sentiment: 'lower-is-better' },
                { metric: 'HTTP_GATEWAY_RESPONSE_TIME', measure: 'AVG', label: 'Avg Response Time', sentiment: 'lower-is-better' },
                { metric: 'HTTP_GATEWAY_RESPONSE_TIME', measure: 'P95', label: 'P95 Response Time', sentiment: 'lower-is-better' },
                { metric: 'HTTP_GATEWAY_RESPONSE_TIME', measure: 'MAX', label: 'Max Response Time', sentiment: 'lower-is-better' },
            ],
            layout: { x: 0, y: 0, cols: 12, rows: 1 },
        },

        // Row 1 — Requests by HTTP Method + P95 (9c) + Status Distribution doughnut (3c)
        {
            id: 'http-proxy-method-over-time',
            title: 'Requests by HTTP Method + P95',
            description: 'Request volume per HTTP method (stacked) with P95 response time overlay.',
            type: 'cartesian',
            series: [
                {
                    metric: 'HTTP_REQUESTS',
                    measure: 'COUNT',
                    representation: 'bar',
                    by: ['HTTP_METHOD'],
                    stackId: 'method',
                    axisId: 'left',
                    unit: 'req',
                },
                {
                    metric: 'HTTP_GATEWAY_RESPONSE_TIME',
                    measure: 'P95',
                    representation: 'line',
                    axisId: 'right',
                    unit: 'ms',
                    curveType: 'monotone',
                },
            ],
            layout: { x: 0, y: 1, cols: 9, rows: 2 },
        },
        {
            id: 'http-proxy-status-distribution',
            title: 'Status Distribution',
            description: 'Share of responses by status group, with individual status on hover.',
            type: 'doughnut',
            by: ['HTTP_STATUS_CODE_GROUP', 'HTTP_STATUS'],
            valueLabel: 'Requests',
            layout: { x: 9, y: 1, cols: 3, rows: 2 },
        },

        // Row 3 — Requests vs Avg Response Time (5c) + Top 5 APIs (4c) + Top 5 Applications doughnut (3c)
        {
            id: 'http-proxy-requests-vs-latency',
            title: 'Requests vs Avg Response Time',
            description: 'Request volume correlated with average gateway response time.',
            type: 'cartesian',
            series: [
                { metric: 'HTTP_REQUESTS', measure: 'COUNT', representation: 'bar', axisId: 'left', unit: 'req' },
                {
                    metric: 'HTTP_GATEWAY_RESPONSE_TIME',
                    measure: 'AVG',
                    representation: 'line',
                    axisId: 'right',
                    unit: 'ms',
                    curveType: 'monotone',
                },
            ],
            layout: { x: 0, y: 3, cols: 5, rows: 2 },
        },
        {
            id: 'http-proxy-top-apis',
            title: 'Top 5 APIs',
            description: 'Most-used HTTP Proxy APIs by request count.',
            type: 'bar',
            by: ['API'],
            valueLabel: 'Requests',
            limit: 5,
            sorts: TOP_BY_COUNT,
            barLayout: 'horizontal',
            layout: { x: 5, y: 3, cols: 4, rows: 2 },
        },
        {
            id: 'http-proxy-top-applications',
            title: 'Top 5 Applications',
            description: 'Most active consumer applications by request count.',
            type: 'doughnut',
            by: ['APPLICATION'],
            valueLabel: 'Requests',
            limit: 5,
            sorts: TOP_BY_COUNT,
            layout: { x: 9, y: 3, cols: 3, rows: 2 },
        },

        // Row 5 — Gateway Latency Percentiles (6c) + Endpoint vs Gateway Response Time (6c)
        {
            id: 'http-proxy-latency-percentiles',
            title: 'Gateway Latency — Percentiles',
            description: 'P90 / P95 / P99 internal gateway processing time.',
            type: 'cartesian',
            series: [
                { metric: 'HTTP_GATEWAY_LATENCY', measure: 'P90', representation: 'line', unit: 'ms', curveType: 'monotone' },
                { metric: 'HTTP_GATEWAY_LATENCY', measure: 'P95', representation: 'line', unit: 'ms', curveType: 'monotone' },
                { metric: 'HTTP_GATEWAY_LATENCY', measure: 'P99', representation: 'line', unit: 'ms', curveType: 'monotone' },
            ],
            layout: { x: 0, y: 5, cols: 6, rows: 2 },
        },
        {
            id: 'http-proxy-endpoint-vs-gateway',
            title: 'Endpoint vs Gateway Response Time',
            description: 'Average backend response time compared to total gateway response time.',
            type: 'cartesian',
            series: [
                {
                    metric: 'HTTP_ENDPOINT_RESPONSE_TIME',
                    measure: 'AVG',
                    representation: 'line',
                    axisId: 'left',
                    unit: 'ms',
                    curveType: 'monotone',
                },
                {
                    metric: 'HTTP_GATEWAY_RESPONSE_TIME',
                    measure: 'AVG',
                    representation: 'line',
                    axisId: 'left',
                    unit: 'ms',
                    curveType: 'monotone',
                },
            ],
            layout: { x: 6, y: 5, cols: 6, rows: 2 },
        },

        // Row 7 — Status Over Time (6c) + Top 5 Paths bar (6c)
        {
            id: 'http-proxy-status-over-time',
            title: 'Status Over Time',
            description: 'Response status groups stacked over time.',
            type: 'cartesian',
            showTotal: true,
            totalLabel: 'Total requests',
            series: [
                {
                    metric: 'HTTP_REQUESTS',
                    measure: 'COUNT',
                    representation: 'bar',
                    by: ['HTTP_STATUS_CODE_GROUP'],
                    stackId: 'status',
                    unit: 'req',
                },
            ],
            layout: { x: 0, y: 7, cols: 6, rows: 2 },
        },
        {
            id: 'http-proxy-top-paths',
            title: 'Top 5 Paths',
            description: 'Most-requested API path mappings.',
            type: 'bar',
            by: ['HTTP_PATH_MAPPING'],
            valueLabel: 'Requests',
            limit: 5,
            sorts: TOP_BY_COUNT,
            barLayout: 'horizontal',
            layout: { x: 6, y: 7, cols: 6, rows: 2 },
        },

        // Row 9 — Request/Response Content Length (6c) + Top 5 Plans bar (6c)
        {
            id: 'http-proxy-content-length',
            title: 'Request / Response Content Length',
            description: 'Average request and response payload size over time.',
            type: 'cartesian',
            series: [
                {
                    metric: 'HTTP_REQUEST_CONTENT_LENGTH',
                    measure: 'AVG',
                    representation: 'line',
                    axisId: 'left',
                    unit: 'bytes',
                    curveType: 'monotone',
                },
                {
                    metric: 'HTTP_RESPONSE_CONTENT_LENGTH',
                    measure: 'AVG',
                    representation: 'line',
                    axisId: 'left',
                    unit: 'bytes',
                    curveType: 'monotone',
                },
            ],
            layout: { x: 0, y: 9, cols: 6, rows: 2 },
        },
        {
            id: 'http-proxy-top-plans',
            title: 'Top 5 Plans',
            description: 'Most-used subscription plans by request count.',
            type: 'bar',
            by: ['PLAN'],
            valueLabel: 'Requests',
            limit: 5,
            sorts: TOP_BY_COUNT,
            barLayout: 'horizontal',
            layout: { x: 6, y: 9, cols: 6, rows: 2 },
        },
    ]),
    filters: [],
    timeRange: { type: 'relative', period: '7d' },
    createdAt: 0,
    updatedAt: 0,
};
