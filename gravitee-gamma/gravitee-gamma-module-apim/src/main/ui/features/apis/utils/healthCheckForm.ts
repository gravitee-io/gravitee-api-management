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
import { HTTP_HEALTH_CHECK_PLUGIN } from '../services/apiPlugins';
import type { HealthCheckConfiguration, HealthCheckService } from '../types';
import { validateCron } from './cronSchedule';

export interface HealthCheckHeaderRow {
    _id: string;
    name: string;
    value: string;
}

export interface HealthCheckFormState {
    enabled: boolean;
    /** Endpoint-only: inherit group health-check configuration. */
    inherit?: boolean;
    configuration: HealthCheckConfigFormState;
}

export interface HealthCheckConfigFormState {
    schedule: string;
    method: string;
    target: string;
    overrideEndpointPath: boolean;
    headers: HealthCheckHeaderRow[];
    assertion: string;
    successThreshold: number;
    failureThreshold: number;
}

export const DEFAULT_HEALTH_CHECK_CONFIG: HealthCheckConfigFormState = {
    schedule: '0 */5 * * * *',
    method: 'GET',
    target: '/',
    overrideEndpointPath: true,
    headers: [],
    assertion: '{#response.status == 200}',
    successThreshold: 2,
    failureThreshold: 2,
};

export const HTTP_HEALTH_CHECK_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'CONNECT', 'OPTIONS', 'TRACE'] as const;

export function parseHealthCheckConfig(config: HealthCheckConfiguration | undefined): HealthCheckConfigFormState {
    if (!config) return { ...DEFAULT_HEALTH_CHECK_CONFIG, headers: [] };
    return {
        schedule: config.schedule ?? DEFAULT_HEALTH_CHECK_CONFIG.schedule,
        method: config.method ?? DEFAULT_HEALTH_CHECK_CONFIG.method,
        target: config.target ?? DEFAULT_HEALTH_CHECK_CONFIG.target,
        overrideEndpointPath: config.overrideEndpointPath ?? DEFAULT_HEALTH_CHECK_CONFIG.overrideEndpointPath,
        headers: (config.headers ?? []).map(h => ({
            _id: Math.random().toString(36).slice(2, 10),
            name: h.name ?? '',
            value: h.value ?? '',
        })),
        assertion: config.assertion ?? DEFAULT_HEALTH_CHECK_CONFIG.assertion,
        successThreshold: config.successThreshold ?? DEFAULT_HEALTH_CHECK_CONFIG.successThreshold,
        failureThreshold: config.failureThreshold ?? DEFAULT_HEALTH_CHECK_CONFIG.failureThreshold,
    };
}

export function healthCheckFromGroup(service: HealthCheckService | undefined): HealthCheckFormState {
    return {
        enabled: service?.enabled ?? false,
        configuration: parseHealthCheckConfig(service?.configuration),
    };
}

export function healthCheckFromEndpoint(
    endpointService: HealthCheckService | undefined,
    groupService: HealthCheckService | undefined,
): HealthCheckFormState {
    const inherit = endpointService?.overrideConfiguration !== true;
    const source = inherit ? groupService : endpointService;
    return {
        enabled: source?.enabled ?? false,
        inherit,
        configuration: parseHealthCheckConfig(inherit ? groupService?.configuration : endpointService?.configuration),
    };
}

export function serializeHealthCheckConfig(form: HealthCheckConfigFormState): HealthCheckConfiguration {
    return {
        schedule: form.schedule.trim(),
        method: form.method,
        target: form.target.trim(),
        overrideEndpointPath: form.overrideEndpointPath,
        headers: form.headers.filter(h => h.name.trim()).map(h => ({ name: h.name.trim(), value: h.value })),
        assertion: form.assertion.trim() || DEFAULT_HEALTH_CHECK_CONFIG.assertion,
        successThreshold: form.successThreshold,
        failureThreshold: form.failureThreshold,
    };
}

export function buildGroupHealthCheckService(form: HealthCheckFormState): HealthCheckService | undefined {
    if (!form.enabled) return undefined;
    return {
        enabled: true,
        type: HTTP_HEALTH_CHECK_PLUGIN,
        configuration: serializeHealthCheckConfig(form.configuration),
        overrideConfiguration: false,
    };
}

export function buildEndpointHealthCheckService(form: HealthCheckFormState): HealthCheckService | undefined {
    if (form.inherit) return undefined;
    if (!form.enabled) {
        return {
            enabled: false,
            type: HTTP_HEALTH_CHECK_PLUGIN,
            configuration: serializeHealthCheckConfig(form.configuration),
            overrideConfiguration: true,
        };
    }
    return {
        enabled: true,
        type: HTTP_HEALTH_CHECK_PLUGIN,
        configuration: serializeHealthCheckConfig(form.configuration),
        overrideConfiguration: true,
    };
}

/** List-row badge for HTTP proxy endpoints (legacy `toEndpointOptions` parity). */
export function getEndpointHealthCheckBadge(
    group: { services?: { healthCheck?: HealthCheckService } },
    endpoint: { type: string; services?: { healthCheck?: HealthCheckService } },
): { label: string; tooltip: string } | null {
    if (endpoint.type !== 'http-proxy') return null;
    const groupEnabled = group.services?.healthCheck?.enabled ?? false;
    const endpointEnabled = endpoint.services?.healthCheck?.enabled ?? false;
    if (!groupEnabled && !endpointEnabled) return null;
    return {
        label: 'Health Check',
        tooltip: endpointEnabled
            ? 'Health check enabled by endpoint configuration'
            : 'Health check enabled via inherited group configuration',
    };
}

export function validateHealthCheckForm(form: HealthCheckFormState): Record<string, string> {
    const errors: Record<string, string> = {};
    if (!form.enabled || form.inherit) return errors;

    const config = form.configuration;
    if (!config.target.trim()) errors.target = 'Target is required.';
    const cronError = validateCron(config.schedule);
    if (cronError) errors.schedule = cronError;
    if (config.successThreshold < 1) errors.successThreshold = 'Must be at least 1.';
    if (config.failureThreshold < 1) errors.failureThreshold = 'Must be at least 1.';
    if (!config.assertion.trim()) errors.assertion = 'Assertion is required.';

    return errors;
}
