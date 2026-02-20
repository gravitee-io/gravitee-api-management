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

export const LLM_TEMPLATE: DashboardTemplate = {
  id: 'ai-gateway',
  name: 'AI Gateway',
  shortDescription: 'LLM & MCP metrics: token usage and cost tracking.',
  description:
    'Advanced monitoring for AI services. Track token usage, model performance, ' +
    'and latencies for LLM and MCP-based architectures. It provides deep visibility into ' +
    'prompt-response cycles and cost-efficiency metrics to ensure your AI infrastructure ' +
    'remains reliable and scalable under heavy load.',
  previewImage: 'assets/images/templates/ai-gateway-preview.png',
  initialConfig: {
    name: 'New AI Gateway Dashboard - ' + Date.now().toString(),
    labels: { Focus: 'LLM / MCP', Theme: 'AI' },
    widgets: [
      {
        id: crypto.randomUUID(),
        title: 'Requests',
        description: '-----',
        type: 'stats',
        layout: { cols: 2, rows: 1, x: 0, y: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Cost',
        description: '-----',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 2, y: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_ERRORS', measures: ['PERCENTAGE'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Tokens',
        description: '----',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 3, y: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['AVG'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Cost per Prompt',
        description: 'Average cost per request',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 4, y: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['AVG'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Cost per Completion',
        description: 'Average cost per request',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 5, y: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['AVG'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Tokens per prompt',
        description: 'Average tokens per request',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 6, y: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['AVG'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Tokens per completion',
        description: 'Average tokens per request',
        type: 'stats',
        layout: { cols: 1, rows: 1, x: 7, y: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_RESPONSE_TIME', measures: ['AVG'] }],
        },
      },

      // Row 2
      {
        id: crypto.randomUUID(),
        title: 'Calls per model',
        description: 'Number of calls per model',
        type: 'bar',
        layout: { cols: 2, rows: 2, x: 0, y: 1 },
        request: {
          type: 'time-series',
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
        },
      },
      {
        id: crypto.randomUUID(),
        title: 'Tokens consumption',
        description: 'Tokens consumed over the time',
        type: 'line',
        layout: { cols: 3, rows: 2, x: 2, y: 1 },
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
        title: 'Cost over time',
        description: 'Cost over time',
        type: 'line',
        layout: { cols: 3, rows: 2, x: 5, y: 1 },
        request: {
          type: 'time-series',
          by: [],
          metrics: [
            { name: 'HTTP_ENDPOINT_RESPONSE_TIME', measures: ['AVG'] },
            { name: 'HTTP_GATEWAY_RESPONSE_TIME', measures: ['AVG'] },
          ],
        },
      },

      // Row 3
      {
        id: crypto.randomUUID(),
        title: 'Response time',
        description: 'Average response time for the gateway and the API',
        type: 'line',
        layout: { cols: 3, rows: 2, x: 0, y: 3 },
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
        title: 'Response status',
        description: 'Hits repartition by HTTP Status',
        type: 'doughnut',
        layout: { cols: 2, rows: 2, x: 3, y: 3 },
        request: {
          type: 'facets',
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
        },
      },
    ],
  },
};
