/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { DashboardTemplate } from './dashboard-template.model';

export const HTTP_PROXY_TEMPLATE: DashboardTemplate = {
  id: 'proxy-performance',
  name: 'Proxy Generic Protocol',
  shortDescription: 'Monitor real-time API health, traffic trends, and service reliability.',
  description:
    'This dashboard provides a centralized view of global API performance, error distribution, and latency across your infrastructure. It enables teams to quickly identify service bottlenecks and ensure consistent reliability for all API consumers.',
  previewImage: 'assets/images/templates/proxy-preview.png',
  initialConfig: {
    labels: { Focus: 'HTTP / TCP', Theme: 'proxy' },
    widgets: [
      {
        id: 'proxy-requests',
        title: 'Requests',
        description: 'Requests count',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'HTTP_PROXY' }],
        },
      },
      {
        id: 'proxy-error-rate',
        title: 'Error Rate',
        description: 'Percentage of responses in error',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 1 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_ERROR_RATE', measures: ['PERCENTAGE'] }],
        },
      },
      {
        id: 'proxy-average-latency',
        title: 'Average Latency in ms',
        description: 'Average latency of the Gateway',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 2 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['AVG'] }],
        },
      },
      {
        id: 'proxy-average-response-time',
        title: 'Average Response Time in ms',
        description: 'Average response time of the Gateway',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 3 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_RESPONSE_TIME', measures: ['AVG'] }],
        },
      },
      {
        id: 'proxy-http-statuses',
        title: 'HTTP Statuses',
        description: 'Number of HTTP requests per HTTP Status',
        type: 'doughnut',
        layout: { cols: 1, rows: 2, y: 1, x: 0 },
        request: {
          type: 'facets',
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
        },
      },
      {
        id: 'proxy-response-time',
        title: 'Response Time',
        description: 'Average response time of the Endpoint and Gateway in ms',
        type: 'time-series-line',
        layout: { cols: 3, rows: 2, y: 1, x: 1 },
        request: {
          type: 'time-series',
          by: [],
          metrics: [
            { name: 'HTTP_ENDPOINT_RESPONSE_TIME', measures: ['AVG'] },
            { name: 'HTTP_GATEWAY_RESPONSE_TIME', measures: ['AVG'] },
          ],
        },
      },
      {
        id: 'proxy-response-statuses',
        title: 'Response Statuses',
        description: 'Number of response statuses over time',
        type: 'time-series-bar',
        layout: { cols: 3, rows: 2, y: 3, x: 0 },
        request: {
          type: 'time-series',
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
        },
      },
      {
        id: 'proxy-top-5-applications',
        title: 'Top 5 Applications',
        description: 'Top 5 applications by number of HTTP requests',
        type: 'pie',
        layout: { cols: 1, rows: 2, y: 3, x: 3 },
        request: {
          type: 'facets',
          limit: 5,
          by: ['APPLICATION'],
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
        },
      },
    ],
  },
};
