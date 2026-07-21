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
import { ExternalLinkIcon, SettingsIcon, Trash2Icon, Wand2Icon } from '@gravitee/graphene-core/icons';
import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';

import { buildStandalonePortalUrl, usePortalApp } from '../../../app/PortalAppContext';
import { usePortalsNavigation } from '../config/navigation';
import type { DeveloperPortal } from '../types';
import { isPlaceholderScreenshot } from '../utils/screenshot';
import { PortalTileSkeleton } from './PortalTileSkeleton';

const HOVER_OVERLAY = 'color-mix(in oklab, var(--color-background) 45%, transparent)';
const TILE_ACTIONS_LAYOUT_STYLE = { placeItems: 'center' } as const;
const TILE_ACTIONS_LAYOUT_CLASS = 'grid w-full grid-cols-4 gap-[6%] px-[4%]';
const TILE_ACTION_BUTTON_CLASS =
    'flex aspect-square !size-auto !h-auto !min-h-0 !min-w-0 w-full max-w-full items-center justify-center p-0';
const TILE_ACTION_ICON_CLASS = 'size-1/2';

function PortalTileAction({
    label,
    to,
    embeddedInConsole,
    standaloneEditorBaseUrl,
    openInNewTab = true,
    externalHref,
    children,
}: {
    readonly label: string;
    readonly to: string;
    readonly embeddedInConsole: boolean;
    readonly standaloneEditorBaseUrl: string;
    readonly openInNewTab?: boolean;
    readonly externalHref?: string;
    readonly children: ReactNode;
}) {
    if (externalHref) {
        return (
            <Button
                variant="ghost"
                className={TILE_ACTION_BUTTON_CLASS}
                aria-label={label}
                onClick={() => window.open(externalHref, '_blank', 'noopener,noreferrer')}
            >
                {children}
            </Button>
        );
    }

    if (embeddedInConsole && openInNewTab) {
        const href = buildStandalonePortalUrl(standaloneEditorBaseUrl, to);
        return (
            <Button
                variant="ghost"
                className={TILE_ACTION_BUTTON_CLASS}
                aria-label={label}
                onClick={() => window.open(href, '_blank', 'noopener,noreferrer')}
            >
                {children}
            </Button>
        );
    }

    return (
        <Button variant="ghost" className={TILE_ACTION_BUTTON_CLASS} asChild>
            <Link to={to} aria-label={label}>
                {children}
            </Link>
        </Button>
    );
}

export function PortalTile({
    portal,
    onRequestDelete,
}: {
    readonly portal: DeveloperPortal;
    readonly onRequestDelete: () => void;
}) {
    const [isHovered, setIsHovered] = useState(false);
    const { embeddedInConsole, standaloneEditorBaseUrl } = usePortalApp();
    const { portalSettingsPath } = usePortalsNavigation();
    const showSkeleton = isPlaceholderScreenshot(portal.screenshotDataUrl);
    const viewPath = `/portals/${portal.id}`;
    const editPath = `/portals/${portal.id}/edit`;
    const settingsPath = portalSettingsPath(portal.id);
    const publicPortalUrl = portal.portalUrl?.trim() || undefined;

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
            {showSkeleton ? (
                <PortalTileSkeleton />
            ) : (
                <img
                    src={portal.screenshotDataUrl}
                    alt=""
                    className="size-full object-cover"
                    aria-hidden="true"
                />
            )}
            <div className="absolute inset-x-0 bottom-0 bg-background/70 px-3 py-2">
                <p className="truncate text-sm font-medium">{portal.name}</p>
                {portal.portalUrl ? (
                    <p className="truncate text-xs text-muted-foreground">{portal.portalUrl}</p>
                ) : null}
            </div>
            {isHovered && (
                <div
                    className="absolute inset-0 flex items-center justify-center"
                    style={{ backgroundColor: HOVER_OVERLAY }}
                >
                    <div className={TILE_ACTIONS_LAYOUT_CLASS} style={TILE_ACTIONS_LAYOUT_STYLE}>
                        <PortalTileAction
                            label="Open portal"
                            to={viewPath}
                            embeddedInConsole={embeddedInConsole}
                            standaloneEditorBaseUrl={standaloneEditorBaseUrl}
                            externalHref={publicPortalUrl}
                        >
                            <ExternalLinkIcon className={TILE_ACTION_ICON_CLASS} aria-hidden="true" />
                        </PortalTileAction>
                        <PortalTileAction
                            label="Edit portal"
                            to={editPath}
                            embeddedInConsole={embeddedInConsole}
                            standaloneEditorBaseUrl={standaloneEditorBaseUrl}
                        >
                            <Wand2Icon className={TILE_ACTION_ICON_CLASS} aria-hidden="true" />
                        </PortalTileAction>
                        <PortalTileAction
                            label="Portal settings"
                            to={settingsPath}
                            embeddedInConsole={embeddedInConsole}
                            standaloneEditorBaseUrl={standaloneEditorBaseUrl}
                            openInNewTab={false}
                        >
                            <SettingsIcon className={TILE_ACTION_ICON_CLASS} aria-hidden="true" />
                        </PortalTileAction>
                        <Button
                            variant="ghost"
                            className={TILE_ACTION_BUTTON_CLASS}
                            aria-label="Delete portal"
                            onClick={event => {
                                event.stopPropagation();
                                onRequestDelete();
                            }}
                        >
                            <Trash2Icon className={TILE_ACTION_ICON_CLASS} aria-hidden="true" />
                        </Button>
                    </div>
                </div>
            )}
        </Card>
    );
}
