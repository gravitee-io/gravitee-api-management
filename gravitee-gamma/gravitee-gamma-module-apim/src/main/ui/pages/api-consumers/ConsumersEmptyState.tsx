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
import { ArrowRightIcon, CircleCheckIcon, CreditCardIcon, KeyRoundIcon, MonitorIcon, UsersRoundIcon } from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';

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

const BENEFITS = [
    { icon: UsersRoundIcon, text: 'Track which applications are consuming your API' },
    { icon: KeyRoundIcon, text: 'Manage API keys — issue, revoke, and rotate per consumer' },
    { icon: CircleCheckIcon, text: 'Monitor per-consumer request volume and error rates' },
] as const;

export function ConsumersEmptyState() {
    return (
        <Card>
            <CardContent className="p-6 space-y-6">
                <div className="space-y-1">
                    <p className="text-sm font-semibold">Why manage consumers?</p>
                    <p className="text-sm text-muted-foreground">
                        Consumers are the applications subscribed to your API through plans. Track who&apos;s calling your API, manage their
                        API keys, and monitor per-consumer usage and health.
                    </p>
                </div>

                <div
                    className="rounded-xl p-5 space-y-3"
                    style={{
                        border: '2px solid color-mix(in oklab, var(--color-primary) 20%, transparent)',
                        backgroundColor: 'color-mix(in oklab, var(--color-primary) 4%, transparent)',
                    }}
                >
                    <p className="text-xs font-semibold text-primary">How it works</p>
                    <div className="flex items-center justify-center gap-3">
                        <FlowNode icon={MonitorIcon} label="Application" />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <FlowNode icon={CreditCardIcon} label="Subscription" active />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <FlowNode icon={UsersRoundIcon} label="API Proxy" />
                    </div>
                </div>

                <ul className="space-y-2">
                    {BENEFITS.map(({ text }) => (
                        <li key={text} className="flex items-start gap-2">
                            <CircleCheckIcon className="size-3.5 shrink-0 mt-0.5 text-success" aria-hidden />
                            <span className="text-xs text-muted-foreground">{text}</span>
                        </li>
                    ))}
                </ul>
            </CardContent>
        </Card>
    );
}
