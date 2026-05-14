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
import { ArrowRightIcon, CircleCheckIcon, CircleXIcon, KeyIcon, MonitorIcon, ShieldIcon, UsersIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import { ApplicationsPageHeader } from './ApplicationsPageHeader';
import { FeatureTile } from '../../shared/components';

const WITHOUT_APP_CONS = [
    'No consumer identity or subscription lifecycle',
    'No centralized API keys or OAuth client management',
    'Harder to track which clients call which APIs',
] as const;

const WITH_APP_PROS = [
    'Dedicated credentials per consumer application',
    'Subscriptions tied to plans, keys, and quotas',
    'Clear ownership and access across environments',
] as const;

const FEATURE_TILES: { readonly Icon: LucideIcon; readonly title: string; readonly description: string }[] = [
    {
        Icon: UsersIcon,
        title: 'Consumer identity',
        description: 'Register each client app so subscriptions, owners, and credentials stay organized.',
    },
    {
        Icon: KeyIcon,
        title: 'Access management',
        description: 'Issue API keys or OAuth clients and control how consumers authenticate.',
    },
    {
        Icon: ShieldIcon,
        title: 'Controlled access',
        description: 'Approve subscriptions to plans and revoke access without touching backend services.',
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

export function ApplicationsEmptyLanding({
    onRegisterApplication,
    canCreate = false,
}: {
    onRegisterApplication?: () => void;
    canCreate?: boolean;
}) {
    return (
        <div className="space-y-6">
            <ApplicationsPageHeader canCreate={canCreate} onRegisterApplication={onRegisterApplication ?? (() => undefined)} />

            <Card>
                <CardContent className="pt-6 space-y-6">
                    <div>
                        <h2 className="text-base font-semibold">Why register an application?</h2>
                        <p className="text-xs text-muted-foreground mt-1">
                            Applications represent the consumers of your APIs. Registering them lets you manage subscriptions, credentials,
                            and ownership in one place.
                        </p>
                    </div>

                    <div className="flex flex-row gap-4 items-stretch">
                        <div className="flex-1 rounded-xl border p-4 space-y-3">
                            <p className="text-xs font-semibold text-muted-foreground">Without registered applications</p>
                            <div className="flex items-center justify-center gap-2">
                                <FlowNode Icon={MonitorIcon} label="Client" />
                                <ArrowRightIcon className="size-4 text-muted-foreground shrink-0" aria-hidden />
                                <FlowNode Icon={ShieldIcon} label="API" />
                            </div>
                            <ul className="space-y-1">
                                {WITHOUT_APP_CONS.map(label => (
                                    <ComparisonLine key={label} label={label} variant="negative" />
                                ))}
                            </ul>
                        </div>

                        <div className="flex items-center justify-center shrink-0">
                            <ArrowRightIcon className="size-5 text-primary" aria-hidden />
                        </div>

                        <div className="flex-1 rounded-xl border-2 border-primary/20 bg-primary/5 p-4 space-y-3">
                            <p className="text-xs font-semibold text-primary">With registered applications</p>
                            <div className="flex items-center justify-center gap-2">
                                <FlowNode Icon={MonitorIcon} label="Client" />
                                <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                                <FlowNode Icon={UsersIcon} label="Application" />
                                <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                                <FlowNode Icon={ShieldIcon} label="API" />
                            </div>
                            <ul className="space-y-1">
                                {WITH_APP_PROS.map(label => (
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
        </div>
    );
}
