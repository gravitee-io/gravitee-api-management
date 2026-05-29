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

import { Card, CardContent, cn } from '@gravitee/graphene-core';
import { ArrowRightIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link } from 'react-router-dom';

import type { GetStartedStep } from './get-started';
import { ACCENT_CLASSES } from '../accents';

/** "Get Started with Gravitee" entry-point card. */
export function GetStartedCard({ step, to }: { readonly step: GetStartedStep; readonly to: string }) {
    const { Icon, title, description, accent } = step;
    const { bg, fg, hoverColor } = ACCENT_CLASSES[accent];
    const hoverRing = `0 0 0 1px color-mix(in oklab, ${hoverColor} 40%, transparent), 0 4px 16px 0 rgb(0 0 0 / 0.08)`;
    const [isHovered, setIsHovered] = useState(false);

    return (
        <Link
            to={to}
            className="cursor-pointer rounded-xl"
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
        >
            <Card className="h-full transition-shadow duration-150" style={{ boxShadow: isHovered ? hoverRing : undefined }}>
                <CardContent className="flex h-full flex-col gap-3">
                    <div className={cn('w-fit rounded-lg p-2', bg)}>
                        <Icon className={cn('size-5', fg)} aria-hidden />
                    </div>
                    <div>
                        <h3 className="text-sm font-semibold">{title}</h3>
                        <p className="text-xs text-muted-foreground">{description}</p>
                    </div>
                    <p
                        className="mt-auto flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors duration-150"
                        style={{ color: isHovered ? 'var(--color-foreground)' : undefined }}
                    >
                        Get started
                        <ArrowRightIcon
                            className="size-3 transition-transform duration-150"
                            aria-hidden
                            style={{ transform: isHovered ? 'translateX(3px)' : undefined }}
                        />
                    </p>
                </CardContent>
            </Card>
        </Link>
    );
}
