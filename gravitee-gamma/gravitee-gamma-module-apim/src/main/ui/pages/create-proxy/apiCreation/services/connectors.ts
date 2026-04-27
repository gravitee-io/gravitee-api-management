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
import { getOrganizationV2BaseUrl, type ApimRuntimeConfig } from '../context/apimRuntimeContext';
import { apimFetchJson } from './apimFetch';
import type { ConnectorPluginDto, ProxyConnectorBootstrap } from '../dto/types';

const MCP_ENTRYPOINT_ID = 'mcp';

/** List sync (PROXY) entrypoint plugins — mirrors Angular `listSyncEntrypointPlugins`. */
export async function listSyncEntrypointPlugins(runtime: ApimRuntimeConfig): Promise<ConnectorPluginDto[]> {
    const base = getOrganizationV2BaseUrl(runtime);
    const all = await apimFetchJson<ConnectorPluginDto[]>(`${base}/plugins/entrypoints`);
    return all.filter((e) => e.supportedApiType === 'PROXY' && e.id !== MCP_ENTRYPOINT_ID);
}

export async function getEndpointPlugin(runtime: ApimRuntimeConfig, entrypointId: string): Promise<ConnectorPluginDto> {
    const base = getOrganizationV2BaseUrl(runtime);
    return apimFetchJson<ConnectorPluginDto>(`${base}/plugins/endpoints/${encodeURIComponent(entrypointId)}`);
}

/**
 * Resolves HTTP proxy entrypoint + paired endpoint plugin.
 * Prefers `http-proxy` when present (Console tests), otherwise first PROXY entrypoint.
 */
export async function resolveProxyConnectorBootstrap(runtime: ApimRuntimeConfig): Promise<ProxyConnectorBootstrap> {
    const entrypoints = await listSyncEntrypointPlugins(runtime);
    if (entrypoints.length === 0) {
        throw new Error('No PROXY entrypoint plugins are available in this organization.');
    }
    const preferred = entrypoints.find((e) => e.id === 'http-proxy') ?? entrypoints[0];
    const endpoint = await getEndpointPlugin(runtime, preferred.id);
    return { entrypoint: preferred, endpoint };
}
