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

import { Button } from '@gravitee/graphene-core';
import { useNavigate } from 'react-router-dom';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { useDetailBasePath } from '../../shared/hooks/useDetailBasePath';
import type { IntegrationAgentStatus } from '../types/integration';
import { ENVIRONMENT_INTEGRATION_CREATE_PERMISSION } from '../utils/integrationPermissions';

type IntegrationDiscoverButtonProps = Readonly<{
    integrationId: string;
    agentStatus: IntegrationAgentStatus | undefined;
    isIngesting: boolean;
}>;

export function IntegrationDiscoverButton({ integrationId, agentStatus, isIngesting }: IntegrationDiscoverButtonProps) {
    const navigate = useNavigate();
    const basePath = useDetailBasePath('integrations', integrationId);
    const canDiscover = useHasPermission({ anyOf: [ENVIRONMENT_INTEGRATION_CREATE_PERMISSION] });

    if (!canDiscover) return null;

    const isEnabled = agentStatus === 'CONNECTED' && !isIngesting;

    return (
        <div className="flex justify-end">
            <Button size="sm" disabled={!isEnabled} onClick={() => navigate(`${basePath}/discover`)}>
                Discover APIs
            </Button>
        </div>
    );
}
