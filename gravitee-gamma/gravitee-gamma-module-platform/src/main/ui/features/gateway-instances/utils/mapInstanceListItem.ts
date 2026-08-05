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

import type { GatewayInstanceRow, InstanceListItem, InstanceState } from '../types/instance';

/** Classic list truncates version at the first '(' (build/revision suffix). */
export function shortInstanceVersion(version: string | null | undefined): string {
    if (!version) return '';
    if (version.includes('(')) {
        return version.substring(0, version.indexOf('(')).trim();
    }
    return version;
}

function normalizeState(state: string | undefined): InstanceState {
    if (state === 'STARTED' || state === 'STOPPED') return state;
    return 'UNKNOWN';
}

/** Classic navigates with event id (not gateway instance id); fall back to instance id when event is absent. */
export function mapInstanceListItem(instance: InstanceListItem): GatewayInstanceRow {
    return {
        id: instance.event || instance.id,
        hostname: instance.hostname ?? '',
        version: shortInstanceVersion(instance.version),
        state: normalizeState(instance.state),
        lastHeartbeat: instance.last_heartbeat_at ? new Date(instance.last_heartbeat_at) : null,
        os: instance.operating_system_name ?? '',
        ip: instance.ip ?? '',
        port: instance.port ?? '',
        tenant: instance.tenant ?? '',
        tags: instance.tags ?? [],
    };
}
