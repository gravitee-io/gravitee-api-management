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
  id: 'llm',
  name: 'LLM',
  shortDescription: 'Monitor real-time LLM usage, token consumption, and associated AI costs.',
  description:
    'This dashboard provides a centralized view of your LLM usage, token consumption, and costs. Track total and average tokens, monitor cost over time, analyze usage per model, and observe response status repartition to optimize your AI integrations.',
  previewImage: 'assets/images/templates/llm-preview.png',
  initialConfig: {
    labels: { Focus: 'LLM / Tokens', Theme: 'AI' },
    widgets: [
      {
        id: 'llm-requests',
        title: 'LLM requests',
        description: 'Number of requests targeting LLM providers.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 3, x: 4 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'LLM' }],
        },
      },
      {
        id: 'llm-total-tokens',
        title: 'Total tokens',
        description: 'Total number of tokens processed (prompt and completion).',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 0, x: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'LLM_PROMPT_TOTAL_TOKEN', measures: ['COUNT'] }],
        },
      },
      {
        id: 'llm-total-cost',
        title: 'Total cost',
        description: 'Total cost incurred by LLM usage.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 2, x: 2 },
        request: {
          type: 'measures',
          metrics: [{ name: 'LLM_PROMPT_TOKEN_TOTAL_COST', measures: ['COUNT'] }],
        },
      },
      {
        id: 'llm-average-cost-per-request',
        title: 'Average cost per request',
        description: 'Average cost incurred per LLM request.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 3, x: 2 },
        request: {
          type: 'measures',
          metrics: [{ name: 'LLM_PROMPT_TOKEN_TOTAL_COST', measures: ['AVG'] }],
        },
      },
      {
        id: 'llm-average-tokens-per-request',
        title: 'Average tokens per request',
        description: 'Average number of tokens consumed per LLM request.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 1, x: 0 },
        request: {
          type: 'measures',
          metrics: [{ name: 'LLM_PROMPT_TOTAL_TOKEN', measures: ['AVG'] }],
        },
      },
      {
        id: 'llm-total-requests',
        title: 'Total requests',
        description: 'Total number of HTTP requests processed by the gateway.',
        type: 'stats',
        layout: { cols: 1, rows: 1, y: 2, x: 4 },
        request: {
          type: 'measures',
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
        },
      },
      {
        id: 'llm-token-count-over-time',
        title: 'Token count over time',
        description: 'Evolution of token consumption (prompt, completion, and total).',
        type: 'time-series-line',
        layout: { cols: 3, rows: 2, y: 0, x: 1 },
        request: {
          type: 'time-series',
          by: [],
          metrics: [
            { name: 'LLM_PROMPT_TOTAL_TOKEN', measures: ['COUNT'] },
            { name: 'LLM_PROMPT_TOKEN_SENT', measures: ['COUNT'] },
            { name: 'LLM_PROMPT_TOKEN_RECEIVED', measures: ['COUNT'] },
          ],
        },
      },
      {
        id: 'llm-token-cost-over-time',
        title: 'Token cost over time',
        description: 'Evolution of LLM costs over time, broken down by prompt and completion.',
        type: 'time-series-line',
        layout: { cols: 2, rows: 2, y: 2, x: 0 },
        request: {
          type: 'time-series',
          interval: '1h',
          by: [],
          metrics: [
            { name: 'LLM_PROMPT_TOKEN_TOTAL_COST', measures: ['COUNT'] },
            { name: 'LLM_PROMPT_TOKEN_SENT_COST', measures: ['COUNT'] },
            { name: 'LLM_PROMPT_TOKEN_RECEIVED_COST', measures: ['COUNT'] },
          ],
        },
      },
      {
        id: 'llm-total-tokens-per-model',
        title: 'Total tokens per model',
        description: 'Distribution of total tokens consumed across different LLM models.',
        type: 'doughnut',
        layout: { cols: 1, rows: 2, y: 0, x: 4 },
        request: {
          type: 'facets',
          by: ['LLM_PROXY_MODEL'],
          limit: 5,
          metrics: [{ name: 'LLM_PROMPT_TOTAL_TOKEN', measures: ['COUNT'], sorts: [{ measure: 'COUNT', order: 'DESC' }] }],
        },
      },
      {
        id: 'llm-response-status-repartition',
        title: 'Response status repartition',
        description: 'Distribution of HTTP response status codes for LLM requests.',
        type: 'doughnut',
        layout: { cols: 1, rows: 2, y: 2, x: 3 },
        request: {
          type: 'facets',
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [{ name: 'HTTP_REQUESTS', measures: ['COUNT'] }],
          filters: [{ name: 'API_TYPE', operator: 'EQ', value: 'LLM' }],
        },
      },
    ],
  },
};
