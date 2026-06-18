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
const STORAGE_KEY = 'platform.am-config.v2';
const PLUGIN_ID = 'platform';

export interface AmConfig {
    organizationId: string;
    // AM-side environment / domain selected for the connection.
    environmentId: string;
    domainId: string;
    // Current Gravitee environment, used only to scope the module URL so the
    // ENVIRONMENT_AM_CONFIGURATION permission check resolves. Not persisted state of the connection.
    graviteeEnvironmentId: string;
}

const EMPTY: AmConfig = { organizationId: '', environmentId: '', domainId: '', graviteeEnvironmentId: '' };

export function loadAmConfig(): AmConfig {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return EMPTY;
        return { ...EMPTY, ...(JSON.parse(raw) as Partial<AmConfig>) };
    } catch {
        return EMPTY;
    }
}

export function saveAmConfig(cfg: AmConfig): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cfg));
}

export function moduleBaseUrl(cfg: AmConfig): string {
    // Relative to the bootstrap-resolved gammaBaseURL; AM settings hang off PlatformRootResource at /am.
    // Routed under the Gravitee environment so GraviteeContext carries an env id and the
    // ENVIRONMENT_AM_CONFIGURATION permission check resolves (the connection itself stays org-scoped).
    return `/organizations/${encodeURIComponent(cfg.organizationId)}/environments/${encodeURIComponent(
        cfg.graviteeEnvironmentId,
    )}/modules/${PLUGIN_ID}/am`;
}
