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
import { ArrowDownIcon } from '@gravitee/graphene-core/icons';

export type FlowNodeState = 'active' | 'muted' | 'complete';
export type FlowArrowVariant = 'muted' | 'emphasized' | 'complete';

const NODE_STATE_CLASSES: Record<FlowNodeState, string> = {
    active: 'border-primary/30 bg-primary/10 text-primary',
    muted: 'border-border bg-muted/50 text-muted-foreground',
    complete: 'border-success/30 bg-success/10 text-success',
};

const CONNECTOR_COLOR_CLASS: Record<FlowArrowVariant, string> = {
    muted: 'text-muted-foreground',
    emphasized: 'text-primary',
    complete: 'text-success',
};

type FlowNodeProps = Readonly<{
    label: string;
    state?: FlowNodeState;
    mono?: boolean;
    center?: boolean;
    title?: string;
}>;

export function FlowNode({ label, state = 'muted', mono = false, center = false, title }: FlowNodeProps) {
    return (
        <div
            className={[
                'w-full rounded-md border px-2 py-1 font-medium leading-snug overflow-hidden',
                mono ? 'font-mono' : '',
                center ? 'text-center' : 'text-left',
                NODE_STATE_CLASSES[state],
            ]
                .filter(Boolean)
                .join(' ')}
            style={{ fontSize: '10px', overflowWrap: 'break-word', wordBreak: 'break-word' }}
            title={title}
        >
            {label}
        </div>
    );
}

type FlowConnectorProps = Readonly<{ variant?: FlowArrowVariant }>;

export function FlowConnector({ variant = 'muted' }: FlowConnectorProps) {
    return (
        <div className={`flex shrink-0 items-center justify-center py-0.5 ${CONNECTOR_COLOR_CLASS[variant]}`} aria-hidden>
            <ArrowDownIcon className="size-4" />
        </div>
    );
}
