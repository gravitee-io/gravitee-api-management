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
import { Button, Card } from '@gravitee/graphene-core';
import { ArrowRightIcon, CircleCheckIcon, PencilIcon, RocketIcon, ScrollTextIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';
import type * as React from 'react';

const DOCS_URL = 'https://documentation.gravitee.io/apim/guides/api-configuration/audit-logs';

const FEATURES = [
    'Complete change history for compliance and governance',
    'Track who modified configurations and when',
    'Filterable by event type, user, and date range',
] as const;

type AuditLogsLandingProps = Readonly<{ onViewSample?: () => void }>;

export function AuditLogsLanding({ onViewSample }: AuditLogsLandingProps) {
    return (
        <div className="space-y-8">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Audit Logs</h1>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Track every configuration change, deployment, and subscription decision.
                    </p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                    <Button type="button" variant="outline" size="sm" asChild>
                        <a href={DOCS_URL} target="_blank" rel="noopener noreferrer">
                            <ScrollTextIcon className="size-4" aria-hidden="true" />
                            Documentation
                        </a>
                    </Button>
                    {onViewSample ? (
                        <Button type="button" size="sm" onClick={onViewSample}>
                            <RocketIcon className="size-4" aria-hidden="true" />
                            View sample audit trail
                        </Button>
                    ) : null}
                </div>
            </div>

            <Card className="rounded-xl p-6">
                <h3 className="text-base font-semibold">Why track audit logs?</h3>
                <p className="mt-2 text-sm text-muted-foreground">
                    Audit logs record every configuration change — who changed what, when, and from where — giving you a compliance-ready
                    trail for governance and troubleshooting.
                </p>

                <div className="mt-6 rounded-xl border border-primary/20 bg-primary/5 p-5">
                    <p className="mb-5 text-xs font-semibold text-primary">How it works</p>
                    <div className="flex items-start justify-center gap-3">
                        <FlowStep icon={PencilIcon} label="Config change" variant="muted" />
                        <ArrowRightIcon className="mt-3 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                        <FlowStep icon={ScrollTextIcon} label="Audit trail" variant="active" />
                        <ArrowRightIcon className="mt-3 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                        <FlowStep icon={ShieldCheckIcon} label="Who / What / When" variant="muted" />
                    </div>
                </div>

                <ul className="mt-6 space-y-2">
                    {FEATURES.map(feature => (
                        <li key={feature} className="flex items-center gap-2 text-sm text-muted-foreground">
                            <CircleCheckIcon className="size-4 shrink-0 text-green-600" aria-hidden="true" />
                            {feature}
                        </li>
                    ))}
                </ul>
            </Card>
        </div>
    );
}

function FlowStep({
    icon: Icon,
    label,
    variant,
}: Readonly<{
    icon: React.ComponentType<React.SVGProps<SVGSVGElement>>;
    label: string;
    variant: 'active' | 'muted';
}>) {
    const isActive = variant === 'active';
    return (
        <div className="flex flex-col items-center gap-2">
            <div
                className={`flex size-10 shrink-0 items-center justify-center rounded-full ${
                    isActive ? 'border-2 border-primary bg-primary/10' : 'border border-border bg-muted'
                }`}
            >
                <Icon className={`size-5 ${isActive ? 'text-primary' : 'text-muted-foreground'}`} aria-hidden="true" />
            </div>
            <span className={`text-center text-xs font-medium ${isActive ? 'text-primary' : 'text-muted-foreground'}`}>{label}</span>
        </div>
    );
}
