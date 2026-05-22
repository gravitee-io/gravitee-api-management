/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Shared portal-rendered modal primitive.
 *
 * <p>The first overlay in this module (`ScimConnectorsDialog`) had to be
 * portal-rendered with bespoke inline styles because the gamma-console host
 * shell's sidebar sits in a separate stacking context that the graphene
 * `Dialog`'s backdrop fails to cover — clicks would leak through the dimmed
 * area. Rather than reproduce that workaround per dialog (and have
 * subtly-different shells for each), `PortalModal` lifts the layout into a
 * single primitive every panel-style surface uses: SCIM connectors, the
 * entity detail sheet, policy creation, "add local principal", etc.
 *
 * <p>API is intentionally permissive on the header: pages that just need
 * `icon + title + description` pass strings; pages that need a rich shell
 * (an editable name input, view-toggle, save/deploy buttons) pass JSX into
 * `title` / `headerActions` instead. The {@link subHeader} slot is rendered
 * full-width between the header rule and the body — use it for banners
 * (deploy toast, target picker, description field) without breaking the
 * body's scroll container.
 *
 * <p>Behaviour: ESC closes; click-outside closes (the backdrop owns the
 * click event); body scrolls; the panel is locked at {@code 88vh} so a
 * busy editor doesn't push the page chrome off-screen.
 */
import { X } from 'lucide-react';
import { useEffect, type CSSProperties, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

export interface PortalModalProps {
    readonly open: boolean;
    readonly onOpenChange: (next: boolean) => void;
    /**
     * Accessible label. Required for screen-reader output. Pass the same
     * string you'd put in `title` when `title` is a string; pass a separate
     * human-readable label when `title` is JSX (e.g. an editable name input).
     */
    readonly ariaLabel: string;
    /**
     * Optional icon tile rendered at the start of the header (36×36, indigo
     * background). Omit for headers that own their own visual hierarchy
     * (e.g. PolicyEditor's editable-name layout).
     */
    readonly icon?: ReactNode;
    /**
     * Header title. Pass a string for the common case; pass JSX when the
     * header needs a richer shell (editable input, status pill, etc).
     */
    readonly title: ReactNode;
    /** Optional one-line description rendered under the title. */
    readonly description?: ReactNode;
    /**
     * Optional slot rendered at the right edge of the header — for action
     * buttons (Save / Deploy / view toggles). Aligns with the title block.
     */
    readonly headerActions?: ReactNode;
    /**
     * Optional full-width strip rendered between the header rule and the
     * body — for banners (deploy toast, target chip, description input)
     * that should not scroll with the body and should not split the panel
     * border.
     */
    readonly subHeader?: ReactNode;
    /** Body content. Scrolls when overflowing. */
    readonly children: ReactNode;
    /**
     * Optional fixed footer (Cancel / submit buttons). Rendered with a
     * top border + light grey background to anchor it visually.
     */
    readonly footer?: ReactNode;
    /**
     * Panel max-width. Defaults to `min(880px, 100%)`. The policy editor
     * uses a wider variant because of the embedded GAPL code editor.
     */
    readonly width?: string;
}

// ---------- styles (inline for stacking-context safety) ----------------------

const overlay: CSSProperties = {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(15, 23, 42, 0.55)',
    backdropFilter: 'blur(2px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '2rem',
    zIndex: 10000,
    animation: 'pm-fadein 120ms ease-out',
};
const panelBase: CSSProperties = {
    backgroundColor: '#fff',
    borderRadius: 12,
    maxHeight: '88vh',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
    boxShadow: '0 25px 70px -10px rgba(15,23,42,0.35), 0 10px 25px -5px rgba(15,23,42,0.18)',
    animation: 'pm-pop 160ms cubic-bezier(.2,.8,.2,1)',
};
const headerBar: CSSProperties = {
    padding: '1.25rem 1.5rem 1rem',
    borderBottom: '1px solid #e5e7eb',
    display: 'flex',
    alignItems: 'flex-start',
    gap: '0.75rem',
};
const headerIconBox: CSSProperties = {
    flex: 'none',
    width: 36,
    height: 36,
    borderRadius: 8,
    backgroundColor: '#eef2ff',
    color: '#4338ca',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
};
const headerTitleBlock: CSSProperties = {
    flex: 1,
    minWidth: 0,
};
const headerTitleText: CSSProperties = {
    fontSize: 16,
    fontWeight: 600,
    color: '#111827',
    lineHeight: 1.25,
};
const headerDescriptionText: CSSProperties = {
    fontSize: 13,
    color: '#6b7280',
    marginTop: 2,
    lineHeight: 1.4,
};
const headerActionsBox: CSSProperties = {
    flex: 'none',
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
};
const closeBtn: CSSProperties = {
    border: 'none',
    background: 'transparent',
    color: '#6b7280',
    cursor: 'pointer',
    padding: 6,
    borderRadius: 6,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
};
const bodyBox: CSSProperties = {
    padding: '1.25rem 1.5rem',
    overflowY: 'auto',
    flex: 1,
};
const footerBar: CSSProperties = {
    padding: '0.875rem 1.5rem',
    borderTop: '1px solid #e5e7eb',
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '0.5rem',
    backgroundColor: '#fafafa',
};

export function PortalModal({
    open,
    onOpenChange,
    ariaLabel,
    icon,
    title,
    description,
    headerActions,
    subHeader,
    children,
    footer,
    width,
}: PortalModalProps) {
    useEffect(() => {
        if (!open) return undefined;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onOpenChange(false);
        };
        document.addEventListener('keydown', onKey);
        return () => document.removeEventListener('keydown', onKey);
    }, [open, onOpenChange]);

    if (!open) return null;

    const panel: CSSProperties = {
        ...panelBase,
        width: width ?? 'min(880px, 100%)',
    };

    return createPortal(
        <>
            <style>{`
                @keyframes pm-fadein { from { opacity: 0; } to { opacity: 1; } }
                @keyframes pm-pop {
                    from { opacity: 0; transform: translateY(8px) scale(0.98); }
                    to   { opacity: 1; transform: translateY(0) scale(1); }
                }
            `}</style>
            <div role="presentation" onClick={() => onOpenChange(false)} style={overlay}>
                <div role="dialog" aria-modal="true" aria-label={ariaLabel} onClick={e => e.stopPropagation()} style={panel}>
                    <div style={headerBar}>
                        {icon ? <div style={headerIconBox}>{icon}</div> : null}
                        <div style={headerTitleBlock}>
                            {typeof title === 'string' ? <div style={headerTitleText}>{title}</div> : title}
                            {description ? (
                                typeof description === 'string' ? (
                                    <div style={headerDescriptionText}>{description}</div>
                                ) : (
                                    description
                                )
                            ) : null}
                        </div>
                        {headerActions ? <div style={headerActionsBox}>{headerActions}</div> : null}
                        <button
                            type="button"
                            onClick={() => onOpenChange(false)}
                            style={closeBtn}
                            aria-label="Close"
                            title="Close"
                        >
                            <X size={18} />
                        </button>
                    </div>

                    {subHeader ?? null}

                    <div style={bodyBox}>{children}</div>

                    {footer ? <div style={footerBar}>{footer}</div> : null}
                </div>
            </div>
        </>,
        document.body,
    );
}
