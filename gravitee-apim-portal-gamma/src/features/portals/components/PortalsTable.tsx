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
    Badge,
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Skeleton,
} from '@gravitee/graphene-core';
import {
    ExternalLinkIcon,
    MoreHorizontalIcon,
    Trash2Icon,
    Wand2Icon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { buildStandalonePortalUrl, usePortalApp } from '../../../app/PortalAppContext';
import { usePortalsNavigation } from '../config/navigation';
import type { DeveloperPortal } from '../types';
import {
    formatRelativeUpdatedAt,
    getPortalCustomDomain,
    getPortalPublishStatus,
    type PortalPublishStatus,
} from '../utils/portal-display';
import { DeletePortalDialog } from './DeletePortalDialog';

type StatusFilter = 'all' | PortalPublishStatus;

interface PortalsTableProps {
    readonly portals: readonly DeveloperPortal[];
    readonly loading: boolean;
    readonly onDeletePortal: (id: string) => Promise<void>;
}

function StatusBadge({ status }: { readonly status: PortalPublishStatus }) {
    if (status === 'Published') {
        return (
            <Badge variant="outline" className="border-success/30 text-success">
                Published
            </Badge>
        );
    }
    return (
        <Badge variant="outline" className="text-muted-foreground">
            Draft
        </Badge>
    );
}

function PortalRowActions({
    portal,
    onRequestDelete,
}: {
    readonly portal: DeveloperPortal;
    readonly onRequestDelete: () => void;
}) {
    const navigate = useNavigate();
    const { embeddedInConsole, standaloneEditorBaseUrl } = usePortalApp();

    const viewPath = `/portals/${portal.id}`;
    const editPath = `/portals/${portal.id}/edit`;
    const publicPortalUrl = portal.portalUrl?.trim() || undefined;

    const openPath = useCallback(
        (path: string, openInNewTab = true) => {
            if (embeddedInConsole && openInNewTab) {
                window.open(buildStandalonePortalUrl(standaloneEditorBaseUrl, path), '_blank', 'noopener,noreferrer');
                return;
            }
            navigate(path);
        },
        [embeddedInConsole, navigate, standaloneEditorBaseUrl],
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
                >
                    <MoreHorizontalIcon className="size-4" aria-hidden="true" />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="min-w-48">
                <DropdownMenuItem
                    className="gap-2 whitespace-nowrap"
                    onClick={() => {
                        if (publicPortalUrl) {
                            window.open(publicPortalUrl, '_blank', 'noopener,noreferrer');
                            return;
                        }
                        openPath(viewPath);
                    }}
                >
                    <ExternalLinkIcon className="size-4 shrink-0" aria-hidden="true" />
                    Open portal
                </DropdownMenuItem>
                <DropdownMenuItem className="gap-2 whitespace-nowrap" onClick={() => openPath(editPath)}>
                    <Wand2Icon className="size-4 shrink-0" aria-hidden="true" />
                    Portal Designer
                </DropdownMenuItem>
                <DropdownMenuItem className="gap-2 whitespace-nowrap text-destructive" onClick={onRequestDelete}>
                    <Trash2Icon className="size-4 shrink-0" aria-hidden="true" />
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

export function PortalsTable({ portals, loading, onDeletePortal }: PortalsTableProps) {
    const navigate = useNavigate();
    const { portalSettingsSectionPath } = usePortalsNavigation();
    const [nameFilter, setNameFilter] = useState('');
    const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
    const [deleteTarget, setDeleteTarget] = useState<DeveloperPortal | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const filteredPortals = useMemo(() => {
        const query = nameFilter.trim().toLowerCase();
        return portals.filter(portal => {
            const status = getPortalPublishStatus(portal);
            if (statusFilter !== 'all' && status !== statusFilter) {
                return false;
            }
            if (!query) {
                return true;
            }
            return portal.name.toLowerCase().includes(query);
        });
    }, [nameFilter, portals, statusFilter]);

    const handleConfirmDelete = useCallback(async () => {
        if (!deleteTarget) {
            return;
        }

        setIsDeleting(true);
        try {
            await onDeletePortal(deleteTarget.id);
            setDeleteTarget(null);
        } finally {
            setIsDeleting(false);
        }
    }, [deleteTarget, onDeletePortal]);

    return (
        <>
            <div className="space-y-4">
                <div className="flex flex-wrap items-center gap-3">
                    <Input
                        value={nameFilter}
                        onChange={event => setNameFilter(event.target.value)}
                        placeholder="Filter portals..."
                        aria-label="Filter portals by name"
                        className="h-9 w-56 shrink-0"
                    />
                    <Select value={statusFilter} onValueChange={value => setStatusFilter(value as StatusFilter)}>
                        <SelectTrigger className="h-9 w-auto shrink-0 min-w-28" aria-label="Filter by status">
                            <SelectValue placeholder="Status" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">All statuses</SelectItem>
                            <SelectItem value="Published">Published</SelectItem>
                            <SelectItem value="Draft">Draft</SelectItem>
                        </SelectContent>
                    </Select>
                </div>

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

                            {!loading && filteredPortals.length === 0 && (
                                <tr className="border-t">
                                    <td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">
                                        No portals match your filters.
                                    </td>
                                </tr>
                            )}

                            {!loading &&
                                filteredPortals.map(portal => (
                                    <tr
                                        key={portal.id}
                                        className="cursor-pointer border-t hover:bg-muted/40"
                                        onClick={() =>
                                            navigate(portalSettingsSectionPath(portal.id, 'general'))
                                        }
                                    >
                                        <td className="px-4 py-3 font-medium">{portal.name}</td>
                                        <td className="px-4 py-3">
                                            <StatusBadge status={getPortalPublishStatus(portal)} />
                                        </td>
                                        <td className="px-4 py-3 text-muted-foreground">
                                            {getPortalCustomDomain(portal)}
                                        </td>
                                        <td className="px-4 py-3 text-muted-foreground">
                                            {formatRelativeUpdatedAt(portal.updatedAt)}
                                        </td>
                                        <td className="px-4 py-3 text-right">
                                            <PortalRowActions
                                                portal={portal}
                                                onRequestDelete={() => setDeleteTarget(portal)}
                                            />
                                        </td>
                                    </tr>
                                ))}
                        </tbody>
                    </table>
                </div>
            </div>

            <DeletePortalDialog
                portal={deleteTarget}
                open={deleteTarget !== null}
                isPending={isDeleting}
                onOpenChange={open => {
                    if (!open) {
                        setDeleteTarget(null);
                    }
                }}
                onConfirm={() => void handleConfirmDelete()}
            />
        </>
    );
}
