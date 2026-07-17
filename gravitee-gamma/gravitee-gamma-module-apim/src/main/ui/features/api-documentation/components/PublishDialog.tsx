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
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { ChevronDownIcon, ChevronUpIcon, ExternalLinkIcon, FolderOpenIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { buildStandalonePortalUrl } from '@apim/portal-editor/app/PortalAppContext';
import type { DeveloperPortal, PortalNavigationFolder, PortalNavigationItem } from '@apim/portal-editor/portals/types';
import { getNavItems } from '@apim/portal-editor/portals/storage/navigation-items.storage';
import { isHeaderRootNavItem, sortNavItemsByOrder } from '@apim/portal-editor/portal-shell/utils/nav-items';
import type { PublishMode } from '../services/publish-to-portal';
import styles from './PublishDialog.module.scss';

interface PublishDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly portals: readonly DeveloperPortal[];
    readonly selectedPortalId: string;
    readonly onSelectedPortalIdChange: (portalId: string) => void;
    readonly initialParentId: string | null;
    readonly apiId: string;
    readonly isPublishing: boolean;
    readonly standaloneEditorBaseUrl: string;
    readonly onQuickPublish: (options: { portalId: string }) => void;
    readonly onConfirm: (options: { portalId: string; parentId: string | null; mode: PublishMode }) => void;
}

interface LocationNode {
    readonly id: string | null;
    readonly title: string;
    readonly depth: number;
}

function buildFolderLocations(items: readonly PortalNavigationItem[]): LocationNode[] {
    const folders = sortNavItemsByOrder(
        items.filter((item): item is PortalNavigationFolder => item.type === 'FOLDER' && isHeaderRootNavItem(item)),
    );
    const locations: LocationNode[] = [{ id: null, title: 'Portal root', depth: 0 }];

    function walk(folder: PortalNavigationFolder, depth: number): void {
        locations.push({ id: folder.id, title: folder.title, depth });
        const children = sortNavItemsByOrder(
            items.filter(item => item.parentId === folder.id && item.type === 'FOLDER'),
        ) as PortalNavigationFolder[];
        for (const child of children) {
            walk(child, depth + 1);
        }
    }

    for (const folder of folders) {
        walk(folder, 1);
    }

    return locations;
}

function hasExistingApiListing(items: readonly PortalNavigationItem[], apiId: string): boolean {
    return items.some(item => item.type === 'API' && item.apiId === apiId);
}

