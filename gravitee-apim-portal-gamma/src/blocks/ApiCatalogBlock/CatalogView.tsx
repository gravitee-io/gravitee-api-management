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
import { useQueries } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';

import type { Api } from '../../features/editor/entities/api';
import { getApiById } from '../../features/editor/services/api.service';
import { usePortalPageOptional } from '../../features/portal-shell/context/PortalPageContext';
import type { BlockNoteDocument } from '../../features/portals/types';

import { findFirstChildPage, getPublishedApiNavItems } from './catalog-utils';
import { CatalogListRow } from './CatalogListRow';
import { TileRenderer } from './TileRenderer';
import styles from './CatalogView.module.scss';

export type ViewMode = 'cards' | 'list';

interface CatalogViewProps {
    readonly title?: string;
    readonly tileTemplate: BlockNoteDocument;
    readonly viewMode?: ViewMode;
    readonly clickable?: boolean;
}

interface PublishedApiEntry {
    readonly navItemId: string;
    readonly apiId: string;
    readonly api: Api | undefined;
    readonly isLoading: boolean;
    readonly isError: boolean;
}

function usePortalPageNavigation(portalId?: string) {
    const { id: routePortalId } = useParams<{ id: string }>();
    const location = useLocation();
    const navigate = useNavigate();
    const isEditMode = /\/edit(\/|$)/.test(location.pathname);
    const resolvedPortalId = portalId ?? routePortalId;

    const navigateToPageSlug = useCallback(
        (slug: string) => {
            if (!resolvedPortalId) {
                return;
            }
            const path = isEditMode
                ? `/portals/${resolvedPortalId}/edit/${slug}`
                : `/portals/${resolvedPortalId}/${slug}`;
            navigate(path);
        },
        [resolvedPortalId, isEditMode, navigate],
    );

    return { navigateToPageSlug };
}

export function CatalogView({ title, tileTemplate, viewMode = 'cards', clickable = false }: CatalogViewProps) {
    const portalPage = usePortalPageOptional();
    const navItems = portalPage?.navItems ?? [];
    const { navigateToPageSlug } = usePortalPageNavigation(portalPage?.portalId);

    const publishedApiNavItems = useMemo(() => getPublishedApiNavItems(navItems), [navItems]);

    const apiQueries = useQueries({
        queries: publishedApiNavItems.map(navItem => ({
            queryKey: ['api', navItem.apiId],
            queryFn: () => getApiById(navItem.apiId),
            enabled: Boolean(navItem.apiId),
        })),
    });

    const entries: PublishedApiEntry[] = useMemo(
        () =>
            publishedApiNavItems.map((navItem, index) => ({
                navItemId: navItem.id,
                apiId: navItem.apiId,
                api: apiQueries[index]?.data,
                isLoading: apiQueries[index]?.isLoading ?? false,
                isError: apiQueries[index]?.isError ?? false,
            })),
        [publishedApiNavItems, apiQueries],
    );

    const isLoading = entries.some(entry => entry.isLoading);
    const hasError = entries.some(entry => entry.isError);
    const resolvedEntries = entries.filter((entry): entry is PublishedApiEntry & { api: Api } => Boolean(entry.api));

    const handleTileClick = useCallback(
        (navItemId: string) => {
            const overviewPage = findFirstChildPage(navItems, navItemId);
            if (!overviewPage) {
                return;
            }

            if (portalPage?.onSelectNavItem) {
                portalPage.onSelectNavItem(overviewPage.id);
                return;
            }

            navigateToPageSlug(overviewPage.slug);
        },
        [navItems, navigateToPageSlug, portalPage],
    );

    return (
        <div className={styles.container}>
            {title ? <h3 className={styles.title}>{title}</h3> : null}

            {isLoading && resolvedEntries.length === 0 ? (
                <div className={styles.state}>
                    <div className={styles.spinner} />
                    <span>Loading APIs...</span>
                </div>
            ) : null}

            {hasError && resolvedEntries.length === 0 ? (
                <div className={styles.state}>
                    <span className={styles.error}>Failed to load APIs.</span>
                </div>
            ) : null}

            {!isLoading && !hasError && publishedApiNavItems.length === 0 ? (
                <div className={styles.state}>
                    <span>No APIs published to this portal yet.</span>
                </div>
            ) : null}

            {resolvedEntries.length > 0 ? (
                viewMode === 'cards' ? (
                    <div className={styles.grid}>
                        {resolvedEntries.map(entry => (
                            <TileRenderer
                                key={entry.navItemId}
                                api={entry.api}
                                tileTemplate={tileTemplate}
                                clickable={clickable}
                                onClick={() => handleTileClick(entry.navItemId)}
                            />
                        ))}
                    </div>
                ) : (
                    <div className={styles.list}>
                        {resolvedEntries.map(entry => (
                            <CatalogListRow
                                key={entry.navItemId}
                                api={entry.api}
                                clickable={clickable}
                                onClick={() => handleTileClick(entry.navItemId)}
                            />
                        ))}
                    </div>
                )
            ) : null}
        </div>
    );
}
