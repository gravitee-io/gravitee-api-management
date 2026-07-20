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

import { HTTP_PROXY_TEMPLATE } from './http-proxy.template';
import { LLM_TEMPLATE } from './llm.template';
import { MCP_TEMPLATE } from './mcp.template';
import { DashboardTemplate } from './dashboard-template.model';

type TemplateWidget = NonNullable<DashboardTemplate['initialConfig']['widgets']>[number];

const widgetsOf = (template: DashboardTemplate): TemplateWidget[] => template.initialConfig.widgets ?? [];

const apiTypeFilters = (template: DashboardTemplate) =>
  widgetsOf(template).flatMap(widget => (widget.request?.filters ?? []).filter(filter => filter.name === 'API_TYPE'));

describe('Dashboard templates', () => {
  it('should not pin the HTTP proxy dashboard to a single API type', () => {
    expect(apiTypeFilters(HTTP_PROXY_TEMPLATE)).toEqual([]);
  });

  it('should leave the Requests widget free of a hardcoded API type', () => {
    const requests = widgetsOf(HTTP_PROXY_TEMPLATE).find(widget => widget.id === 'proxy-requests');

    expect(requests).toBeDefined();
    expect(requests?.request?.filters ?? []).toEqual([]);
  });

  it.each([
    ['LLM', LLM_TEMPLATE],
    ['MCP', MCP_TEMPLATE],
  ])('should keep the %s dashboard scoped to its own API type', (_name, template) => {
    expect(apiTypeFilters(template as DashboardTemplate).length).toBeGreaterThan(0);
  });
});
