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
import { cn } from '@gravitee/graphene-core';

import { useApiCreation } from '../../store/apiCreationStore';
import { buildPreviewGatewayUrl } from '../../utils/apiProxyMapper';
import { AUTH_LABEL } from '../../utils/securityFormatters';

type NodeState = 'active' | 'muted' | 'complete';
type ArrowVariant = 'muted' | 'emphasized' | 'complete';

const NODE_CLASSES: Record<NodeState, string> = {
    active: 'border-primary text-primary bg-primary/5',
    muted: 'border-border text-muted-foreground bg-muted/30',
    complete: 'border-success text-success bg-success/5',
};

function arrowColor(variant: ArrowVariant): string {
    if (variant === 'complete') return 'var(--color-success)';
    if (variant === 'emphasized') return 'var(--color-primary)';
    return 'var(--color-muted-foreground)';
}

function arrowOpacity(variant: ArrowVariant): number {
    if (variant === 'muted') return 0.3;
    if (variant === 'complete') return 1;
    return 0.5;
}

function DNode({ label, state, mono, centered }: { label: string; state: NodeState; mono?: boolean; centered?: boolean }) {
    return (
        <div
            className={cn('rounded-md border px-2 py-1.5 text-xs font-medium', NODE_CLASSES[state])}
            style={{
                fontFamily: mono ? 'monospace' : undefined,
                wordBreak: mono ? 'break-all' : undefined,
                overflowWrap: mono ? 'break-word' : undefined,
                whiteSpace: 'normal',
                lineHeight: '1.4',
                textAlign: centered ? 'center' : undefined,
            }}
        >
            {label}
        </div>
    );
}

function DArrowDown({ variant }: { variant: ArrowVariant }) {
    const color = arrowColor(variant);
    const opacity = arrowOpacity(variant);
    return (
        <div className="flex flex-col items-center self-center py-0.5" aria-hidden>
            <div style={{ width: '1px', height: '0.75rem', backgroundColor: color, opacity }} />
            <span style={{ fontSize: '8px', lineHeight: 1, color, opacity }}>▼</span>
        </div>
    );
}

function nodeState(which: 'client' | 'gateway' | 'security' | 'upstream', mode: 'scratch' | 'template', step: number): NodeState {
    const complete = (mode === 'scratch' && step === 3) || (mode === 'template' && step === 1);
    if (complete) return 'complete';

    if (mode === 'scratch') {
        if (step === 0) return 'muted';
        if (step === 1) return which === 'gateway' || which === 'upstream' ? 'active' : 'muted';
        if (step === 2) return which === 'security' ? 'active' : 'muted';
    }
    if (mode === 'template' && step === 0) {
        return which === 'client' ? 'muted' : 'active';
    }
    return 'muted';
}

function toArrowVariant(state: NodeState): ArrowVariant {
    if (state === 'complete') return 'complete';
    if (state === 'active') return 'emphasized';
    return 'muted';
}

function stepCaption(mode: 'scratch' | 'template', step: number): string {
    if (mode === 'scratch') {
        switch (step) {
            case 0:
                return 'Defining API identity';
            case 1:
                return 'Configuring entrypoints & upstream';
            case 2:
                return 'Configuring security & access';
            case 3:
                return 'Ready to review & deploy';
            default:
                return '';
        }
    }
    if (step === 0) return 'Filling in essentials';
    return 'Ready to review & deploy';
}

interface ProxyFlowVisualizationProps {
    mode: 'scratch' | 'template';
}

export function ProxyFlowVisualization({ mode }: ProxyFlowVisualizationProps) {
    const { state } = useApiCreation();
    const { form, step } = state;

    const client = nodeState('client', mode, step);
    const gateway = nodeState('gateway', mode, step);
    const security = nodeState('security', mode, step);
    const upstream = nodeState('upstream', mode, step);

    // Strip protocol prefix — URL is already clear from context
    const gatewayDisplay = buildPreviewGatewayUrl(form).replace(/^https?:\/\//, '');
    const upstreamDisplay = form.targetUrl.trim() || 'upstream:port';
    const securityLabel = AUTH_LABEL[form.authType];
    const caption = stepCaption(mode, step);

    return (
        <div
            className="rounded-xl border bg-card p-4"
            role="img"
            aria-label={`Request path: Client → ${gatewayDisplay} → ${securityLabel} → ${upstreamDisplay}. ${caption}`}
        >
            <p className="mb-3 text-center text-xs font-medium" style={{ color: 'var(--color-foreground)', opacity: 0.9 }}>
                Request path
            </p>

            <div className="flex flex-col items-stretch">
                <DNode label="Client" state={client} centered />
                <DArrowDown variant={toArrowVariant(gateway)} />
                <DNode label={gatewayDisplay} state={gateway} mono />
                <DArrowDown variant={toArrowVariant(security)} />
                <DNode label={securityLabel} state={security} centered />
                <DArrowDown variant={toArrowVariant(upstream)} />
                <DNode label={upstreamDisplay} state={upstream} mono />
            </div>

            {caption && (
                <p className="mt-3 text-center text-muted-foreground" style={{ fontSize: '11px' }}>
                    {caption}
                </p>
            )}
        </div>
    );
}
