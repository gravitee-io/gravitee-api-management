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
import { forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react';

import { BlockEditor, type BlockEditorHandle } from '../../editor/components/BlockEditor';
import { BlockViewer } from '../../editor/components/BlockViewer';
import { OpenApiPageEditor, type OpenApiPageEditorHandle } from '../../editor/components/OpenApiPageEditor';
import { OpenApiPageViewer } from '../../editor/components/OpenApiPageViewer';
import type { PageWidth } from '../../editor/constants/page-width';
import type { EditorMode } from '../../editor/stores/editor.store';
import type {
    BlockNoteDocument,
    BlockPageContent,
    OpenApiPageContent,
    PageContent,
    PortalNavigationItem,
    PortalNavigationOpenApiPage,
    PortalNavigationPage,
} from '../../portals/types';
import type { BlockStyleOverrides } from '../../theming/types/block-styles.types';
import { getPageContent, savePageContent } from '../../portals/storage/page-contents.storage';
import { getPageContentType, isBlockPageContent, isOpenApiPage, isOpenApiPageContent } from '../../portals/utils/page-content-type';
import { resolveBlockPageDocument, serializeDocumentToGmd } from '../../editor/gmd/gmd-content';
import type { UpdateNavItemPatch } from '../hooks/useNavigation';
import { PortalPageProvider } from '../context/PortalPageContext';
import { ApiDataProviderFromPortal } from '../../../blocks/ApiMetadataBlock/ApiDataContext';
import styles from './ContentArea.module.scss';

export interface ContentAreaHandle {
    save: () => Promise<void>;
}

interface ContentAreaProps {
    readonly portalId: string;
    readonly selectedNavItemId: string | null;
    readonly navItems: readonly PortalNavigationItem[];
    readonly mode: EditorMode;
    readonly pageWidth: PageWidth;
    readonly onUpdateNavItem?: (id: string, patch: UpdateNavItemPatch) => void;
}

export const ContentArea = forwardRef<ContentAreaHandle, ContentAreaProps>(function ContentArea(
    { portalId, selectedNavItemId, navItems, mode, pageWidth, onUpdateNavItem },
    ref,
) {
    const blockEditorRef = useRef<BlockEditorHandle>(null);
    const openApiEditorRef = useRef<OpenApiPageEditorHandle>(null);
    const [pageContent, setPageContent] = useState<PageContent | undefined>();
    const [loading, setLoading] = useState(false);
    const [editorKey, setEditorKey] = useState(0);

    const selectedPage = useMemo(
        () =>
            navItems.find(
                (item): item is PortalNavigationPage => item.id === selectedNavItemId && item.type === 'PAGE',
            ),
        [navItems, selectedNavItemId],
    );

    const pageContentType = selectedPage ? getPageContentType(selectedPage) : 'BLOCK';

    useEffect(() => {
        if (!selectedNavItemId) {
            setPageContent(undefined);
            return;
        }

        let cancelled = false;
        setLoading(true);

        void (async () => {
            const content = await getPageContent(selectedNavItemId);
            if (!cancelled) {
                setPageContent(content ?? undefined);
                setEditorKey(prev => prev + 1);
                setLoading(false);
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [selectedNavItemId, portalId]);

    const handleDocumentSave = useCallback(
        async (document: BlockNoteDocument, blockStyles: Record<string, BlockStyleOverrides>) => {
            if (!pageContent || !isBlockPageContent(pageContent)) return;
            const updated: BlockPageContent = {
                ...pageContent,
                contentType: 'BLOCK',
                document,
                gmd: serializeDocumentToGmd(document),
                blockStyles: Object.keys(blockStyles).length > 0 ? blockStyles : undefined,
            };
            await savePageContent(updated);
            setPageContent(updated);
        },
        [pageContent],
    );

    const handleOpenApiSave = useCallback(
        async (content: OpenApiPageContent, pagePatch?: Partial<PortalNavigationOpenApiPage>) => {
            await savePageContent(content);
            setPageContent(content);
            if (pagePatch && selectedPage && isOpenApiPage(selectedPage)) {
                onUpdateNavItem?.(selectedPage.id, {
                    renderer: pagePatch.renderer,
                    specSource: pagePatch.specSource,
                });
            }
        },
        [onUpdateNavItem, selectedPage],
    );

    useImperativeHandle(ref, () => ({
        save: async () => {
            if (pageContentType === 'OPENAPI') {
                await openApiEditorRef.current?.save();
                return;
            }
            await blockEditorRef.current?.save();
        },
    }));

    const savePage = useCallback(async () => {
        await blockEditorRef.current?.save();
    }, []);

    if (loading) {
        return (
            <main className={styles.contentArea}>
                <p className="p-6 text-sm text-muted-foreground">Loading page…</p>
            </main>
        );
    }

    if (!pageContent || !selectedPage) {
        return (
            <main className={styles.contentArea}>
                <p className="p-6 text-sm text-muted-foreground">Select a page to view its content.</p>
            </main>
        );
    }

    return (
        <main className={styles.contentArea}>
            <PortalPageProvider
                portalId={portalId}
                selectedNavItemId={selectedNavItemId}
                navItems={navItems}
                savePage={mode === 'edit' && pageContentType !== 'OPENAPI' ? savePage : undefined}
            >
                <ApiDataProviderFromPortal>
                    {pageContentType === 'OPENAPI' && isOpenApiPage(selectedPage) && isOpenApiPageContent(pageContent) ? (
                    mode === 'edit' ? (
                        <OpenApiPageEditor
                            key={editorKey}
                            ref={openApiEditorRef}
                            page={selectedPage}
                            content={pageContent}
                            navItems={navItems}
                            onSave={handleOpenApiSave}
                        />
                    ) : (
                        <OpenApiPageViewer page={selectedPage} content={pageContent} navItems={navItems} />
                    )
                ) : isBlockPageContent(pageContent) ? (
                    mode === 'edit' ? (
                        <BlockEditor
                            key={editorKey}
                            ref={blockEditorRef}
                            document={resolveBlockPageDocument(pageContent.document, pageContent.gmd)}
                            blockStyles={pageContent.blockStyles}
                            navigationItemId={selectedPage.id}
                            pageWidth={pageWidth}
                            onSave={handleDocumentSave}
                        />
                    ) : (
                        <BlockViewer
                            document={resolveBlockPageDocument(pageContent.document, pageContent.gmd)}
                            pageWidth={pageWidth}
                        />
                    )
                ) : (
                    <p className="p-6 text-sm text-muted-foreground">This page type is not supported yet.</p>
                )}
                </ApiDataProviderFromPortal>
            </PortalPageProvider>
        </main>
    );
});
