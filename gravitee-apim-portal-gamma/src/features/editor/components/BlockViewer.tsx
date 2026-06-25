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
import '../styles/blocknote.css';
import { BlockNoteView } from '@blocknote/mantine';
import { useCreateBlockNote } from '@blocknote/react';
import { locales as multiColumnLocales } from '@blocknote/xl-multi-column';
import { en as coreEn } from '@blocknote/core/locales';

import { schema } from '../../../blocks/schema';
import type { BlockNoteDocument } from '../../portals/types';
import { PAGE_WIDTH_VALUES, type PageWidth } from '../constants/page-width';
import styles from './BlockViewer.module.scss';

type PartialBlockType = typeof schema.PartialBlock;

interface BlockViewerProps {
    readonly document?: BlockNoteDocument;
    readonly pageWidth?: PageWidth;
}

function BlockViewerInner({ content, pageWidth }: { readonly content: PartialBlockType[]; readonly pageWidth: PageWidth }) {
    const editor = useCreateBlockNote({
        schema,
        initialContent: content,
        dictionary: {
            ...coreEn,
            multi_column: multiColumnLocales.en,
        },
    });

    return (
        <div
            className={styles.container}
            style={{ '--page-width': PAGE_WIDTH_VALUES[pageWidth] } as React.CSSProperties}
        >
            <BlockNoteView editor={editor} editable={false} />
        </div>
    );
}

export function BlockViewer({ document, pageWidth = 'narrow' }: BlockViewerProps) {
    if (!document || document.length === 0) {
        return (
            <div className={styles.empty}>
                <p>No content yet.</p>
            </div>
        );
    }

    return <BlockViewerInner content={document as PartialBlockType[]} pageWidth={pageWidth} />;
}
