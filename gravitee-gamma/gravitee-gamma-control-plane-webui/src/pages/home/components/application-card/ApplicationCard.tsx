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
import { Badge, Card, CardContent, cn } from '@gravitee/graphene-core';
import { ArrowRightIcon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';
import { Link } from 'react-router-dom';

import { COMING_SOON_BADGE, type Application } from './applications';
import { ACCENT_CLASSES } from '../accents';

/**
 * Renders a single app card.
 *
 * Three visual states:
 *  - **Active module** (`to !== null`): full card with "Open →" CTA, wrapped in a
 *    `<Link>` to `/environments/{env}/{moduleId}`. Hover effects.
 *  - **Module not loaded** (`kind: 'module'` but `to === null`): same card minus the
 *    "Open →" CTA. No link wrapper, no hover. The home stays a fixed product showcase
 *    while keeping navigation honest: only modules returned by `GET /…/modules` (the
 *    same source the app switcher consumes) are clickable.
 *  - **Coming soon** (`kind: 'coming-soon'`): card rendered at opacity-60 with a
 *    "Coming soon" badge instead of a count, no "Open →".
 */
export function ApplicationCard({ app, to }: { readonly app: Application; readonly to: string | null }) {
    const { Icon, title, description, accent } = app;
    const comingSoon = app.kind === 'coming-soon';
    const accentClasses = ACCENT_CLASSES[accent];
    const titleId = useId();

    const inner = (
        <CardContent className="pt-5 pb-5 space-y-3">
            <div className="flex items-start justify-between gap-2">
                <div className={cn('rounded-lg p-2', accentClasses.bg)}>
                    <Icon className={cn('size-5', accentClasses.fg)} aria-hidden />
                </div>
                {comingSoon ? (
                    <Badge variant="outline" className="font-normal">
                        {COMING_SOON_BADGE}
                    </Badge>
                ) : (
                    app.badge && (
                        <Badge variant="secondary" className="font-normal">
                            {app.badge}
                        </Badge>
                    )
                )}
            </div>
            <div className="space-y-1">
                <h3 id={titleId} className="text-base font-semibold leading-tight">
                    {title}
                </h3>
                <p className="text-xs text-muted-foreground">{description}</p>
            </div>
            {to !== null && (
                <p className="inline-flex items-center gap-1 text-sm font-medium text-primary">
                    Open
                    <ArrowRightIcon className="size-3.5" aria-hidden />
                </p>
            )}
        </CardContent>
    );

    if (comingSoon || to === null) {
        // Non-clickable variant: announce as a labelled group so screen-reader users get
        // a named region ("Agent Management group") rather than just a styled box, and
        // `aria-disabled` tells them it's not interactive. The `role="group"` + label
        // also gives tests a stable hook (`getByRole('group', { name })`) — review #2.
        return (
            <Card role="group" aria-labelledby={titleId} aria-disabled className={cn(comingSoon && 'opacity-60 cursor-not-allowed')}>
                {inner}
            </Card>
        );
    }

    // Clickable variant: the wrapping <Link> already carries the accessible name from the
    // card title (via inner text), so no explicit role here.
    // `focus-visible:outline-none` strips the browser default; we restore visibility with
    // a ring on the <Link> so keyboard navigation stays usable.
    return (
        <Link
            to={to}
            className="group block rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        >
            <Card className="h-full transition-all group-hover:border-primary/40 group-hover:shadow-sm">{inner}</Card>
        </Link>
    );
}
