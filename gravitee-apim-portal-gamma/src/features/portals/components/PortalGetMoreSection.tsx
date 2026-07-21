/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    ChartLineIcon,
    GlobeIcon,
    UsersIcon,
    XIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';
import { useState } from 'react';

interface GetMoreCardProps {
    readonly Icon: LucideIcon;
    readonly title: string;
    readonly description: string;
}

function GetMoreCard({ Icon, title, description }: GetMoreCardProps) {
    return (
        <Card className="bg-background">
            <CardContent className="flex h-full flex-col gap-3 pt-5 pb-5">
                <div className="w-fit rounded-lg bg-primary/10 p-2">
                    <Icon className="size-5 text-primary" aria-hidden />
                </div>
                <div className="flex-1">
                    <h3 className="text-sm font-semibold">{title}</h3>
                    <p className="mt-1 text-xs leading-relaxed text-muted-foreground">{description}</p>
                </div>
                <button
                    type="button"
                    className="mt-auto flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                >
                    Get started
                    <ArrowRightIcon className="size-3" aria-hidden />
                </button>
            </CardContent>
        </Card>
    );
}

const GET_MORE_CARDS: readonly GetMoreCardProps[] = [
    {
        Icon: GlobeIcon,
        title: 'Set up custom domain',
        description: 'Map your own domain to a portal so developers can find your APIs on your brand.',
    },
    {
        Icon: UsersIcon,
        title: 'Identity Provider',
        description: 'Connect your identity provider to manage user authentication and enforce SSO policies.',
    },
    {
        Icon: ChartLineIcon,
        title: 'Add tracking',
        description: 'Monitor API usage, request volumes, and portal engagement across your catalogs.',
    },
];

export function PortalGetMoreSection() {
    const [dismissed, setDismissed] = useState(false);

    if (dismissed) {
        return null;
    }

    return (
        <section aria-labelledby="get-more-heading" className="relative space-y-4 rounded-xl bg-muted p-6">
            <div className="flex items-start justify-between gap-4">
                <h2 id="get-more-heading" className="text-base font-semibold tracking-tight text-foreground">
                    Get more from Developer Portals
                </h2>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-8 shrink-0"
                    aria-label="Dismiss get more section"
                    onClick={() => setDismissed(true)}
                >
                    <XIcon className="size-4" aria-hidden />
                </Button>
            </div>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                {GET_MORE_CARDS.map(card => (
                    <GetMoreCard key={card.title} {...card} />
                ))}
            </div>
        </section>
    );
}
