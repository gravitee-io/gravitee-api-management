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
import type { CreateApiV4Payload, EntrypointV4, ListenerV4, PathV4 } from '../features/apis/types/api.types';
import type { ProxyConnectorBootstrap } from '../features/apis/types/api.types';
import type { ApiCreationState } from '../features/apis/types/models';

function normalizeTargetUrl(url: string): string {
    return url.trim();
}

function pathsFromDraft(data: ApiCreationState['proxy']): PathV4[] {
    if (data.enableVirtualHosts) {
        const rows = data.virtualHosts?.length ? data.virtualHosts : [{ host: '', path: '/', overrideAccess: false }];
        return rows.map(virtualHost => ({
            path: virtualHost.path?.trim() || '/',
            host: virtualHost.host?.trim() || undefined,
            overrideAccess: virtualHost.overrideAccess,
        }));
    }
    const p = data.contextPath?.trim() || '/';
    return [{ path: p.startsWith('/') ? p : `/${p}` }];
}

/**
 * Maps Gamma wizard draft + resolved HTTP proxy connectors to CreateApiV4,
 * mirroring Angular `ApiCreationV4Component.createApi$` for PROXY + HTTP listener.
 */
export function buildCreateApiV4(data: ApiCreationState, bootstrap: ProxyConnectorBootstrap): CreateApiV4Payload {
    const { entrypoint, endpoint } = bootstrap;
    const paths = pathsFromDraft(data.proxy);
    const target = normalizeTargetUrl(data.proxy.targetUrl);

    const entrypoints: EntrypointV4[] = [
        {
            type: entrypoint.id,
            configuration: {},
            qos: 'AUTO',
        },
    ];

    const listener: ListenerV4 = {
        type: entrypoint.supportedListenerType ?? 'HTTP',
        paths,
        entrypoints,
    };

    const groupName = `Default ${endpoint.name} group`;
    const endpointName = `Default ${endpoint.name}`;

    return {
        definitionVersion: 'V4',
        name: data.details.name.trim(),
        apiVersion: data.details.version.trim(),
        description: data.details.description?.trim() ?? '',
        type: 'PROXY',
        allowedInApiProducts: false,
        listeners: [listener],
        endpointGroups: [
            {
                name: groupName,
                type: endpoint.id,
                sharedConfiguration: {},
                endpoints: [
                    {
                        name: endpointName,
                        type: endpoint.id,
                        weight: 1,
                        inheritConfiguration: true,
                        configuration: { target },
                    },
                ],
            },
        ],
    };
}
