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

import { useId } from 'react';

import { IntegrationStatusBadge } from './IntegrationStatusBadge';
import type { IntegrationAgentStatus } from '../types/integration';

export function IntegrationAgentConnection({ agentStatus }: Readonly<{ agentStatus: IntegrationAgentStatus | undefined }>) {
    const headingId = useId();

    return (
        <section className="min-w-0 flex-1 space-y-2" data-testid="integration-agent-connection" aria-labelledby={headingId}>
            <h2 id={headingId} className="text-sm font-semibold">
                Agent Connection
            </h2>
            <IntegrationStatusBadge agentStatus={agentStatus} />
        </section>
    );
}
