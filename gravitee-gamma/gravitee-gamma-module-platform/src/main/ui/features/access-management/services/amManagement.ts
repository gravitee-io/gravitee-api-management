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
import { ApimApiError, gammaFetchJson } from '../../../shared/api/apimClient';
import type {
    AmConnectionRequest,
    AmConnectionTestResult,
    AmConnectionView,
    AmDomain,
    AmEnvironment,
    AmGatewayEntrypoint,
} from '../types/amManagement';
import { type AmConfig, moduleBaseUrl } from '../utils/amConfig';

export function listEnvironments(cfg: AmConfig): Promise<AmEnvironment[]> {
    return gammaFetchJson<AmEnvironment[]>(`${moduleBaseUrl(cfg)}/environments`);
}

export function listDomains(cfg: AmConfig, envId: string, q?: string): Promise<AmDomain[]> {
    const suffix = q ? `?q=${encodeURIComponent(q)}` : '';
    return gammaFetchJson<AmDomain[]>(`${moduleBaseUrl(cfg)}/environments/${encodeURIComponent(envId)}/domains${suffix}`);
}

export function getDomain(cfg: AmConfig, envId: string, domainId: string): Promise<AmDomain> {
    return gammaFetchJson<AmDomain>(
        `${moduleBaseUrl(cfg)}/environments/${encodeURIComponent(envId)}/domains/${encodeURIComponent(domainId)}`,
    );
}

export function listDomainEntrypoints(cfg: AmConfig, envId: string, domainId: string): Promise<AmGatewayEntrypoint[]> {
    return gammaFetchJson<AmGatewayEntrypoint[]>(
        `${moduleBaseUrl(cfg)}/environments/${encodeURIComponent(envId)}/domains/${encodeURIComponent(domainId)}/entrypoints`,
    );
}

export function getAmConnection(cfg: AmConfig): Promise<AmConnectionView> {
    return gammaFetchJson<AmConnectionView>(`${moduleBaseUrl(cfg)}/am-config`);
}

export function saveAmConnection(cfg: AmConfig, payload: AmConnectionRequest): Promise<AmConnectionView> {
    return gammaFetchJson<AmConnectionView>(`${moduleBaseUrl(cfg)}/am-config`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function testAmConnection(cfg: AmConfig, payload: AmConnectionRequest): Promise<AmConnectionTestResult> {
    return gammaFetchJson<AmConnectionTestResult>(`${moduleBaseUrl(cfg)}/am-config/_test`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

/** True when the module reports AM is unreachable or not yet configured — a recoverable state. */
export function isAmUnavailable(error: unknown): boolean {
    if (!(error instanceof ApimApiError)) return false;
    if (error.status === 503) return true;
    return (error.body as { code?: unknown } | undefined)?.code === 'am_not_configured';
}
