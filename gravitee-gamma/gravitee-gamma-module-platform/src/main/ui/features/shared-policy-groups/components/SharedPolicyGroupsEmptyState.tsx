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
import { ArrowRightIcon, CircleCheckIcon, ClockIcon, LayersIcon, PlugIcon, WorkflowIcon } from '@gravitee/graphene-core/icons';
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
            <p className={active ? 'text-xs font-semibold text-center' : 'text-xs text-muted-foreground text-center'}>{label}</p>
        </div>
    );
}

const BENEFITS = [
    'Reuse the same policy logic — auth, rate limiting, transformations — across as many APIs as you like',
    'Update the shared logic in one place; every API referencing it picks it up on the next deploy',
    'Deploy, undeploy, and roll back the group independently from the APIs that reference it',
] as const;

/**
 * Educational content for the Shared Policy Groups first-use empty state — rendered as
 * `DataTableEmptyState`'s `children` (see SharedPolicyGroupsPage), between the description and actions.
 */
export function SharedPolicyGroupsEmptyState() {
    return (
        <div className="w-full space-y-6 text-left">
            <div className="rounded-xl border-2 border-primary/20 bg-primary/[0.04] p-5 space-y-3">
                <p className="text-xs font-semibold text-primary">How it works</p>
                <div className="flex items-center justify-center gap-3">
                    <FlowNode icon={WorkflowIcon} label="Policy steps" />
                    <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <FlowNode icon={LayersIcon} label="Shared Policy Group" active />
                    <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <FlowNode icon={PlugIcon} label="Attached to API flows" />
                </div>
            </div>

            <ul className="space-y-2">
                {BENEFITS.map(b => (
                    <li key={b} className="flex items-start gap-2">
                        <CircleCheckIcon className="size-3.5 shrink-0 mt-0.5 text-success" aria-hidden="true" />
                        <span className="text-xs text-muted-foreground">{b}</span>
                    </li>
                ))}
            </ul>

            <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <ClockIcon className="size-3.5 shrink-0" aria-hidden="true" />
                <span>Every change is versioned — view a group&apos;s history and roll back to a previous revision at any time.</span>
            </div>
        </div>
    );
}
