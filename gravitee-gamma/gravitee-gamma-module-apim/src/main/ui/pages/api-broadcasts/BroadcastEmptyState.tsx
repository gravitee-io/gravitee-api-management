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
import { ArrowRightIcon, CircleCheckIcon, RadioIcon, UserIcon, UsersIcon } from '@gravitee/graphene-core/icons';
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
    'Announce deprecations and breaking changes to all subscribers',
    'Schedule maintenance notifications in advance',
    'Target broadcasts by plan or subscription status',
] as const;

export function BroadcastEmptyState() {
    return (
        <Card>
            <CardContent className="p-6 space-y-6">
                <div className="space-y-1">
                    <p className="text-sm font-semibold">Why send broadcasts?</p>
                    <p className="text-sm text-muted-foreground">
                        Broadcasts let you communicate directly with API consumers — announce breaking changes, scheduled maintenance
                        windows, or new endpoint availability.
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
                        <FlowNode icon={UserIcon} label="Admin" />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <FlowNode icon={RadioIcon} label="Broadcast" active />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <FlowNode icon={UsersIcon} label="Subscribers" />
                    </div>
                </div>

                <ul className="space-y-2">
                    {BENEFITS.map(b => (
                        <li key={b} className="flex items-start gap-2">
                            <CircleCheckIcon className="size-3.5 shrink-0 mt-0.5 text-success" aria-hidden />
                            <span className="text-xs text-muted-foreground">{b}</span>
                        </li>
                    ))}
                </ul>
            </CardContent>
        </Card>
    );
}
