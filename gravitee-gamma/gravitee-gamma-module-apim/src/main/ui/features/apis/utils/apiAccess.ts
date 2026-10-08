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
import type { ApiListItem } from '../types';

function listenerHost(host: string | { host?: string }): string {
    return typeof host === 'string' ? host : (host.host ?? '');
}

/**
 * Access strings for the API list. Classic `getApiAccess` (`shared/utils/api-access.util.ts`):
 * TCP hosts when any exist, otherwise every HTTP host+path. The slash is kept.
 */
export function getApiAccessPaths(api: ApiListItem): string[] {
    const listeners = api.listeners ?? [];
    const tcpHosts = listeners
        .filter(listener => listener.type === 'TCP')
        .flatMap(listener => (listener.hosts ?? []).map(listenerHost))
        .filter(Boolean);
    if (tcpHosts.length > 0) return tcpHosts;

    return listeners
        .filter(listener => listener.type === 'HTTP')
        .flatMap(listener => (listener.paths ?? []).map(path => `${path.host ?? ''}${path.path ?? ''}`))
        .filter(Boolean);
}

/** First access string. Classic `getApiContextPath`. */
export function getApiAccessPath(api: ApiListItem): string | null {
    return getApiAccessPaths(api)[0] ?? null;
}
