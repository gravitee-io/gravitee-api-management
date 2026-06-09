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
    environmentId: string;
    domainId: string;
}

const EMPTY: AmConfig = { organizationId: '', environmentId: '', domainId: '' };

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

export function isAmConfigured(cfg: AmConfig): boolean {
    return Boolean(cfg.organizationId && cfg.environmentId && cfg.domainId);
}

export function moduleBaseUrl(cfg: AmConfig): string {
    // Relative to the bootstrap-resolved gammaBaseURL; AM settings hang off PlatformRootResource at /am.
    return `/organizations/${encodeURIComponent(cfg.organizationId)}/modules/${PLUGIN_ID}/am`;
}
