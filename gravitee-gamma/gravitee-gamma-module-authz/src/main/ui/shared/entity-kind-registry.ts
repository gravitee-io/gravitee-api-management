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
import type { PolicyType } from './api/authz-api.types';

export interface EntityKindEntry {
    readonly canonical: string;
    readonly uiType: string;
    readonly policyType?: PolicyType;
    readonly aliases?: readonly string[];
}

export const ENTITY_KIND_REGISTRY: readonly EntityKindEntry[] = [
    { canonical: 'user', uiType: 'User' },
    { canonical: 'group', uiType: 'Group' },
    { canonical: 'serviceaccount', uiType: 'ServiceAccount', aliases: ['service-account', 'service_account'] },
    { canonical: 'agent-identity', uiType: 'AgentIdentity', aliases: ['agentidentity'], policyType: 'AGENT' },
    { canonical: 'mcp', uiType: 'MCPServer', aliases: ['mcpserver'], policyType: 'MCP' },
    { canonical: 'model', uiType: 'Model', aliases: ['llm', 'llmmodel', 'llmroute'], policyType: 'MODEL' },
    { canonical: 'agent', uiType: 'Agent', aliases: ['a2a', 'a2aagent'], policyType: 'AGENT' },
    { canonical: 'api', uiType: 'API', policyType: 'API' },
    { canonical: 'event', uiType: 'Event', policyType: 'EVENT' },
    { canonical: 'resource', uiType: 'Resource' },
    { canonical: 'action', uiType: 'Action' },
];

const BY_KIND = new Map<string, EntityKindEntry>();
const BY_UI_TYPE = new Map<string, EntityKindEntry>();
for (const entry of ENTITY_KIND_REGISTRY) {
    BY_KIND.set(entry.canonical, entry);
    for (const alias of entry.aliases ?? []) BY_KIND.set(alias, entry);
    BY_UI_TYPE.set(entry.uiType.toLowerCase(), entry);
}

/** Lowercase kind hint → UI type. Returns undefined when the hint is unknown. */
export function kindToUiType(kind: unknown): string | undefined {
    if (typeof kind !== 'string') return undefined;
    return BY_KIND.get(kind.toLowerCase())?.uiType;
}

/** UI type → canonical lowercase kind. Returns the input lowercased when unknown. */
export function uiTypeToKind(type: string): string {
    return BY_UI_TYPE.get(type.toLowerCase())?.canonical ?? type.toLowerCase();
}

/** EntityId (`<kind>.<id>`) → policy type. Anything without a registered policy
 *  type — including null/undefined entityIds — falls back to `CUSTOM`. */
export function deriveServiceType(entityId: string | null | undefined): PolicyType {
    if (!entityId) return 'CUSTOM';
    const prefix = entityId.split('.')[0]?.toLowerCase();
    if (!prefix) return 'CUSTOM';
    return BY_KIND.get(prefix)?.policyType ?? 'CUSTOM';
}
