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
import { type MouseEvent, type RefObject, useLayoutEffect, useMemo, useRef } from 'react';

import { HtmlSlotHydrator } from './hydrate-slots';
import { sanitizePortalHtml } from './sanitize-html';
import { scopeCustomCss } from './scope-custom-css';
import styles from './HtmlEditorShell.module.scss';

interface HtmlContentViewProps {
    readonly html: string;
    readonly css?: string;
    readonly scopeId: string;
    readonly shouldHydrateSlots?: boolean;
    readonly className?: string;
    readonly isolateBlockNoteEvents?: boolean;
    readonly styleTarget?: string;
}

export function stopBlockNoteTableHandling(event: MouseEvent) {
    // BlockNote's TableHandles listens on the editor root for mouse events and
    // misidentifies native <table> markup inside custom HTML blocks as BlockNote
    // table blocks, then crashes on content.rows.
    event.stopPropagation();
}

export function HtmlContentView({
    html,
    css = '',
    scopeId,
    shouldHydrateSlots = true,
    className,
    isolateBlockNoteEvents = false,
    styleTarget = 'html-block',
}: HtmlContentViewProps) {
    const htmlContentRef = useRef<HTMLDivElement>(null);
    const scopedCss = useMemo(
        () => scopeCustomCss(css, `[data-block-scope="${scopeId}"]`),
        [css, scopeId],
    );
    const sanitizedHtml = useMemo(() => sanitizePortalHtml(html), [html]);

    useLayoutEffect(() => {
        if (htmlContentRef.current) {
            htmlContentRef.current.innerHTML = sanitizedHtml;
        }
    }, [sanitizedHtml]);

    const eventHandlers = isolateBlockNoteEvents
        ? {
              onMouseDown: stopBlockNoteTableHandling,
              onMouseMove: stopBlockNoteTableHandling,
              onMouseUp: stopBlockNoteTableHandling,
          }
        : undefined;

    return (
        <div
            className={className ?? styles.contentView}
            data-block-scope={scopeId}
            data-style-target={styleTarget}
            {...eventHandlers}
        >
            <style>{scopedCss}</style>
            <div ref={htmlContentRef} className={styles.htmlContentRoot} />
            {shouldHydrateSlots ? (
                <HtmlSlotHydrator
                    containerRef={htmlContentRef as RefObject<HTMLElement | null>}
                    enabled={shouldHydrateSlots}
                    htmlRevision={sanitizedHtml}
                />
            ) : null}
        </div>
    );
}
