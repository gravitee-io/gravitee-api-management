/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Button } from '@gravitee/graphene-core';
import { useNavigate } from 'react-router-dom';

import { pathForAgentTab } from './tabs';

export function AgentSubscribeTab({ apiName }: { apiName: string }) {
    return (
        <div className="space-y-3">
            <h2 className="text-base font-semibold">Subscribe to {apiName}</h2>
            <p className="text-sm text-muted-foreground">
                Choose a plan, accept terms if required, and get credentials to call this agent through the gateway.
            </p>
        </div>
    );
}

export function AgentChatTab({ apiId }: { apiId: string }) {
    const navigate = useNavigate();
    return (
        <div className="space-y-3">
            <p className="text-sm text-muted-foreground">Chat is available after you subscribe.</p>
            <Button type="button" onClick={() => navigate(pathForAgentTab(apiId, 'subscribe'))}>
                Subscribe to this agent
            </Button>
        </div>
    );
}
