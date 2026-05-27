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

/**
 * Single source of truth for the kind ↔ UI type ↔ policy type mapping.
 *
 * The canonical backend speaks lowercase kinds (`user`, `mcp`, `llm`, …) that
 * appear both as the first dotted segment of an entityId (`mcp.flight-status`)
 * and as the `_kind` attribute. The UI uses richer camel-case types
 * (`MCPServer`, `LLMRoute`) and a smaller policy taxonomy
 * (`MCP | AGENT | LLM | API | EVENT | CUSTOM`).
 *
 * Each entry maps one kind to one UI type and (optionally) one policy type.
 * `aliases` lets older or longer forms (`mcpserver`, `service-account`) resolve
 * to the same UI type without polluting the canonical form used on the wire.
 */
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
    { canonical: 'agent', uiType: 'AgentIdentity', aliases: ['agentidentity'], policyType: 'AGENT' },
    { canonical: 'mcp', uiType: 'MCPServer', aliases: ['mcpserver'], policyType: 'MCP' },
    { canonical: 'llm', uiType: 'LLMRoute', aliases: ['llmroute'], policyType: 'LLM' },
    { canonical: 'api', uiType: 'API', policyType: 'API' },
    { canonical: 'event', uiType: 'Event', policyType: 'EVENT' },
    { canonical: 'resource', uiType: 'Resource' },
    // `action` entities live on their own page; mapped here so EntitiesPage can hide them.
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
