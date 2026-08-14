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
import { Card, cn } from '@gravitee/graphene-core';
import {
    ActivityIcon,
    ArrowRightIcon,
    BellIcon,
    CircleCheckIcon,
    MailIcon,
    MessageSquareIcon,
    WebhookIcon,
} from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';

import { ALERT_RULES } from '../constants/alertRules';

const CAPABILITIES = [
    'Send notifications via email, Slack, or webhook',
    'Set severity levels: info, warning, or critical',
    'Configure timeframes for notification windows',
    'Apply dampening rules to avoid alert fatigue',
] as const;

function FlowNode({
    icon: Icon,
    label,
    active = false,
}: Readonly<{ icon: ComponentType<{ className?: string }>; label: string; active?: boolean }>) {
    return (
        <div className={cn('flex flex-col items-center gap-1.5', active && 'rounded-lg border border-border bg-card px-3 py-2')}>
            <div className={cn('rounded-lg p-2', active ? 'bg-primary/10' : 'bg-muted')}>
                <Icon className={cn('size-4', active ? 'text-primary' : 'text-muted-foreground')} />
            </div>
            <p className={cn('text-center text-xs', active ? 'font-semibold' : 'text-muted-foreground')}>{label}</p>
        </div>
    );
}

export function AlertsEducationalEmptyState() {
    return (
        <Card className="space-y-6 p-6">
            <div className="space-y-1">
                <p className="text-sm font-semibold">Why configure alerts?</p>
                <p className="text-sm text-muted-foreground">
                    Get notified automatically when a gateway node or the platform needs attention — health status changes, heartbeat
                    anomalies, health-check failures — without constantly monitoring dashboards.
                </p>
            </div>

            <div className="rounded-xl border-2 border-primary bg-primary/10 p-5">
                <p className="mb-4 text-xs font-semibold text-primary">How it works</p>
                <div className="flex items-center justify-center gap-3">
                    <FlowNode icon={ActivityIcon} label="Platform events" />
                    <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <FlowNode icon={BellIcon} label="Alert rule" active />
                    <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <div className="flex flex-col items-center gap-1.5">
                        <div className="flex gap-1">
                            <div className="rounded-lg bg-muted p-1.5">
                                <MailIcon className="size-3 text-muted-foreground" />
                            </div>
                            <div className="rounded-lg bg-muted p-1.5">
                                <MessageSquareIcon className="size-3 text-muted-foreground" />
                            </div>
                            <div className="rounded-lg bg-muted p-1.5">
                                <WebhookIcon className="size-3 text-muted-foreground" />
                            </div>
                        </div>
                        <p className="text-xs text-muted-foreground">Email · Slack · Webhook</p>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                <div className="space-y-3">
                    <p className="text-xs font-semibold">Available alert rules</p>
                    <ul className="space-y-2.5">
                        {ALERT_RULES.map(rule => (
                            <li key={rule.id} className="flex items-start gap-2">
                                <BellIcon className="mt-0.5 size-3.5 shrink-0 text-primary" aria-hidden="true" />
                                <span className="text-xs text-muted-foreground">
                                    {rule.description} <span className="text-muted-foreground/60">({rule.category})</span>
                                </span>
                            </li>
                        ))}
                    </ul>
                </div>

                <div className="space-y-3">
                    <p className="text-xs font-semibold">Key capabilities</p>
                    <ul className="space-y-2.5">
                        {CAPABILITIES.map(cap => (
                            <li key={cap} className="flex items-start gap-2">
                                <CircleCheckIcon className="mt-0.5 size-3.5 shrink-0 text-success" aria-hidden="true" />
                                <span className="text-xs text-muted-foreground">{cap}</span>
                            </li>
                        ))}
                    </ul>
                </div>
            </div>
        </Card>
    );
}
