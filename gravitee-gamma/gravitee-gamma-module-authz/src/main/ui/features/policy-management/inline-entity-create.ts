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
import type { PolicyType } from '../../shared/api/authz-api.types';
import type { ChipOption } from '../../shared/chip-option';
import { kindToUiType } from '../../shared/entity-kind-registry';

export type InlineEntityKind = 'PRINCIPAL' | 'RESOURCE';

export interface InlineCreatePreset {
    readonly canonical: string;
    readonly label: string;
}

export interface InlineCreateInput {
    readonly canonicalPrefix: string;
    readonly slug: string;
    readonly displayName: string;
}

export interface InlineCreateConfig {
    readonly kind: InlineEntityKind;
    readonly presets: readonly InlineCreatePreset[];
    readonly defaultCanonical: string;
    readonly create: (input: InlineCreateInput) => ChipOption;
}

export const PRINCIPAL_INLINE_PRESETS: readonly InlineCreatePreset[] = [
    { canonical: 'user', label: 'User' },
    { canonical: 'group', label: 'Group' },
    { canonical: 'serviceaccount', label: 'Service Account' },
    { canonical: 'agent-identity', label: 'Agent Identity' },
];

export const RESOURCE_INLINE_PRESETS: readonly InlineCreatePreset[] = [
    { canonical: 'mcp', label: 'MCP' },
    { canonical: 'model', label: 'Model' },
    { canonical: 'agent', label: 'Agent' },
    { canonical: 'api', label: 'API' },
    { canonical: 'event', label: 'Event' },
    { canonical: 'resource', label: 'Resource' },
];

/** Single entityId segment, mirroring the backend regex between dots. */
export const SLUG_REGEX = /^[a-z0-9_-]+$/;

export function slugify(value: string): string {
    return value
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/[^a-z0-9_-]+/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-+|-+$/g, '');
}

/** Mirror of ServicePolicyPage.serviceResourceOptions group derivation so an
 *  optimistically-created resource chip matches the canonical option id. */
function resourceGroupOf(canonicalPrefix: string): string {
    switch (canonicalPrefix) {
        case 'mcp':
            return 'MCP';
        case 'model':
        case 'llm':
            return 'Model';
        case 'agent':
            return 'Agent';
        case 'api':
            return 'API';
        default:
            return 'Resource';
    }
}

function capitalize(value: string): string {
    return value.length === 0 ? value : value[0]!.toUpperCase() + value.slice(1);
}

function toChipOption(kind: InlineEntityKind, input: InlineCreateInput): ChipOption {
    const { canonicalPrefix, slug, displayName } = input;
    if (kind === 'PRINCIPAL') {
        const uiType = kindToUiType(canonicalPrefix) ?? capitalize(canonicalPrefix);
        return { id: `${uiType}::"${slug}"`, label: slug, group: uiType };
    }
    const group = resourceGroupOf(canonicalPrefix);
    return { id: `${group}::"${slug}"`, label: displayName || slug, group };
}

const POLICY_TYPE_TO_CANONICAL: Partial<Record<PolicyType, string>> = {
    MCP: 'mcp',
    MODEL: 'model',
    AGENT: 'agent',
    API: 'api',
    EVENT: 'event',
};

export function defaultResourceCanonical(type: PolicyType): string {
    return POLICY_TYPE_TO_CANONICAL[type] ?? 'resource';
}

export function makeInlineEntityCreator(kind: InlineEntityKind): (input: InlineCreateInput) => ChipOption {
    return (input: InlineCreateInput): ChipOption => toChipOption(kind, input);
}
