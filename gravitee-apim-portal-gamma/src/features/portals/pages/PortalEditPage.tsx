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
import { Button } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { BlockEditor, type BlockEditorHandle } from '../../editor/components/BlockEditor';
import { BlockViewer } from '../../editor/components/BlockViewer';
import { EditorHeader } from '../../editor/components/EditorHeader';
import { EditorShellPlaceholder } from '../../editor/components/EditorShellPlaceholder';
import { useEditorStore } from '../../editor/stores/editor.store';
import { getNavItems } from '../storage/navigation-items.storage';
import { ensureDefaultPageForPortal } from '../storage/ensure-default-page';
import { getPageContent, savePageContent } from '../storage/page-contents.storage';
import { getPortal, savePortal } from '../storage/portals.storage';
import type { BlockNoteDocument, DeveloperPortal, PageContent } from '../types';

function BackToDashboardsLink() {
    return (
        <Button variant="ghost" size="sm" className="-ml-2 w-fit gap-1.5" asChild>
            <Link to="/">
                <ArrowLeftIcon className="size-4" aria-hidden="true" />
                Back to dashboards
            </Link>
        </Button>
    );
}

export function PortalEditPage() {
    const { id } = useParams<{ id: string }>();
    const editorRef = useRef<BlockEditorHandle>(null);
    const [portal, setPortal] = useState<DeveloperPortal | undefined>();
    const [pageContent, setPageContent] = useState<PageContent | undefined>();
    const [loading, setLoading] = useState(true);

    const {
        mode,
        pageWidth,
        layout,
        isSaving,
        initialize,
        reset,
        setMode,
        setPageWidth,
        setLayout,
        save,
    } = useEditorStore();

    useEffect(() => {
        if (!id) {
            setLoading(false);
            return;
        }

        let cancelled = false;

        void (async () => {
            const loadedPortal = await getPortal(id);
            if (cancelled) {
                return;
            }

            if (!loadedPortal) {
                setLoading(false);
                return;
            }

            setPortal(loadedPortal);
            initialize(loadedPortal);

            const navItems = await getNavItems(id);
            const firstPage = navItems.find(item => item.type === 'PAGE');
            const content = firstPage
                ? (await getPageContent(firstPage.id)) ?? (await ensureDefaultPageForPortal(id))
                : await ensureDefaultPageForPortal(id);
            if (!cancelled) {
                setPageContent(content);
            }

            if (!cancelled) {
                setLoading(false);
            }
        })();

        return () => {
            cancelled = true;
            reset();
        };
    }, [id, initialize, reset]);

    const handleSave = useCallback(async () => {
        if (!portal) {
            return;
        }

        await save(async () => {
            if (pageContent) {
                await editorRef.current?.save();
            }
            await savePortal({ ...portal, layout, updatedAt: new Date().toISOString() });
            setPortal(current => (current ? { ...current, layout, updatedAt: new Date().toISOString() } : current));
        });
    }, [layout, pageContent, portal, save]);

    const handleDocumentSave = useCallback(
        async (document: BlockNoteDocument) => {
            if (!pageContent) {
                return;
            }

            const updated: PageContent = { ...pageContent, document };
            await savePageContent(updated);
            setPageContent(updated);
        },
        [pageContent],
    );

    if (loading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading portal…</p>;
    }

    if (!portal) {
        return (
            <div className="space-y-4 p-6">
                <BackToDashboardsLink />
                <p className="text-sm text-muted-foreground">Portal not found.</p>
            </div>
        );
    }

    const isEditMode = mode === 'edit';

    return (
        <div className="flex h-screen flex-col overflow-hidden">
            <EditorHeader
                portalName={portal.name}
                mode={mode}
                pageWidth={pageWidth}
                layout={layout}
                isSaving={isSaving}
                onModeChange={setMode}
                onPageWidthChange={setPageWidth}
                onLayoutChange={setLayout}
                onSave={() => void handleSave()}
            />

            <EditorShellPlaceholder layout={layout} mode={mode}>
                {isEditMode ? (
                    <BlockEditor
                        ref={editorRef}
                        document={pageContent?.document}
                        pageWidth={pageWidth}
                        onSave={handleDocumentSave}
                    />
                ) : (
                    <BlockViewer document={pageContent?.document} pageWidth={pageWidth} />
                )}
            </EditorShellPlaceholder>
        </div>
    );
}
