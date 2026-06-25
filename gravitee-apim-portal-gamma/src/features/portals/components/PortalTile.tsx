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
import { Button, Card } from '@gravitee/graphene-core';
import { EyeIcon, PencilIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link } from 'react-router-dom';

import type { DeveloperPortal } from '../types';

const HOVER_OVERLAY = 'color-mix(in oklab, var(--color-background) 45%, transparent)';

export function PortalTile({ portal }: { readonly portal: DeveloperPortal }) {
    const [isHovered, setIsHovered] = useState(false);

    return (
        <Card
            className="relative size-full overflow-hidden p-0"
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
            onFocus={() => setIsHovered(true)}
            onBlur={event => {
                if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
                    setIsHovered(false);
                }
            }}
            tabIndex={0}
        >
            <img
                src={portal.screenshotDataUrl}
                alt=""
                className="size-full object-cover"
                aria-hidden="true"
            />
            <div className="absolute inset-x-0 bottom-0 bg-background/70 px-3 py-2">
                <p className="truncate text-sm font-medium">{portal.name}</p>
            </div>
            {isHovered && (
                <div
                    className="absolute inset-0 flex items-center justify-center gap-4"
                    style={{ backgroundColor: HOVER_OVERLAY }}
                >
                    <Button variant="ghost" size="icon" className="size-12" asChild>
                        <Link to={`/portals/${portal.id}`} aria-label="Open portal">
                            <EyeIcon className="size-6" aria-hidden="true" />
                        </Link>
                    </Button>
                    <Button variant="ghost" size="icon" className="size-12" asChild>
                        <Link to={`/portals/${portal.id}/edit`} aria-label="Edit portal">
                            <PencilIcon className="size-6" aria-hidden="true" />
                        </Link>
                    </Button>
                </div>
            )}
        </Card>
    );
}
