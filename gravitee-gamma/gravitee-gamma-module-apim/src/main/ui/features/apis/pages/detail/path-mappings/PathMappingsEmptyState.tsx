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
import { cn, DataTableEmptyState } from '@gravitee/graphene-core';
import { ActivityIcon, ArrowRightIcon, GlobeIcon, ListIcon, RocketIcon, SearchIcon, WaypointsIcon } from '@gravitee/graphene-core/icons';
import type { ComponentType, ReactNode } from 'react';

const FLOW = [
    { label: 'Client request', icon: GlobeIcon },
    { label: 'Request path', icon: ListIcon },
    { label: 'Mapping', icon: WaypointsIcon, active: true },
    { label: 'Analytics', icon: ActivityIcon },
] as const;

const POINTS = [
    {
        title: 'Grouped analytics',
        detail: 'Aggregate requests that share a path pattern instead of listing every URL.',
        icon: ActivityIcon,
    },
    {
        title: 'Pattern lookup',
        detail: 'Match a real request, such as /orders/42, with a template such as /orders/:orderId.',
        icon: SearchIcon,
    },
    {
        title: 'Applies on deploy',
        detail: 'The gateway uses the list after you deploy. Saving here does not change traffic yet.',
        icon: RocketIcon,
    },
] as const;

export function PathMappingsEmptyState({ action }: Readonly<{ action?: ReactNode }>) {
    return (
        <div className="rounded-lg border">
            <DataTableEmptyState
                variant="first-use"
                title="Why add a path mapping?"
                description="Path mappings group analytics by path. A mapping such as /orders/:orderId groups every request that matches it, and applies on the next deploy."
                primaryAction={action}
            >
                <div className="space-y-4">
                    <div className="rounded-xl border-2 border-primary bg-primary/10 p-5">
                        <p className="mb-4 text-xs font-semibold text-primary">How it works</p>
                        <div className="flex flex-wrap items-center justify-center gap-3">
                            {FLOW.map((step, index) => (
                                <FlowStep key={step.label} step={step} showArrow={index < FLOW.length - 1} />
                            ))}
                        </div>
                    </div>
                    <div className="grid gap-3 sm:grid-cols-3">
                        {POINTS.map(point => {
                            const Icon = point.icon;
                            return (
                                <div key={point.title} className="rounded-lg border border-border p-3 text-left">
                                    <Icon className="size-4 text-primary" aria-hidden />
                                    <p className="mt-2 text-sm font-medium">{point.title}</p>
                                    <p className="mt-1 text-xs text-muted-foreground">{point.detail}</p>
                                </div>
                            );
                        })}
                    </div>
                </div>
            </DataTableEmptyState>
        </div>
    );
}

function FlowStep({
    step,
    showArrow,
}: Readonly<{ step: { label: string; icon: ComponentType<{ className?: string }>; active?: boolean }; showArrow: boolean }>) {
    const Icon = step.icon;
    return (
        <>
            <div
                className={cn(
                    'flex flex-col items-center gap-1.5 text-center',
                    step.active && 'rounded-lg border border-border bg-card px-3 py-2',
                )}
            >
                <div className={cn('rounded-lg p-2', step.active ? 'bg-primary/10' : 'bg-muted')}>
                    <Icon className={cn('size-4', step.active ? 'text-primary' : 'text-muted-foreground')} aria-hidden />
                </div>
                <p className={cn('text-xs', step.active ? 'font-semibold' : 'text-muted-foreground')}>{step.label}</p>
            </div>
            {showArrow ? <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden /> : null}
        </>
    );
}
