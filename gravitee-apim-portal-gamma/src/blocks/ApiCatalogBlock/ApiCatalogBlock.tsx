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
import { createReactBlockSpec } from '@blocknote/react';
import { useQuery } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';

import { getApiById } from '../../features/editor/services/api.service';
import { usePortalPageOptional } from '../../features/portal-shell/context/PortalPageContext';
import type { BlockNoteDocument } from '../../features/portals/types';

import { CatalogView } from './CatalogView';
import { getPublishedApiNavItems } from './catalog-utils';
import { TileEditorDialog } from './TileEditorDialog';
import { DEFAULT_TILE_TEMPLATE, parseTileTemplate, serializeTileTemplate } from './tile-template';
import styles from './ApiCatalogBlock.module.scss';

export const ApiCatalogBlock = createReactBlockSpec(
    {
        type: 'graviteeApiCatalog' as const,
        propSchema: {
            title: { default: 'API Catalog' },
            tileTemplate: { default: serializeTileTemplate(DEFAULT_TILE_TEMPLATE) },
        },
        content: 'none',
    },
    {
        render: ({ block, editor }) => {
            const { title, tileTemplate } = block.props;
            const isEditable = editor.isEditable;
            const [dialogOpen, setDialogOpen] = useState(false);

            const portalPage = usePortalPageOptional();
            const navItems = portalPage?.navItems ?? [];
            const publishedApiNavItems = useMemo(() => getPublishedApiNavItems(navItems), [navItems]);
            const previewApiId = publishedApiNavItems[0]?.apiId;

            const { data: previewApi } = useQuery({
                queryKey: ['api', previewApiId],
                queryFn: () => getApiById(previewApiId!),
                enabled: Boolean(previewApiId),
            });

            const parsedTemplate = useMemo(() => parseTileTemplate(tileTemplate), [tileTemplate]);

            const handleSaveTemplate = useCallback(
                (document: BlockNoteDocument) => {
                    editor.updateBlock(block, {
                        props: { tileTemplate: serializeTileTemplate(document) },
                    });
                },
                [block, editor],
            );

            return (
                <div className={`${styles.wrapper} ${isEditable ? styles.editable : ''}`}>
                    {isEditable ? (
                        <div className={styles.floatingToolbar}>
                            <button
                                className={styles.customizeBtn}
                                onClick={() => setDialogOpen(true)}
                                title="Customize API tiles"
                                type="button"
                            >
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M12 20h9" />
                                    <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                                </svg>
                                Customize tiles
                            </button>
                        </div>
                    ) : null}

                    <CatalogView
                        title={title}
                        tileTemplate={parsedTemplate}
                        clickable={!isEditable}
                    />

                    {isEditable ? (
                        <TileEditorDialog
                            open={dialogOpen}
                            onOpenChange={setDialogOpen}
                            tileTemplate={parsedTemplate}
                            previewApi={previewApi ?? null}
                            onSave={handleSaveTemplate}
                        />
                    ) : null}
                </div>
            );
        },
    },
);
