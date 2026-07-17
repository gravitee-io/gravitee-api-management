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
import { type MouseEvent, type RefObject, useCallback, useLayoutEffect, useMemo, useRef } from 'react';

import { usePortalPageOptional } from '../portal-shell/context/PortalPageContext';
import { usePortalPageNavigation } from '../portal-shell/hooks/usePortalPageNavigation';
import { isExternalUrl } from '../portal-shell/utils/link-target';
import { getPortalPages } from '../portal-shell/utils/portal-pages';
import { resolvePortalHtmlLink } from '../portal-shell/utils/user-menu-url';
import { findNavItemBySlug } from '../portals/utils/slug';
import { HtmlSlotHydrator } from './hydrate-slots';
import { rewritePortalHtmlLinks } from './rewrite-portal-html-links';
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

function isModifiedClick(event: MouseEvent): boolean {
    return event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0;
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
    const portalPage = usePortalPageOptional();
    const { navigateToPageSlug, getPagePath } = usePortalPageNavigation(portalPage?.portalId);
    const portalPages = useMemo(
        () => (portalPage ? getPortalPages(portalPage.navItems) : []),
        [portalPage],
    );
    const scopedCss = useMemo(
        () => scopeCustomCss(css, `[data-block-scope="${scopeId}"]`),
        [css, scopeId],
    );
    const sanitizedHtml = useMemo(() => sanitizePortalHtml(html), [html]);
    const renderedHtml = useMemo(() => {
        if (!portalPage) {
            return sanitizedHtml;
        }

        return rewritePortalHtmlLinks(
            sanitizedHtml,
            portalPages,
            getPagePath,
            portalPage.portalId,
        );
    }, [sanitizedHtml, portalPage, portalPages, getPagePath]);

    useLayoutEffect(() => {
        if (htmlContentRef.current) {
            htmlContentRef.current.innerHTML = renderedHtml;
        }
    }, [renderedHtml]);

    const handleContentClick = useCallback(
        (event: MouseEvent<HTMLDivElement>) => {
            if (!portalPage || isModifiedClick(event)) {
                return;
            }

            const anchor = (event.target as Element).closest('a[href]');
            if (!anchor || anchor.getAttribute('target') === '_blank') {
                return;
            }

            const href = anchor.getAttribute('href') ?? '';
            if (!href || isExternalUrl(href) || href.startsWith('#')) {
                return;
            }

            const resolved = resolvePortalHtmlLink(href, portalPages, getPagePath, portalPage.portalId);
            if (!resolved) {
                return;
            }

            event.preventDefault();
            const targetPage = findNavItemBySlug(portalPage.navItems, resolved.slug);
            if (targetPage?.type === 'PAGE' && portalPage.onSelectNavItem) {
                portalPage.onSelectNavItem(targetPage.id);
                return;
            }

            navigateToPageSlug(resolved.slug);
        },
        [getPagePath, navigateToPageSlug, portalPage, portalPages],
    );

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
            <div
                ref={htmlContentRef}
                className={styles.htmlContentRoot}
                onClickCapture={handleContentClick}
            />
            {shouldHydrateSlots ? (
                <HtmlSlotHydrator
                    containerRef={htmlContentRef as RefObject<HTMLElement | null>}
                    enabled={shouldHydrateSlots}
                    htmlRevision={renderedHtml}
                />
            ) : null}
        </div>
    );
}
