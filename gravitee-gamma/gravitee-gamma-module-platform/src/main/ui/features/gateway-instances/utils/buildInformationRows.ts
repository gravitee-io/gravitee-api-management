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

import { formatInstanceDate } from './formatInstanceDate';
import type { Instance } from '../types/instance';

export type InformationRowTone = 'default' | 'success' | 'danger';

export interface InformationRow {
    type: string;
    value: string;
    tone?: InformationRowTone;
    /** Icon key resolved in the table cell. */
    icon:
        | 'hostname'
        | 'ip'
        | 'port'
        | 'state-started'
        | 'state-stopped'
        | 'state-unknown'
        | 'version'
        | 'started'
        | 'heartbeat'
        | 'tags'
        | 'tenant'
        | 'organizations'
        | 'environments'
        | 'stopped'
        | 'os';
}

function stateIcon(state: Instance['state']): InformationRow['icon'] {
    if (state === 'STARTED') return 'state-started';
    if (state === 'STOPPED') return 'state-stopped';
    return 'state-unknown';
}

function stateTone(state: Instance['state']): InformationRowTone {
    if (state === 'STARTED') return 'success';
    if (state === 'STOPPED') return 'danger';
    return 'default';
}

/** Classic: InstanceDetailsEnvironmentComponent.initInformationTable(). */
export function buildInformationRows(instance: Instance): InformationRow[] {
    const rows: InformationRow[] = [
        { icon: 'hostname', type: 'Hostname', value: instance.hostname || '—' },
        { icon: 'ip', type: 'IP', value: instance.ip || '—' },
        { icon: 'port', type: 'Port', value: instance.port || '—' },
        {
            icon: stateIcon(instance.state),
            type: 'State',
            value: instance.state || 'UNKNOWN',
            tone: stateTone(instance.state),
        },
        { icon: 'version', type: 'Version', value: instance.version || '—' },
        { icon: 'started', type: 'Started at', value: formatInstanceDate(instance.started_at) },
        { icon: 'heartbeat', type: 'Last heartbeat at', value: formatInstanceDate(instance.last_heartbeat_at) },
    ];

    if (instance.operating_system_name) {
        rows.push({ icon: 'os', type: 'OS', value: instance.operating_system_name });
    }

    if (instance.tags?.length) {
        rows.push({ icon: 'tags', type: 'Sharding tags', value: instance.tags.join(', ') });
    }

    if (instance.tenant) {
        rows.push({ icon: 'tenant', type: 'Tenant', value: instance.tenant });
    }

    if (instance.organizations_hrids?.length) {
        rows.push({ icon: 'organizations', type: 'Organizations', value: instance.organizations_hrids.join(', ') });
    }

    if (instance.environments_hrids?.length) {
        rows.push({ icon: 'environments', type: 'Environments', value: instance.environments_hrids.join(', ') });
    }

    if (instance.stopped_at) {
        rows.push({ icon: 'stopped', type: 'Stopped at', value: formatInstanceDate(instance.stopped_at) });
    }

    return rows;
}
