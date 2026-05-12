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
import { Card } from '@gravitee/graphene-core';
import { useMemo } from 'react';

import { type FlowArrowVariant, type FlowNodeState, FlowConnector, FlowNode } from './ProxyFlowDiagram';
import type { ApiCreationState } from '../../types/models';
import { formatSecurityType } from '../../utils/securityFormatters';

type RequestPathPreviewProps = Readonly<{
    stepId: string | undefined;
    proxy: ApiCreationState['proxy'];
    securityType: ApiCreationState['security']['type'];
}>;

type NodePosition = 'client' | 'gateway' | 'security' | 'upstream';
type NodeStates = Record<NodePosition, FlowNodeState>;

function computeNodeStates(stepId: string | undefined): NodeStates {
    if (stepId === 'review-deploy') {
        return { client: 'complete', gateway: 'complete', security: 'complete', upstream: 'complete' };
    }
    if (stepId === 'essentials') {
        return { client: 'muted', gateway: 'active', security: 'active', upstream: 'active' };
    }
    if (stepId === 'configure-proxy') {
        return { client: 'muted', gateway: 'active', security: 'muted', upstream: 'active' };
    }
    if (stepId === 'secure') {
        return { client: 'muted', gateway: 'muted', security: 'active', upstream: 'muted' };
    }
    return { client: 'muted', gateway: 'muted', security: 'muted', upstream: 'muted' };
}

function arrowVariant(target: FlowNodeState): FlowArrowVariant {
    if (target === 'complete') return 'complete';
    if (target === 'active') return 'emphasized';
    return 'muted';
}

function stepCaption(stepId: string | undefined): string {
    if (stepId === 'configure-proxy') return 'Configuring entrypoints & upstream';
    if (stepId === 'secure') return 'Configuring security & access';
    if (stepId === 'review-deploy') return 'Ready to review & deploy';
    if (stepId === 'essentials') return 'Filling in essentials';
    return 'Defining API identity';
}

export function RequestPathPreview({ stepId, proxy, securityType }: RequestPathPreviewProps) {
    const gatewayUrl = useMemo(() => {
        if (proxy.enableVirtualHosts) {
            const first = proxy.virtualHosts?.[0];
            const host = first?.host?.trim() || '';
            const path = first?.path?.trim() || '/';
            return host ? `https://${host}${path}` : '';
        }
        return proxy.contextPath?.trim() ? `https://gateway.company.com${proxy.contextPath.trim()}` : '';
    }, [proxy.contextPath, proxy.enableVirtualHosts, proxy.virtualHosts]);

    const gatewayDisplay = gatewayUrl.replace(/^https?:\/\//, '') || 'gateway.company.com/…';
    const upstreamDisplay = proxy.targetUrl?.trim() || 'upstream:port';
    const securityLabel = formatSecurityType(securityType);
    const caption = stepCaption(stepId);

    const ns = computeNodeStates(stepId);

    return (
        <Card className="rounded-xl p-4" role="img" aria-label={`Request path: Client → Gateway → Security → Upstream. ${caption}`}>
            <p className="mb-3 text-center text-xs font-medium text-foreground/90">Request path</p>

            <div className="flex flex-col items-stretch gap-0">
                <FlowNode label="Client" state={ns.client} center />
                <FlowConnector variant={arrowVariant(ns.gateway)} />
                <FlowNode label={gatewayDisplay} state={ns.gateway} mono title={gatewayUrl || undefined} />
                <FlowConnector variant={arrowVariant(ns.security)} />
                <FlowNode label={securityLabel} state={ns.security} title={securityLabel} center />
                <FlowConnector variant={arrowVariant(ns.upstream)} />
                <FlowNode label={upstreamDisplay} state={ns.upstream} mono title={proxy.targetUrl?.trim() || undefined} />
            </div>

            <p className="mt-3 text-center text-[11px] text-muted-foreground">{caption}</p>
        </Card>
    );
}
