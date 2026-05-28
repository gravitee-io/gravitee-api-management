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

export type EntityCategoryId = 'principal' | 'mcp' | 'api' | 'agent' | 'model' | 'event' | 'custom';

export interface EntityCategory {
    id: EntityCategoryId;
    label: string;
    textColor: string;
}

// chart-1..chart-10 are graphene's categorical palette (designed for distinct categories).
// We pick a stable mapping per entity category; custom falls back to a neutral muted tone.
export const CATEGORIES: EntityCategory[] = [
    { id: 'principal', label: 'Principals', textColor: 'text-chart-1' },
    { id: 'mcp', label: 'MCP', textColor: 'text-chart-2' },
    { id: 'api', label: 'APIs', textColor: 'text-chart-8' },
    { id: 'agent', label: 'Agents', textColor: 'text-chart-7' },
    { id: 'model', label: 'AI Models', textColor: 'text-chart-5' },
    { id: 'event', label: 'Events', textColor: 'text-chart-10' },
    { id: 'custom', label: 'Custom', textColor: 'text-muted-foreground' },
];

export function getCategory(id: EntityCategoryId): EntityCategory | undefined {
    return CATEGORIES.find(c => c.id === id);
}

const ENTITY_CATEGORIES: Readonly<Record<string, EntityCategoryId>> = {
    User: 'principal',
    Group: 'principal',
    ServiceAccount: 'principal',
    AgentIdentity: 'principal',

    MCPServer: 'mcp',
    MCPTool: 'mcp',
    MCPPrompt: 'mcp',
    MCPResource: 'mcp',

    API: 'api',
    Endpoint: 'api',
    DataField: 'api',

    Agent: 'agent',
    AgentSkill: 'agent',
    AgentTool: 'agent',
    AgentMemory: 'agent',
    AgentKnowledge: 'agent',

    Model: 'model',

    EventStream: 'event',
    Topic: 'event',
    SchemaField: 'event',

    Application: 'custom',
    Asset: 'custom',
    Resource: 'custom',
};

export function getEntityCategoryId(name: string): EntityCategoryId | undefined {
    return ENTITY_CATEGORIES[name];
}

export type { AttrValue, EntityInstance, EntitySource } from '../../shared/entity.types';
