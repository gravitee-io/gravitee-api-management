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

import { Badge } from '@gravitee/graphene-core';
import { useEffect, type ComponentProps } from 'react';

import type { IntegrationAgentStatus } from '../types/integration';

const STATUS_CONFIG: Record<IntegrationAgentStatus, { label: string; variant: ComponentProps<typeof Badge>['variant'] }> = {
    CONNECTED: { label: 'Connected', variant: 'success' },
    DISCONNECTED: { label: 'Disconnected', variant: 'destructive' },
};

function statusConfig(agentStatus: IntegrationAgentStatus | undefined) {
    return agentStatus && Object.prototype.hasOwnProperty.call(STATUS_CONFIG, agentStatus) ? STATUS_CONFIG[agentStatus] : undefined;
}

export function IntegrationStatusBadge({ agentStatus }: Readonly<{ agentStatus: IntegrationAgentStatus | undefined }>) {
    const config = statusConfig(agentStatus);

    const unrecognizedStatus = agentStatus && !config ? agentStatus : undefined;
    useEffect(() => {
        if (unrecognizedStatus) {
            console.warn(
                `Integration agent status "${unrecognizedStatus}" is not one of ${Object.keys(STATUS_CONFIG).join(', ')}; rendering an empty Status cell`,
            );
        }
    }, [unrecognizedStatus]);

    if (!config) {
        return null;
    }
    return <Badge variant={config.variant}>{config.label}</Badge>;
}
