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
import { Card, CardContent } from '@gravitee/graphene-core';
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

// ─── Flow diagram (reuses Alerts empty-state pattern) ─────────────────────────

function FlowNode({
    icon: Icon,
    label,
    active = false,
}: Readonly<{ icon: ComponentType<{ className?: string }>; label: string; active?: boolean }>) {
    return (
        <div
            className="flex flex-col items-center gap-1.5"
            style={active ? { border: '1px solid var(--color-border)', borderRadius: '0.5rem', padding: '0.5rem 0.75rem' } : undefined}
        >
            <div
                className="rounded-lg p-2"
                style={{ backgroundColor: active ? 'color-mix(in oklab, var(--color-primary) 10%, transparent)' : 'var(--color-muted)' }}
            >
                <Icon className={active ? 'size-4 text-primary' : 'size-4 text-muted-foreground'} />
            </div>
            <p className={active ? 'text-xs font-semibold text-center' : 'text-xs text-muted-foreground text-center'}>{label}</p>
        </div>
    );
}

// ─── Component ────────────────────────────────────────────────────────────────

const BENEFITS = [
    'Alert on deployments, subscription requests, or policy errors',
    'Route to multiple channels — console, email, or webhooks',
    'Filter by event type to reduce alert fatigue',
] as const;

export function NotificationsEmptyState() {
    return (
        <Card>
            <CardContent className="p-6 space-y-6">
                <div className="space-y-1">
                    <p className="text-sm font-semibold">Why configure notifications?</p>
                    <p className="text-sm text-muted-foreground">
                        Get alerted when key API events happen — deployments, subscription requests, policy violations — without polling the
                        dashboard.
                    </p>
                </div>

                {/* How it works flow */}
                <div
                    className="rounded-xl p-5 space-y-3"
                    style={{
                        border: '2px solid color-mix(in oklab, var(--color-primary) 20%, transparent)',
                        backgroundColor: 'color-mix(in oklab, var(--color-primary) 4%, transparent)',
                    }}
                >
                    <p className="text-xs font-semibold text-primary">How it works</p>
                    <div className="flex items-center justify-center gap-3">
                        <FlowNode icon={ActivityIcon} label="API Event" />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                        <FlowNode icon={BellIcon} label="Notification rule" active />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                        <div className="flex flex-col items-center gap-1.5">
                            <div className="flex gap-1.5">
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
                            <p className="text-xs text-muted-foreground">Email · Console · Webhook</p>
                        </div>
                    </div>
                </div>

                {/* Benefits list */}
                <ul className="space-y-2">
                    {BENEFITS.map(b => (
                        <li key={b} className="flex items-start gap-2">
                            <CircleCheckIcon className="size-3.5 shrink-0 mt-0.5 text-success" aria-hidden="true" />
                            <span className="text-xs text-muted-foreground">{b}</span>
                        </li>
                    ))}
                </ul>
            </CardContent>
        </Card>
    );
}
