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
import { useRef } from 'react';

import { Card, CardContent, cn } from '@gravitee/graphene-core';
import { ArrowRightIcon } from '@gravitee/graphene-core/icons';
import { Link } from 'react-router-dom';

import type { GetStartedStep } from './get-started';
import { ACCENT_CLASSES } from '../accents';

/** "Get Started with Gravitee" entry-point card. */
export function GetStartedCard({ step, to }: { readonly step: GetStartedStep; readonly to: string }) {
    const { Icon, title, description, accent } = step;
    const { bg, fg, hoverColor } = ACCENT_CLASSES[accent];
    const hoverRing = `0 0 0 1px color-mix(in oklab, ${hoverColor} 40%, transparent), 0 4px 16px 0 rgb(0 0 0 / 0.08)`;
    const ctaRef = useRef<HTMLParagraphElement>(null);
    const arrowRef = useRef<SVGSVGElement>(null);

    const onHover = (e: React.MouseEvent<HTMLAnchorElement>, enter: boolean) => {
        const card = e.currentTarget.firstElementChild as HTMLElement | null;
        if (card) card.style.boxShadow = enter ? hoverRing : '';
        if (ctaRef.current) ctaRef.current.style.color = enter ? 'var(--color-foreground)' : '';
        if (arrowRef.current) arrowRef.current.style.transform = enter ? 'translateX(3px)' : '';
    };

    return (
        <Link
            to={to}
            className="cursor-pointer rounded-xl"
            onMouseEnter={e => onHover(e, true)}
            onMouseLeave={e => onHover(e, false)}
        >
            <Card className="h-full" style={{ transition: 'box-shadow 150ms ease' }}>
                <CardContent className="flex h-full flex-col gap-3">
                    <div className={cn('w-fit rounded-lg p-2', bg)}>
                        <Icon className={cn('size-5', fg)} aria-hidden />
                    </div>
                    <div>
                        <h3 className="text-sm font-semibold">{title}</h3>
                        <p className="text-xs text-muted-foreground">{description}</p>
                    </div>
                    <p
                        ref={ctaRef}
                        className="mt-auto flex items-center gap-1 text-xs font-medium text-muted-foreground"
                        style={{ transition: 'color 150ms ease' }}
                    >
                        Get started
                        <ArrowRightIcon
                            ref={arrowRef}
                            className="size-3"
                            aria-hidden
                            style={{ transition: 'transform 150ms ease' }}
                        />
                    </p>
                </CardContent>
            </Card>
        </Link>
    );
}
