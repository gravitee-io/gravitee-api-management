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
import { forwardRef, useCallback, useImperativeHandle, useState } from 'react';

import type { HtmlPageContent } from '../../portals/types';
import { HtmlEditorShell, type HtmlEditorLayout } from '../../html/HtmlEditorShell';
import styles from './HtmlPageEditor.module.scss';

export interface HtmlPageEditorHandle {
    save: () => Promise<void>;
}

interface HtmlPageEditorProps {
    readonly content: HtmlPageContent;
    readonly onSave: (content: HtmlPageContent) => Promise<void>;
}

export const HtmlPageEditor = forwardRef<HtmlPageEditorHandle, HtmlPageEditorProps>(function HtmlPageEditor(
    { content, onSave },
    ref,
) {
    const [html, setHtml] = useState(content.html);
    const [css, setCss] = useState(content.css ?? '');
    const [layout, setLayout] = useState<HtmlEditorLayout>('split');

    const persist = useCallback(async () => {
        await onSave({
            ...content,
            html,
            css,
        });
    }, [content, css, html, onSave]);

    useImperativeHandle(ref, () => ({
        save: persist,
    }));

    return (
        <div className={styles.editor}>
            <HtmlEditorShell
                html={html}
                css={css}
                scopeId={`page-${content.navigationItemId}`}
                layout={layout}
                onLayoutChange={setLayout}
                onHtmlChange={setHtml}
                onCssChange={setCss}
                className={styles.shell}
            />
        </div>
    );
});
