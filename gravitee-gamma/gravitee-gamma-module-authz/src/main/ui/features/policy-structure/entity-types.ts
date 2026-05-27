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

export type EntityCategoryId = 'principal' | 'mcp' | 'api' | 'agent' | 'llm' | 'event' | 'custom';

export interface EntityCategory {
    id: EntityCategoryId;
    label: string;
}

export const CATEGORIES: EntityCategory[] = [
    { id: 'principal', label: 'Principals' },
    { id: 'mcp', label: 'MCP' },
    { id: 'api', label: 'APIs' },
    { id: 'agent', label: 'Agents' },
    { id: 'llm', label: 'LLMs' },
    { id: 'event', label: 'Events' },
    { id: 'custom', label: 'Custom' },
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

    LLMRoute: 'llm',
    LLMModel: 'llm',
    LLMProvider: 'llm',

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
