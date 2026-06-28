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
import '../../features/editor/styles/blocknote.css';
import { BlockNoteView } from '@blocknote/mantine';
import { useCreateBlockNote } from '@blocknote/react';
import { useTheme } from '@gravitee/graphene-core';
import { locales as multiColumnLocales } from '@blocknote/xl-multi-column';
import { en as coreEn } from '@blocknote/core/locales';

import { schema } from '../schema';
import { ApiDataProvider } from '../ApiMetadataBlock/ApiDataContext';
import type { Api } from '../../features/editor/entities/api';
import type { BlockNoteDocument } from '../../features/portals/types';
import styles from './TileRenderer.module.scss';

type PartialBlockType = typeof schema.PartialBlock;

interface TileRendererProps {
    readonly api: Api;
    readonly tileTemplate: BlockNoteDocument;
    readonly clickable?: boolean;
    readonly onClick?: () => void;
}

function TileViewerInner({ content }: { readonly content: PartialBlockType[] }) {
    const { resolvedTheme } = useTheme();
    const blockNoteTheme = resolvedTheme === 'dark' ? 'dark' : 'light';

    const editor = useCreateBlockNote({
        schema,
        initialContent: content,
        dictionary: {
            ...coreEn,
            multi_column: multiColumnLocales.en,
        },
    });

    return (
        <div className={styles.viewer}>
            <BlockNoteView editor={editor} editable={false} theme={blockNoteTheme} />
        </div>
    );
}

export function TileRenderer({ api, tileTemplate, clickable = false, onClick }: TileRendererProps) {
    const handleKeyDown = (event: React.KeyboardEvent) => {
        if (clickable && (event.key === 'Enter' || event.key === ' ')) {
            event.preventDefault();
            onClick?.();
        }
    };

    return (
        <ApiDataProvider api={api}>
            <div
                className={`${styles.tile} ${clickable ? styles.clickable : ''}`}
                onClick={clickable ? onClick : undefined}
                onKeyDown={clickable ? handleKeyDown : undefined}
                role={clickable ? 'button' : undefined}
                tabIndex={clickable ? 0 : undefined}
            >
                <TileViewerInner content={tileTemplate as PartialBlockType[]} />
            </div>
        </ApiDataProvider>
    );
}
