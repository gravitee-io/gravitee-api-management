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

export const PROXY_DASHBOARD_TEMPLATE: DashboardTemplate = {
  id: 'proxy-performance',
  name: 'Proxy Performance',
  description: 'Real-time monitoring of traffic, HTTP errors, and gateway latency.',
  previewImage: 'assets/images/templates/proxy-preview.png',
  initialConfig: {
    name: 'New Proxy Dashboard',
    labels: {},
    widgets: [
      {
        id: crypto.randomUUID(),
        title: 'Requests',
        description: 'Requests count',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Error Rate',
        description: 'Percentage of responses in error',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 1 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_ERRORS', measures: ['PERCENTAGE'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Average Latency',
        description: 'Average latency of the Gateway',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 2 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['AVG'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Average Response Time',
        description: 'Average response time of the Gateway',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 3 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_RESPONSE_TIME', measures: ['AVG'] }],
        },
      },
      {
        id: crypto.randomUUID(),
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
        id: crypto.randomUUID(),
        title: 'Response Time',
        description: 'Average response time of the Endpoint and Gateway',
        type: 'line',
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
        id: crypto.randomUUID(),
        title: 'Response Statuses',
        description: 'Number of response statuses over time',
        type: 'bar',
        layout: { cols: 3, rows: 2, y: 3, x: 0 },
        request: {
          type: 'time-series',
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
        },
      },
      {
        id: crypto.randomUUID(),
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
