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
import { ArrowRightIcon, CircleCheckIcon, ListIcon, ScrollTextIcon, ShieldCheckIcon, SparklesIcon } from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';

const CAPABILITIES = [
    'Evaluate this API on demand against your environment rulesets',
    'Filter findings by severity — errors, warnings, infos, and hints',
    'Inspect line, column, and JSON path for each recommendation',
] as const;

function FlowNode({
    icon: Icon,
    label,
    active = false,
}: Readonly<{ icon: ComponentType<{ className?: string }>; label: string; active?: boolean }>) {
    return (
        <div className={cn('flex flex-col items-center gap-1.5', active && 'rounded-lg border border-border bg-card px-3 py-2')}>
            <div className={cn('rounded-lg p-2', active ? 'bg-primary/10' : 'bg-muted')}>
                <Icon className={cn('size-4', active ? 'text-primary' : 'text-muted-foreground')} aria-hidden />
            </div>
            <p className={cn('max-w-[7rem] text-center text-xs', active ? 'font-semibold' : 'text-muted-foreground')}>{label}</p>
        </div>
    );
}

/** First-use education for API-level scoring — matches Gamma “How it works” empty states (Alerts, health check, …). */
export function ApiScoringEducationalEmptyState() {
    return (
        <Card className="space-y-6 p-6" data-testid="api-scoring-educational-empty">
            <div className="space-y-1">
                <p className="text-sm font-semibold">Why run API Score?</p>
                <p className="text-sm text-muted-foreground">
                    Check this API against your organization&apos;s rulesets before publish or review. You get a score plus actionable
                    recommendations on OpenAPI and Gravitee definition assets.
                </p>
            </div>

            <div className="rounded-xl border-2 border-primary bg-primary/10 p-5">
                <p className="mb-4 text-xs font-semibold text-primary">How it works</p>
                <div className="flex flex-wrap items-center justify-center gap-3">
                    <FlowNode icon={ScrollTextIcon} label="API assets" />
                    <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                    <FlowNode icon={ListIcon} label="Rulesets" />
                    <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                    <FlowNode icon={ShieldCheckIcon} label="Score & findings" active />
                    <ArrowRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                    <FlowNode icon={SparklesIcon} label="Fix & re-evaluate" />
                </div>
            </div>

            <div className="space-y-3">
                <p className="text-xs font-semibold">Key capabilities</p>
                <ul className="space-y-2.5">
                    {CAPABILITIES.map(cap => (
                        <li key={cap} className="flex items-start gap-2">
                            <CircleCheckIcon className="mt-0.5 size-3.5 shrink-0 text-success" aria-hidden />
                            <span className="text-xs text-muted-foreground">{cap}</span>
                        </li>
                    ))}
                </ul>
            </div>

            <p className="text-sm text-muted-foreground">
                Click <span className="font-medium text-foreground">Evaluate</span> above to generate the first score for this API.
            </p>
        </Card>
    );
}
