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
import type { ApiDetailDto, TcpTarget } from '../types';

/** Structural — matches both `ApiDetailDto` and the narrower `PolicyStudioApiDetail`. */
interface ApiWithListeners {
    listeners?: { type?: string }[];
}

export function hasTcpListeners(api: ApiWithListeners | null | undefined): boolean {
    return Boolean(api?.listeners?.some(l => l.type === 'TCP'));
}

/** HTTP PROXY API without TCP listeners — health-check is available (legacy console rule). */
export function isHttpProxyApi(api: ApiDetailDto | null | undefined): boolean {
    if (!api) return false;
    if (api.type !== 'PROXY') return false;
    return !hasTcpListeners(api);
}

/** An endpoint's `configuration.target` is a plain URL for http-proxy, or a {host,port,secured} object for tcp-proxy. */
export function formatEndpointTarget(target: string | TcpTarget | undefined): string | undefined {
    if (target === undefined) return undefined;
    if (typeof target === 'string') return target;
    return `${target.host}:${target.port}`;
}
