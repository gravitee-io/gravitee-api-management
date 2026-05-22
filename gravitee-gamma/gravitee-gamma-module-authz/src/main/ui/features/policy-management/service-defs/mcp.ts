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
import { ServerIcon } from '@gravitee/graphene-core/icons';
import type { ServicePageConfig } from '../ServicePolicyPage';

export const mcpServiceConfig: ServicePageConfig = {
    type: 'MCP',
    title: 'MCP Policies',
    description: 'Grant and restrict what principals can do on each MCP Server, its tools, prompts, and resources.',
    createButtonLabel: 'Create Policy for MCP',
    searchPlaceholder: 'Search MCP policies…',
    icon: ServerIcon,
    hasTarget: true,
    serviceLabel: 'MCP',
    resourceGroups: [
        { key: 'MCPServer', label: 'MCP Server' },
        { key: 'MCPTool', label: 'Tools' },
        { key: 'MCPPrompt', label: 'Prompts' },
        { key: 'MCPResource', label: 'Resources' },
    ],
    conditionSnippets: [
        { label: 'Business hours', snippet: 'context.time.hour >= 9 && context.time.hour < 17' },
        { label: 'Trusted device', snippet: 'context.device.trusted == true' },
        { label: 'Corporate IP range', snippet: 'context.source.ip.in_cidr("10.0.0.0/8")' },
    ],
};
