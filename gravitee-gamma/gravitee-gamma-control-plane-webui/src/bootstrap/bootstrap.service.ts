/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export interface BootstrapConfig {
    managementBaseURL: string;
    organizationId: string;
    gammaBaseURL: string;
}

function sanitizeBaseURL(url: string): string {
    return url.endsWith('/') ? url.slice(0, -1) : url;
}

let configPromise: Promise<BootstrapConfig> | null = null;

export function getBootstrapConfig(): Promise<BootstrapConfig> {
    if (!configPromise) {
        configPromise = fetchBootstrapConfig();
    }
    return configPromise;
}

async function fetchBootstrapConfig(): Promise<BootstrapConfig> {
    // 1. Fetch constants.json to get the generic base URL
    const constantsRes = await fetch('/constants.json');
    if (!constantsRes.ok) {
        throw new Error(`Failed to fetch constants.json: ${constantsRes.status}`);
    }
    const constants = await constantsRes.json();
    const gammaBaseURL = sanitizeBaseURL(constants.gammaBaseURL);

    // 2. Call /bootstrap to get the final base URL and organization ID
    const bootstrapRes = await fetch(`${gammaBaseURL}/ui/bootstrap`);
    if (!bootstrapRes.ok) {
        throw new Error(`Failed to fetch bootstrap config: ${bootstrapRes.status}`);
    }
    const bootstrap = await bootstrapRes.json();

    return {
        managementBaseURL: sanitizeBaseURL(bootstrap.managementBaseURL),
        organizationId: bootstrap.organizationId,
        gammaBaseURL: sanitizeBaseURL(bootstrap.gammaBaseURL),
    };
}
