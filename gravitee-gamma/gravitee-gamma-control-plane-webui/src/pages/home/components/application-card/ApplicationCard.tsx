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

import { Badge, Card, CardContent } from '@gravitee/graphene-core';
import { ArrowRightIcon } from '@gravitee/graphene-core/icons';
import { useId, useState } from 'react';
import { Link } from 'react-router-dom';

import { type Application } from './applications';

/**
 * Two visual states:
 *  - **Active module** (`to !== null`): clickable card with hover ring, elevation and animated CTA.
 *  - **Module not loaded** (`to === null`): same Card, no hover, no CTA.
 */
export function ApplicationCard({ app, to }: { readonly app: Application; readonly to: string | null }) {
    const { Icon, title, description } = app;
    const titleId = useId();
    const hoverRing = `0 0 0 1px color-mix(in oklab, var(--color-muted-foreground) 40%, transparent), 0 4px 16px 0 rgb(0 0 0 / 0.08)`;
    const [isHovered, setIsHovered] = useState(false);

    const inner = (
        <CardContent className="flex h-full flex-col gap-3">
            <div className="flex items-start justify-between gap-2">
                <div className="rounded-lg p-2 bg-accent/20">
                    <Icon className="size-12" aria-hidden />
                </div>
                {app.badge && (
                    <Badge variant="secondary" className="font-normal">
                        {app.badge}
                    </Badge>
                )}
            </div>
            <div className="space-y-1">
                <h3 id={titleId} className="text-sm font-semibold leading-tight">
                    {title}
                </h3>
                <p className="text-xs text-muted-foreground">{description}</p>
            </div>
            {to !== null && (
                <p
                    className="mt-auto flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors duration-150"
                    style={{ color: isHovered ? 'var(--color-foreground)' : undefined }}
                >
                    Open
                    <ArrowRightIcon
                        className="size-3 transition-transform duration-150"
                        aria-hidden
                        style={{ transform: isHovered ? 'translateX(3px)' : undefined }}
                    />
                </p>
            )}
        </CardContent>
    );

    if (to === null) {
        return (
            <Card role="group" aria-labelledby={titleId} aria-disabled className="h-full">
                {inner}
            </Card>
        );
    }

    return (
        <Link
            to={to}
            className="cursor-pointer rounded-xl"
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
        >
            <Card className="h-full transition-shadow duration-150" style={{ boxShadow: isHovered ? hoverRing : undefined }}>
                {inner}
            </Card>
        </Link>
    );
}
