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
import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';

import { BlockEditor, type BlockEditorHandle } from '../../editor/components/BlockEditor';
import { BlockViewer } from '../../editor/components/BlockViewer';
import type { PageWidth } from '../../editor/constants/page-width';
import type { EditorMode } from '../../editor/stores/editor.store';
import type { BlockNoteDocument, PageContent } from '../../portals/types';
import { getPageContent, savePageContent } from '../../portals/storage/page-contents.storage';
import styles from './ContentArea.module.scss';

export interface ContentAreaHandle {
    save: () => Promise<void>;
}

interface ContentAreaProps {
    readonly portalId: string;
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly pageWidth: PageWidth;
}

export const ContentArea = forwardRef<ContentAreaHandle, ContentAreaProps>(function ContentArea(
    { portalId, selectedNavItemId, mode, pageWidth },
    ref,
) {
    const editorRef = useRef<BlockEditorHandle>(null);
    const [pageContent, setPageContent] = useState<PageContent | undefined>();
    const [loading, setLoading] = useState(false);
    const [editorKey, setEditorKey] = useState(0);

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
        async (document: BlockNoteDocument) => {
            if (!pageContent) return;
            const updated: PageContent = { ...pageContent, document };
            await savePageContent(updated);
            setPageContent(updated);
        },
        [pageContent],
    );

    useImperativeHandle(ref, () => ({
        save: async () => {
            await editorRef.current?.save();
        },
    }));

    if (loading) {
        return (
            <main className={styles.contentArea}>
                <p className="p-6 text-sm text-muted-foreground">Loading page…</p>
            </main>
        );
    }

    if (!pageContent) {
        return (
            <main className={styles.contentArea}>
                <p className="p-6 text-sm text-muted-foreground">Select a page to view its content.</p>
            </main>
        );
    }

    return (
        <main className={styles.contentArea}>
            {mode === 'edit' ? (
                <BlockEditor
                    key={editorKey}
                    ref={editorRef}
                    document={pageContent.document}
                    pageWidth={pageWidth}
                    onSave={handleDocumentSave}
                />
            ) : (
                <BlockViewer document={pageContent.document} pageWidth={pageWidth} />
            )}
        </main>
    );
});