export function PublishDialog({
    open,
    onOpenChange,
    portals,
    selectedPortalId,
    onSelectedPortalIdChange,
    initialParentId,
    apiId,
    isPublishing,
    standaloneEditorBaseUrl,
    onQuickPublish,
    onConfirm,
}: PublishDialogProps) {
    const [parentId, setParentId] = useState<string | null>(initialParentId);
    const [mode, setMode] = useState<PublishMode>('replace');
    const [portalNavItems, setPortalNavItems] = useState<PortalNavigationItem[]>([]);
    const [locationExpanded, setLocationExpanded] = useState(false);

    useEffect(() => {
        if (!open) {
            return;
        }
        setParentId(initialParentId);
        setLocationExpanded(false);
    }, [initialParentId, open]);

    useEffect(() => {
        if (!open || !selectedPortalId) {
            setPortalNavItems([]);
            return;
        }

        let cancelled = false;
        void getNavItems(selectedPortalId).then(items => {
            if (!cancelled) {
                setPortalNavItems(items);
                if (hasExistingApiListing(items, apiId)) {
                    setMode('replace');
                }
            }
        });

        return () => {
            cancelled = true;
        };
    }, [apiId, open, selectedPortalId]);

    const locations = useMemo(() => buildFolderLocations(portalNavItems), [portalNavItems]);
    const showModeChoice = hasExistingApiListing(portalNavItems, apiId);

    const handleOpenDesigner = () => {
        if (!selectedPortalId) {
            return;
        }
        const href = buildStandalonePortalUrl(standaloneEditorBaseUrl, `/portals/${selectedPortalId}/edit`);
        window.open(href, '_blank', 'noopener,noreferrer');
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className={styles.content}>
                <DialogHeader>
                    <DialogTitle>Publish to portal</DialogTitle>
                    <DialogDescription>
                        Choose the developer portal and where to publish this API documentation.
                    </DialogDescription>
                </DialogHeader>

                <div className={styles.form}>
                    <div className={styles.field}>
                        <Label htmlFor="publish-portal">Portal</Label>
                        <div className={styles.portalRow}>
                            <Select value={selectedPortalId} onValueChange={onSelectedPortalIdChange}>
                                <SelectTrigger id="publish-portal" className={styles.portalSelect}>
                                    <SelectValue placeholder="Select a portal" />
                                </SelectTrigger>
                                <SelectContent
                                    position="popper"
                                    sideOffset={4}
                                    className={styles.selectContent}
                                    style={{ width: 'var(--radix-select-trigger-width)', minWidth: 'unset' }}
                                >
                                    {portals.map(portal => (
                                        <SelectItem key={portal.id} value={portal.id}>
                                            {portal.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <Button
                                type="button"
                                className={styles.quickPublishButton}
                                disabled={!selectedPortalId || isPublishing}
                                onClick={() => onQuickPublish({ portalId: selectedPortalId })}
                            >
                                {isPublishing ? 'Publishing…' : 'Quick publish'}
                            </Button>
                        </div>
                    </div>

                    <Button
                        type="button"
                        variant="outline"
                        className={styles.locationToggle}
                        aria-expanded={locationExpanded}
                        onClick={() => setLocationExpanded(expanded => !expanded)}
                    >
                        Choose location
                        {locationExpanded ? (
                            <ChevronUpIcon className="size-4 shrink-0" aria-hidden="true" />
                        ) : (
                            <ChevronDownIcon className="size-4 shrink-0" aria-hidden="true" />
                        )}
                    </Button>

                    {locationExpanded ? (
                        <section className={styles.locationPanel} aria-labelledby="choose-location-heading">
                            <h3 id="choose-location-heading" className="sr-only">
                                Choose location
                            </h3>

                            <div className={styles.field}>
                                <Label>Location</Label>
                                <div className={styles.locationList} role="listbox" aria-label="Publish location">
                                    {locations.map(location => (
                                        <button
                                            key={location.id ?? 'root'}
                                            type="button"
                                            role="option"
                                            aria-selected={parentId === location.id}
                                            className={`${styles.locationOption} ${parentId === location.id ? styles.locationOptionSelected : ''}`}
                                            style={{ paddingLeft: `${12 + location.depth * 16}px` }}
                                            onClick={() => setParentId(location.id)}
                                        >
                                            <FolderOpenIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                                            <span>{location.title}</span>
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {showModeChoice ? (
                                <div className={styles.field}>
                                    <Label htmlFor="publish-mode">Existing API documentation</Label>
                                    <Select value={mode} onValueChange={value => setMode(value as PublishMode)}>
                                        <SelectTrigger id="publish-mode">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent
                                            position="popper"
                                            sideOffset={4}
                                            className={styles.selectContent}
                                            style={{ width: 'var(--radix-select-trigger-width)', minWidth: 'unset' }}
                                        >
                                            <SelectItem value="replace">Replace existing pages under this API</SelectItem>
                                            <SelectItem value="merge">Merge with existing pages</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            ) : null}

                            <div className={styles.sectionActions}>
                                <Button
                                    type="button"
                                    disabled={!selectedPortalId || isPublishing}
                                    onClick={() =>
                                        onConfirm({
                                            portalId: selectedPortalId,
                                            parentId,
                                            mode,
                                        })
                                    }
                                >
                                    {isPublishing ? 'Publishing…' : 'Publish'}
                                </Button>
                                <Button
                                    type="button"
                                    variant="outline"
                                    disabled={!selectedPortalId}
                                    onClick={handleOpenDesigner}
                                >
                                    <ExternalLinkIcon className="size-4" aria-hidden="true" />
                                    Open designer
                                </Button>
                            </div>
                        </section>
                    ) : null}
                </div>
            </DialogContent>
        </Dialog>
    );
}
