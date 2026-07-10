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
import { useMemo } from 'react';

import { usePortalScopeDarkMode } from '../../features/theming/hooks/usePortalScopeDarkMode';
import { schema } from '../schema';
import { ApiDataProvider } from '../ApiMetadataBlock/ApiDataContext';
import type { Api } from '../../features/editor/entities/api';
import type { BlockNoteDocument } from '../../features/portals/types';
import { serializeTileTemplate, trimTrailingEmptyBlocks } from './tile-template';
import styles from './TileRenderer.module.scss';

type PartialBlockType = typeof schema.PartialBlock;

interface TileRendererProps {
    readonly api: Api;
    readonly tileTemplate: BlockNoteDocument;
    readonly clickable?: boolean;
    readonly onClick?: () => void;
}

function TileViewerInner({
    content,
    isDark,
}: {
    readonly content: PartialBlockType[];
    readonly isDark: boolean;
}) {
    const blockNoteTheme = isDark ? 'dark' : 'light';

    const editor = useCreateBlockNote({
        schema,
        initialContent: content,
    });

    return (
        <div className={styles.viewer}>
            <BlockNoteView editor={editor} editable={false} theme={blockNoteTheme} />
        </div>
    );
}

export function TileRenderer({ api, tileTemplate, clickable = false, onClick }: TileRendererProps) {
    const { ref: portalScopeRef, isDark } = usePortalScopeDarkMode();
    const templateKey = serializeTileTemplate(tileTemplate);
    const displayContent = useMemo(
        () => trimTrailingEmptyBlocks(tileTemplate as Record<string, unknown>[]) as PartialBlockType[],
        [tileTemplate],
    );

    const handleClick = (event: React.MouseEvent) => {
        if (!clickable) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        onClick?.();
    };

    const handleKeyDown = (event: React.KeyboardEvent) => {
        if (clickable && (event.key === 'Enter' || event.key === ' ')) {
            event.preventDefault();
            event.stopPropagation();
            onClick?.();
        }
    };

    return (
        <ApiDataProvider api={api}>
            <div
                ref={portalScopeRef}
                className={`${styles.tile} ${clickable ? styles.clickable : ''}`}
                onClick={clickable ? handleClick : undefined}
                onKeyDown={clickable ? handleKeyDown : undefined}
                role={clickable ? 'button' : undefined}
                tabIndex={clickable ? 0 : undefined}
            >
                {displayContent.length > 0 ? (
                    <TileViewerInner key={templateKey} content={displayContent} isDark={isDark} />
                ) : null}
            </div>
        </ApiDataProvider>
    );
}
