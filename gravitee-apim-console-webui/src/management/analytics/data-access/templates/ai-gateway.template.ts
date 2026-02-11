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

export const AI_GATEWAY_TEMPLATE: DashboardTemplate = {
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
    widgets: [],
  },
};
