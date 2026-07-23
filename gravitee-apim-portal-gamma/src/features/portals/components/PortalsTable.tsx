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
import {
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Skeleton,
} from '@gravitee/graphene-core';
import { ExternalLinkIcon, MoreHorizontalIcon, Wand2Icon } from '@gravitee/graphene-core/icons';
import { useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

import { buildStandalonePortalUrl, usePortalApp } from '../../../app/PortalAppContext';
import constants from '../../../constants.json';
import { usePortalsNavigation } from '../config/navigation';
import type { DeveloperPortal } from '../types';
import {
    formatRelativeUpdatedAt,
    getPortalCustomDomain,
    getPortalPublishStatus,
} from '../utils/portal-display';
import { PortalStatusBadge } from './PortalStatusBadge';

interface PortalsTableProps {
    readonly portals: readonly DeveloperPortal[];
    readonly loading: boolean;
}

function PortalRowActions({
    portal,
    onActionSelect,
}: {
    readonly portal: DeveloperPortal;
    readonly onActionSelect: () => void;
}) {
    const { standaloneEditorBaseUrl } = usePortalApp();

    const viewPath = `/portals/${portal.id}`;
    const editPath = `/portals/${portal.id}/edit`;
    const editorBaseUrl = standaloneEditorBaseUrl || constants.appBasePath || '';

    const openInNewTab = useCallback(
        (path: string) => {
            window.open(buildStandalonePortalUrl(editorBaseUrl, path), '_blank', 'noopener,noreferrer');
        },
        [editorBaseUrl],
    );

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    variant="ghost"
                    size="icon"
                    className="size-8"
                    aria-label={`Actions for ${portal.name}`}
                    onClick={event => event.stopPropagation()}
                    onPointerDown={event => event.stopPropagation()}
                >
                    <MoreHorizontalIcon className="size-4" aria-hidden="true" />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
                align="end"
                className="min-w-48"
                onCloseAutoFocus={event => event.preventDefault()}
            >
                <DropdownMenuItem
                    className="gap-2 whitespace-nowrap"
                    onSelect={() => {
                        onActionSelect();
                        // POC: ignore portalUrl (custom domains aren't wired; opening them hits DNS errors).
                        openInNewTab(viewPath);
                    }}
                >
                    <ExternalLinkIcon className="size-4 shrink-0" aria-hidden="true" />
                    Open portal
                </DropdownMenuItem>
                <DropdownMenuItem
                    className="gap-2 whitespace-nowrap"
                    onSelect={() => {
                        onActionSelect();
                        openInNewTab(editPath);
                    }}
                >
                    <Wand2Icon className="size-4 shrink-0" aria-hidden="true" />
                    Portal Designer
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

export function PortalsTable({ portals, loading }: PortalsTableProps) {
    const navigate = useNavigate();
    const { portalSettingsSectionPath } = usePortalsNavigation();
    const suppressRowClickRef = useRef(false);

    const handleActionSelect = useCallback(() => {
        suppressRowClickRef.current = true;
        window.setTimeout(() => {
            suppressRowClickRef.current = false;
        }, 300);
    }, []);

    return (
        <div className="overflow-hidden rounded-lg border">
            <table className="w-full text-sm">
                <thead className="bg-muted/40 text-left">
                    <tr>
                        <th className="px-4 py-3 font-medium">Name</th>
                        <th className="px-4 py-3 font-medium">Status</th>
                        <th className="px-4 py-3 font-medium">Custom Domain</th>
                        <th className="px-4 py-3 font-medium">Last Updated</th>
                        <th className="px-4 py-3 font-medium">
                            <span className="sr-only">Actions</span>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {loading &&
                        Array.from({ length: 3 }, (_, index) => (
                            <tr key={index} className="border-t">
                                <td className="px-4 py-3">
                                    <Skeleton className="h-4 w-40 rounded" />
                                </td>
                                <td className="px-4 py-3">
                                    <Skeleton className="h-5 w-20 rounded" />
                                </td>
                                <td className="px-4 py-3">
                                    <Skeleton className="h-4 w-36 rounded" />
                                </td>
                                <td className="px-4 py-3">
                                    <Skeleton className="h-4 w-20 rounded" />
                                </td>
                                <td className="px-4 py-3">
                                    <Skeleton className="ml-auto size-8 rounded" />
                                </td>
                            </tr>
                        ))}

                    {!loading && portals.length === 0 && (
                        <tr className="border-t">
                            <td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">
                                No portals match your filters.
                            </td>
                        </tr>
                    )}

                    {!loading &&
                        portals.map(portal => (
                            <tr
                                key={portal.id}
                                className="cursor-pointer border-t hover:bg-muted/40"
                                onClick={() => {
                                    if (suppressRowClickRef.current) {
                                        return;
                                    }
                                    navigate(portalSettingsSectionPath(portal.id, 'general'));
                                }}
                            >
                                <td className="px-4 py-3 font-medium">{portal.name}</td>
                                <td className="px-4 py-3">
                                    <PortalStatusBadge status={getPortalPublishStatus(portal)} />
                                </td>
                                <td className="px-4 py-3 text-muted-foreground">
                                    {getPortalCustomDomain(portal)}
                                </td>
                                <td className="px-4 py-3 text-muted-foreground">
                                    {formatRelativeUpdatedAt(portal.updatedAt)}
                                </td>
                                <td
                                    className="px-4 py-3 text-right"
                                    onClick={event => event.stopPropagation()}
                                    onPointerDown={event => event.stopPropagation()}
                                >
                                    <PortalRowActions portal={portal} onActionSelect={handleActionSelect} />
                                </td>
                            </tr>
                        ))}
                </tbody>
            </table>
        </div>
    );
}
