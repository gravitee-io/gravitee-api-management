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
    AppWindowIcon,
    ArrowRightIcon,
    CircleCheckIcon,
    CircleXIcon,
    LockIcon,
    ShieldIcon,
    UsersIcon,
} from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import { FeatureTile } from '../features/shared/components';

const WITHOUT_ACCESS = [
    'These menus stay empty in this environment',
    'Applications, groups, and organization settings stay out of reach',
    'Pasting a URL cannot open a page you are not allowed to see',
] as const;

const WITH_ACCESS = [
    'An administrator assigns an environment or organization role',
    'Matching menus appear in the sidebar',
    'You can open the pages your role actually grants',
] as const;

const FEATURE_TILES: { readonly Icon: LucideIcon; readonly title: string; readonly description: string }[] = [
    {
        Icon: UsersIcon,
        title: 'Ask your administrator',
        description: 'Tell them which environment you need (for example Dev or Test) and what you are trying to do.',
    },
    {
        Icon: AppWindowIcon,
        title: 'Typical USER access',
        description: 'ENVIRONMENT:USER opens Applications, Shared Policy Groups, and Groups.',
    },
    {
        Icon: ShieldIcon,
        title: 'Typical ADMIN access',
        description: 'ENVIRONMENT:ADMIN adds gateways, alerts, and settings. ORGANIZATION:ADMIN opens organization configuration.',
    },
];

function FlowNode({ Icon, label }: { readonly Icon: LucideIcon; readonly label: string }) {
    return (
        <div className="flex flex-col items-center text-center">
            <div className="rounded-lg bg-muted p-2">
                <Icon className="size-4 text-muted-foreground" aria-hidden />
            </div>
            <p className="text-xs font-medium mt-1">{label}</p>
        </div>
    );
}

function ComparisonLine({ label, variant }: { readonly label: string; readonly variant: 'positive' | 'negative' }) {
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

export function PlatformNoAccessPage() {
    return (
        <div className="space-y-6" data-testid="platform-no-access-page">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">{"You don't have access here"}</h1>
                <p className="text-sm text-muted-foreground">Ask your administrator to assign an environment or organization role.</p>
            </div>

            <Card>
                <CardContent className="pt-6 space-y-6">
                    <div>
                        <h2 className="text-base font-semibold">Why these menus are empty</h2>
                        <p className="text-xs text-muted-foreground mt-1">
                            Platform Management uses environment and organization roles. Your current role does not unlock any of those
                            menus, so there is nothing to open here.
                        </p>
                    </div>

                    <div className="flex flex-col gap-4 items-stretch md:flex-row">
                        <div className="flex-1 rounded-xl border p-4 space-y-3">
                            <p className="text-xs font-semibold text-muted-foreground">Without an environment or organization role</p>
                            <div className="flex items-center justify-center gap-2">
                                <FlowNode Icon={LockIcon} label="Your account" />
                                <ArrowRightIcon className="size-4 text-muted-foreground shrink-0" aria-hidden />
                                <FlowNode Icon={AppWindowIcon} label="No menus" />
                            </div>
                            <ul className="space-y-1">
                                {WITHOUT_ACCESS.map(label => (
                                    <ComparisonLine key={label} label={label} variant="negative" />
                                ))}
                            </ul>
                        </div>

                        <div className="flex-1 rounded-xl border-2 border-primary/40 bg-primary/5 p-4 space-y-3">
                            <p className="text-xs font-semibold text-primary">With an environment or organization role</p>
                            <div className="flex items-center justify-center gap-2">
                                <FlowNode Icon={UsersIcon} label="Administrator" />
                                <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                                <FlowNode Icon={ShieldIcon} label="Role assigned" />
                                <ArrowRightIcon className="size-4 text-primary/70 shrink-0" aria-hidden />
                                <FlowNode Icon={AppWindowIcon} label="Menus appear" />
                            </div>
                            <ul className="space-y-1">
                                {WITH_ACCESS.map(label => (
                                    <ComparisonLine key={label} label={label} variant="positive" />
                                ))}
                            </ul>
                        </div>
                    </div>

                    <div className="flex flex-col gap-4 border-t pt-5 md:flex-row">
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
