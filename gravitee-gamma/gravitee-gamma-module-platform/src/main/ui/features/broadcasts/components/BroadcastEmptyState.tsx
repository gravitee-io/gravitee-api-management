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
            className={
                active
                    ? 'flex flex-col items-center gap-1.5 rounded-lg border border-border px-3 py-2'
                    : 'flex flex-col items-center gap-1.5'
            }
        >
            <div className={active ? 'rounded-lg bg-primary/10 p-2' : 'rounded-lg bg-muted p-2'}>
                <Icon className={active ? 'size-4 text-primary' : 'size-4 text-muted-foreground'} />
            </div>
            <p className={active ? 'text-center text-xs font-semibold' : 'text-center text-xs text-muted-foreground'}>{label}</p>
        </div>
    );
}

const BENEFITS = [
    'Announce environment-wide maintenance and policy changes',
    'Email or notify members by their environment role',
    'POST the same message to an HTTP webhook when you need an integration',
] as const;

export function BroadcastEmptyState() {
    return (
        <Card>
            <CardContent className="space-y-6 p-6">
                <div className="space-y-1">
                    <p className="text-sm font-semibold">Why send broadcasts?</p>
                    <p className="text-sm text-muted-foreground">
                        Broadcasts let you reach everyone in this environment — API publishers, users, and admins — with portal, email, or
                        HTTP notifications.
                    </p>
                </div>

                <div className="space-y-3 rounded-xl border-2 border-primary/20 bg-primary/5 p-5">
                    <p className="text-xs font-semibold text-primary">How it works</p>
                    <div className="flex items-center justify-center gap-3">
                        <FlowNode icon={UserIcon} label="Admin" />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <FlowNode icon={RadioIcon} label="Broadcast" active />
                        <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                        <FlowNode icon={UsersIcon} label="Members" />
                    </div>
                </div>

                <ul className="space-y-2">
                    {BENEFITS.map(benefit => (
                        <li key={benefit} className="flex items-start gap-2">
                            <CircleCheckIcon className="mt-0.5 size-3.5 shrink-0 text-success" aria-hidden />
                            <span className="text-xs text-muted-foreground">{benefit}</span>
                        </li>
                    ))}
                </ul>
            </CardContent>
        </Card>
    );
}
