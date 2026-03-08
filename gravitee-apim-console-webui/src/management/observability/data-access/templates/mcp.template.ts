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

export const MCP_TEMPLATE: DashboardTemplate = {
  id: 'mcp',
  name: 'MCP',
  shortDescription: 'Monitor MCP protocol usage, method distribution, and gateway performance.',
  description:
    'This dashboard provides a centralized view of your MCP (Model Context Protocol) API usage. Track request volume and latency, analyze method, resource, tool, and prompt usage, monitor response status distribution, and observe gateway response times to optimize your MCP integrations.',
  previewImage: 'assets/images/templates/mcp-preview.png',
  initialConfig: {
    labels: { Focus: 'MCP', Theme: 'AI' },
    widgets: [
      {
        id: 'mcp-requests',
        title: 'MCP requests',
        description: 'Total number of requests targeting MCP APIs.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'MCP' }],
        },
      },
      {
        id: 'mcp-average-latency',
        title: 'Average latency',
        description: 'Average gateway latency for MCP requests.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 1 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['AVG'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'MCP' }],
        },
      },
      {
        id: 'mcp-max-latency',
        title: 'Max latency',
        description: 'Maximum gateway latency observed for MCP requests.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 2 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['MAX'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'MCP' }],
        },
      },
      {
        id: 'mcp-p90-latency',
        title: 'P90 latency',
        description: '90th percentile gateway latency for MCP requests.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 3 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['P90'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'MCP' }],
        },
      },
      {
        id: 'mcp-p99-latency',
        title: 'P99 latency',
        description: '99th percentile gateway latency for MCP requests.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 4 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_GATEWAY_LATENCY', measures: ['P99'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'MCP' }],
        },
      },
      {
        id: 'mcp-method-usage',
        title: 'Method usage',
        description: 'Distribution of MCP proxy methods by request count (top 10).',
        type: 'vertical-bar',
        layout: { cols: 2, rows: 2, y: 1, x: 0 },
        request: {
          type: 'facets',
          by: ['MCP_PROXY_METHOD'],
          limit: 10,
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'], sorts: [{ measure: 'COUNT', order: 'DESC' }] }],
        },
      },
      {
        id: 'mcp-method-usage-over-time',
        title: 'Method usage over time',
        description: 'Evolution of method usage over time',
        type: 'time-series-line',
        layout: { cols: 3, rows: 2, y: 1, x: 2 },
        request: {
          type: 'time-series',
          interval: '1h',
          by: ['MCP_PROXY_METHOD'],
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'], sorts: [{ measure: 'COUNT', order: 'DESC' }] }],
        },
      },
      {
        id: 'mcp-most-used-resources',
        title: 'Most used Resources',
        description: 'Top 5 most used MCP resources by request count.',
        type: 'vertical-bar',
        layout: { cols: 2, rows: 2, y: 3, x: 0 },
        request: {
          type: 'facets',
          by: ['MCP_PROXY_RESOURCE'],
          limit: 5,
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'], sorts: [{ measure: 'COUNT', order: 'DESC' }] }],
        },
      },
      {
        id: 'mcp-response-status-repartition',
        title: 'Response status repartition',
        description: 'Distribution of HTTP response status codes for MCP requests.',
        type: 'doughnut',
        layout: { cols: 1, rows: 2, y: 3, x: 2 },
        request: {
          type: 'facets',
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'MCP' }],
        },
      },
      {
        id: 'mcp-most-used-prompts',
        title: 'Most used Prompts',
        description: 'Top 5 most used MCP prompts by request count.',
        type: 'vertical-bar',
        layout: { cols: 2, rows: 2, y: 3, x: 3 },
        request: {
          type: 'facets',
          by: ['MCP_PROXY_PROMPT'],
          limit: 5,
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'], sorts: [{ measure: 'COUNT', order: 'DESC' }] }],
        },
      },
      {
        id: 'mcp-most-used-tools',
        title: 'Most used Tools',
        description: 'Top 5 most used MCP tools by request count.',
        type: 'vertical-bar',
        layout: { cols: 2, rows: 2, y: 5, x: 0 },
        request: {
          type: 'facets',
          by: ['MCP_PROXY_TOOL'],
          limit: 5,
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'], sorts: [{ measure: 'COUNT', order: 'DESC' }] }],
        },
      },
      {
        id: 'mcp-average-response-time',
        title: 'Average response time',
        description: 'Average gateway response time for MCP requests over time.',
        type: 'time-series-line',
        layout: { cols: 3, rows: 2, y: 2, x: 0 },
        request: {
          type: 'time-series',
          interval: '1h',
          by: [],
          metrics: [{ name: 'HTTP_GATEWAY_RESPONSE_TIME', measures: ['AVG'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'MCP' }],
        },
      },
    ],
  },
};
