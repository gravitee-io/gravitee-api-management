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

import { DEFAULT_CORS_MAX_AGE, getInvalidAllowOrigins } from './corsValidators';
import { isConsoleSettingReadonly } from './isConsoleSettingReadonly';
import type { PortalSettings, PortalSettingsCors } from '../../security-plan-types/services/portalSettings';
import { isPortalSettingReadonly } from '../../security-plan-types/utils/isPortalSettingReadonly';
import type { CorsFieldReadonly, CorsFormState } from '../components/CorsSection';
import type { ConsoleSettings, ConsoleSettingsCors } from '../types/consoleSettings';

/** Integer.MAX_VALUE — the backend storage type for cors.maxAge. */
export const MAX_CORS_MAX_AGE = 2147483647;

const MANAGEMENT_CORS_READONLY_KEYS = {
    allowOrigin: 'http.api.management.cors.allow-origin',
    allowMethods: 'http.api.management.cors.allow-methods',
    allowHeaders: 'http.api.management.cors.allow-headers',
    exposedHeaders: 'http.api.management.cors.exposed-headers',
    maxAge: 'http.api.management.cors.max-age',
} as const;

const PORTAL_CORS_READONLY_KEYS = {
    allowOrigin: 'http.api.portal.cors.allow-origin',
    allowMethods: 'http.api.portal.cors.allow-methods',
    allowHeaders: 'http.api.portal.cors.allow-headers',
    exposedHeaders: 'http.api.portal.cors.exposed-headers',
    maxAge: 'http.api.portal.cors.max-age',
} as const;

export function parseCorsMaxAge(value: string): number | null {
    if (!/^\d+$/.test(value.trim())) return null;
    const parsed = Number(value);
    if (!Number.isSafeInteger(parsed) || parsed > MAX_CORS_MAX_AGE) return null;
    return parsed;
}

export function isCorsFormValid(state: CorsFormState): boolean {
    return parseCorsMaxAge(state.maxAge) !== null && getInvalidAllowOrigins(state.allowOrigin).length === 0;
}

function buildCorsFormState(cors: ConsoleSettingsCors | PortalSettingsCors | undefined): CorsFormState {
    return {
        allowOrigin: cors?.allowOrigin ?? [],
        allowMethods: cors?.allowMethods ?? [],
        allowHeaders: cors?.allowHeaders ?? [],
        exposedHeaders: cors?.exposedHeaders ?? [],
        maxAge: String(cors?.maxAge ?? DEFAULT_CORS_MAX_AGE),
    };
}

export function buildCorsFormStateFromConsoleSettings(settings: ConsoleSettings | undefined): CorsFormState {
    return buildCorsFormState(settings?.cors);
}

export function buildCorsFormStateFromPortalSettings(settings: PortalSettings | undefined): CorsFormState {
    return buildCorsFormState(settings?.cors);
}

export function buildManagementCorsFieldReadonly(settings: ConsoleSettings | undefined): CorsFieldReadonly {
    return {
        allowOrigin: isConsoleSettingReadonly(settings, MANAGEMENT_CORS_READONLY_KEYS.allowOrigin),
        allowMethods: isConsoleSettingReadonly(settings, MANAGEMENT_CORS_READONLY_KEYS.allowMethods),
        allowHeaders: isConsoleSettingReadonly(settings, MANAGEMENT_CORS_READONLY_KEYS.allowHeaders),
        exposedHeaders: isConsoleSettingReadonly(settings, MANAGEMENT_CORS_READONLY_KEYS.exposedHeaders),
        maxAge: isConsoleSettingReadonly(settings, MANAGEMENT_CORS_READONLY_KEYS.maxAge),
    };
}

export function buildPortalCorsFieldReadonly(settings: PortalSettings | undefined): CorsFieldReadonly {
    return {
        allowOrigin: isPortalSettingReadonly(settings, PORTAL_CORS_READONLY_KEYS.allowOrigin),
        allowMethods: isPortalSettingReadonly(settings, PORTAL_CORS_READONLY_KEYS.allowMethods),
        allowHeaders: isPortalSettingReadonly(settings, PORTAL_CORS_READONLY_KEYS.allowHeaders),
        exposedHeaders: isPortalSettingReadonly(settings, PORTAL_CORS_READONLY_KEYS.exposedHeaders),
        maxAge: isPortalSettingReadonly(settings, PORTAL_CORS_READONLY_KEYS.maxAge),
    };
}

export function buildCorsPatch(state: CorsFormState): ConsoleSettingsCors {
    const maxAge = parseCorsMaxAge(state.maxAge);
    if (maxAge === null) {
        throw new Error('Invalid CORS max age');
    }
    return {
        allowOrigin: state.allowOrigin,
        allowMethods: state.allowMethods,
        allowHeaders: state.allowHeaders,
        exposedHeaders: state.exposedHeaders,
        maxAge,
    };
}
