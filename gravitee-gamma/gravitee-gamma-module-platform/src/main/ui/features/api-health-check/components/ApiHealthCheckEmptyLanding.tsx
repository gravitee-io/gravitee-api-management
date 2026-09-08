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
import { ActivityIcon, ArrowRightIcon, CircleCheckIcon, CircleXIcon, RadioIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import { FeatureTile } from '../../shared/components';

const WITHOUT_HEALTH_CHECK = [
    'Backend outages only show up in consumer tickets',
    'You cannot tell which APIs still answer a health probe',
    'There is no environment-wide view of availability over the last minute, hour, or week',
] as const;

const WITH_HEALTH_CHECK = [
    'Filter to APIs that already have health check enabled',
    'See availability for the selected timeframe, with error (≤ 80%) and warning (≤ 95%) counts',
    'Open an API to inspect failed checks and response-time trends',
] as const;

const FEATURE_TILES: { readonly Icon: LucideIcon; readonly title: string; readonly description: string }[] = [
    {
        Icon: RadioIcon,
        title: 'Probe the backend',
        description:
            'Each API with health check enabled is polled by a periodic HTTP request. The gateway decides if the response matches the expected assertion.',
    },
    {
        Icon: CircleCheckIcon,
        title: 'Read availability',
        description:
            'Availability is the share of successful checks in the selected window. At or below 80% is error; at or below 95% is warning.',
    },
    {
        Icon: ActivityIcon,
        title: 'Open the API dashboard',
        description: "From a row with health check enabled, open that API's Health Check Dashboard for response times and failed checks.",
    },
];

function FlowNode({ Icon, label }: { Icon: LucideIcon; label: string }) {
    return (
        <div className="flex flex-col items-center text-center">
            <div className="rounded-lg bg-muted p-2">
                <Icon className="size-4 text-muted-foreground" aria-hidden />
            </div>
            <p className="text-xs font-medium mt-1">{label}</p>
        </div>
    );
}

function ComparisonLine({ label, variant }: { label: string; variant: 'positive' | 'negative' }) {
    return (
        <li className="flex items-center gap-1 text-xs text-muted-foreground">
            {variant === 'positive' ? (
                <CircleCheckIcon className="size-3 shrink-0 text-success" aria-hidden />
            ) : (
                <CircleXIcon className="size-3 shrink-0 text-destructive" aria-hidden />
            )}
            {label}
        </li>
    );
}

export function ApiHealthCheckEmptyLanding() {
    return (
        <Card>
            <CardContent className="pt-6 space-y-6">
                <div>
                    <h2 className="text-base font-semibold">Watch API backends before consumers feel an outage</h2>
                    <p className="text-xs text-muted-foreground mt-1">
                        API Health Check sends a periodic HTTP request to each configured health-check endpoint. The backend responds, and
                        Gravitee records whether that response is healthy — so you can see which APIs are operational, in warning, or in
                        error across this environment.
                    </p>
                </div>

                <div className="flex flex-row gap-4 items-stretch">
                    <div className="flex-1 rounded-xl border p-4 space-y-3">
                        <p className="text-xs font-semibold text-muted-foreground">Without health check</p>
                        <div className="flex items-center justify-center gap-2">
                            <FlowNode Icon={RadioIcon} label="Backend" />
                            <ArrowRightIcon className="size-4 text-muted-foreground shrink-0" aria-hidden />
                            <FlowNode Icon={ActivityIcon} label="Consumers" />
                        </div>
                        <ul className="space-y-1">
                            {WITHOUT_HEALTH_CHECK.map(label => (
                                <ComparisonLine key={label} label={label} variant="negative" />
                            ))}
                        </ul>
                    </div>

                    <div className="flex items-center justify-center shrink-0">
                        <ArrowRightIcon className="size-5 text-primary" aria-hidden />
                    </div>

                    <div className="flex-1 rounded-xl border-2 border-primary/20 bg-primary/5 p-4 space-y-3">
                        <p className="text-xs font-semibold text-primary">With health check</p>
                        <div className="flex items-center justify-center gap-2">
                            <FlowNode Icon={RadioIcon} label="Backend" />
                            <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                            <FlowNode Icon={CircleCheckIcon} label="Health check" />
                            <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                            <FlowNode Icon={ActivityIcon} label="Dashboard" />
                        </div>
                        <ul className="space-y-1">
                            {WITH_HEALTH_CHECK.map(label => (
                                <ComparisonLine key={label} label={label} variant="positive" />
                            ))}
                        </ul>
                    </div>
                </div>

                <div className="flex flex-row gap-4 border-t pt-5">
                    {FEATURE_TILES.map(({ Icon, title, description }) => (
                        <div key={title} className="flex-1">
                            <FeatureTile Icon={Icon} title={title} description={description} />
                        </div>
                    ))}
                </div>
            </CardContent>
        </Card>
    );
}
