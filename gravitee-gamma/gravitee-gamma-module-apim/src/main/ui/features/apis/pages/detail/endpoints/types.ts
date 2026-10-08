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
import type { EndpointDto, EndpointGroupSharedConfiguration, LoadBalancerType } from '../../../types';
import type { HealthCheckFormState } from '../../../utils/healthCheckForm';
import { healthCheckFromGroup } from '../../../utils/healthCheckForm';

export type { LoadBalancerType };

/** Schema-driven shared configuration — plugin JSON Schema values as stored on the API definition. */
export type SharedConfigFormState = Record<string, unknown>;

/** Endpoint-level configuration from the plugin `/schema` (target URL, tcp target, …). */
export type EndpointConfigurationFormState = Record<string, unknown>;

export interface EndpointFormState {
    /** Local row id for React key management — not sent to backend. */
    _id: string;
    name: string;
    configuration: EndpointConfigurationFormState;
    weight: number;
    secondary: boolean;
    inheritConfiguration: boolean;
    tenants: string[];
    /**
     * Original backend DTO — preserved so that fields we don't manage in the form
     * (services, etc.) survive a save round-trip.
     */
    _originalDto?: EndpointDto;
    /**
     * Per-endpoint shared config override — only applied when inheritConfiguration is false.
     * Populated from the endpoint's sharedConfigurationOverride DTO on load; updated by the
     * Configuration step in EndpointForm.
     */
    _configOverride?: SharedConfigFormState;
    healthCheck: HealthCheckFormState;
}

export interface EndpointGroupFormState {
    name: string;
    loadBalancerType: LoadBalancerType;
    sharedConfig: SharedConfigFormState;
    healthCheck: HealthCheckFormState;
    endpoints: EndpointFormState[];
    /** Create flow: default endpoint `configuration` from plugin schema (Console parity). */
    defaultEndpointConfiguration?: EndpointConfigurationFormState;
}

/** Empty until JsonSchemaForm seeds plugin schema defaults (or an existing DTO is loaded). */
export const DEFAULT_SHARED_CONFIG: SharedConfigFormState = {};
export const DEFAULT_ENDPOINT_CONFIGURATION: EndpointConfigurationFormState = {};

export const DEFAULT_GROUP_FORM: EndpointGroupFormState = {
    name: '',
    loadBalancerType: 'ROUND_ROBIN',
    sharedConfig: DEFAULT_SHARED_CONFIG,
    healthCheck: healthCheckFromGroup(undefined),
    endpoints: [],
};

/** Load a DTO shared-configuration object into form state (pass-through for schema-driven forms). */
export function parseSharedConfigDto(sc: EndpointGroupSharedConfiguration | Record<string, unknown>): SharedConfigFormState {
    return { ...(sc as Record<string, unknown>) };
}

export function newEndpointRow(groupHealthCheck?: HealthCheckFormState): EndpointFormState {
    const groupHc = groupHealthCheck ?? healthCheckFromGroup(undefined);
    return {
        _id: Math.random().toString(36).slice(2, 10),
        name: '',
        configuration: {},
        weight: 1,
        secondary: false,
        inheritConfiguration: true,
        tenants: [],
        healthCheck: {
            enabled: groupHc.enabled,
            inherit: true,
            configuration: { ...groupHc.configuration },
        },
    };
}

/** Shared header row shape used by response templates. */
export interface HeaderEntry {
    _id: string;
    name: string;
    value: string;
}

export function newHeaderRow(): HeaderEntry {
    return { _id: Math.random().toString(36).slice(2, 10), name: '', value: '' };
}

export function headersFromRecord(headers: Record<string, string> | undefined): HeaderEntry[] {
    if (!headers || Object.keys(headers).length === 0) return [];
    return Object.entries(headers).map(([name, value]) => ({ _id: Math.random().toString(36).slice(2, 10), name, value }));
}

export function headersToRecord(headers: HeaderEntry[]): Record<string, string> | undefined {
    const entries = headers.filter(h => h.name.trim()).map(h => [h.name.trim(), h.value] as const);
    if (entries.length === 0) return undefined;
    return Object.fromEntries(entries);
}

/** Validate group name: required, no colons. */
export function validateGroupName(name: string): string | null {
    if (!name.trim()) return 'Name is required.';
    if (name.includes(':')) return 'Name must not contain colons.';
    return null;
}

/** Validate endpoint name: required, no colons. */
export function validateEndpointName(name: string): string | null {
    if (!name.trim()) return 'Name is required.';
    if (name.includes(':')) return 'Name must not contain colons.';
    return null;
}

/** http-proxy endpoint configuration requires a non-empty target (plugin schema). */
export function validateEndpointTarget(value: string): string | null {
    if (!value.trim()) return 'Target URL is required.';
    if (/\s/.test(value)) return 'Target URL must not contain whitespace.';
    return null;
}

/** Default endpoint for a new endpoint group (configuration from plugin schema form). */
export function buildDefaultEndpointForGroup(
    groupName: string,
    configuration: EndpointConfigurationFormState,
    type: 'http-proxy' | 'tcp-proxy' = 'http-proxy',
): EndpointDto {
    const cleanName = groupName.trim();
    return {
        name: `${cleanName} default endpoint`,
        type,
        inheritConfiguration: true,
        weight: 1,
        configuration,
    };
}
