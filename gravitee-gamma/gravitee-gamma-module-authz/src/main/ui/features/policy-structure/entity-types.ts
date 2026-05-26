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
    textColor: string;
}

export const CATEGORIES: EntityCategory[] = [
    { id: 'principal', label: 'Principals', textColor: 'text-blue-600 dark:text-blue-400' },
    { id: 'mcp', label: 'MCP', textColor: 'text-teal-600 dark:text-teal-400' },
    { id: 'api', label: 'APIs', textColor: 'text-indigo-600 dark:text-indigo-400' },
    { id: 'agent', label: 'Agents', textColor: 'text-orange-600 dark:text-orange-400' },
    { id: 'llm', label: 'LLMs', textColor: 'text-fuchsia-600 dark:text-fuchsia-400' },
    { id: 'event', label: 'Events', textColor: 'text-cyan-600 dark:text-cyan-400' },
    { id: 'custom', label: 'Custom', textColor: 'text-slate-600 dark:text-slate-400' },
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

export type AttrValue = string | number | boolean;

export type EntitySource = 'local' | 'scim' | 'directory';

export interface EntityInstance {
    uid: { type: string; id: string };
    displayName?: string;
    attrs: Record<string, AttrValue>;
    parents: Array<{ type: string; id: string }>;
    source: EntitySource;
    principalProvider?: string;
    importedAt?: string;
    _backendId?: string;
    createdAt?: string;
    updatedAt?: string;
}
