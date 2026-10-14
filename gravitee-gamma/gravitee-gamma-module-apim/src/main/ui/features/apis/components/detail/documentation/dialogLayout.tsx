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
import { Alert, AlertDescription } from '@gravitee/graphene-core';
import { AlertCircleIcon } from '@gravitee/graphene-core/icons';
import { type CSSProperties, type ReactNode, useEffect, useRef } from 'react';

/**
 * Graphene's dialog grows with its content, and a source's settings can make it taller than the window, pushing its
 * actions out of reach. Capped at the window, it lays its header, body and footer out in a column for the body alone to
 * scroll ({@link ScrollableDialogBody}). Inline, as graphene ships no viewport-based max-height utility.
 *
 * A select's options open inside the dialog, positioned against the viewport. So the dialog is centered by margins
 * rather than by graphene's translate, which would make it their reference and push them aside by the dialog's offset,
 * and it does not clip its overflow, which would scroll it sideways to bring them into view.
 */
export const WITHIN_VIEWPORT: CSSProperties = {
    maxHeight: 'calc(100dvh - 2rem)',
    display: 'flex',
    flexDirection: 'column',
    translate: 'none',
    inset: 0,
    margin: 'auto',
    height: 'fit-content',
};

/**
 * The part of a dialog capped {@link WITHIN_VIEWPORT} that scrolls, keeping the title, the close button and the actions
 * in view. It reaches into the dialog's padding so that focus rings at its edges are not cut off, and so that a switch
 * at its bottom, whose click area reaches 8px past it, does not make it scroll. Inline, as graphene ships no -my-1.
 */
export function ScrollableDialogBody({ children }: Readonly<{ children: ReactNode }>) {
    return (
        <div className="-mx-4 min-h-0 overflow-y-auto px-4 py-2" style={{ marginBlock: '-0.25rem' }}>
            {children}
        </div>
    );
}

/**
 * Why the server refused what was entered, shown above the form and brought into view, as a scrolled dialog body can
 * hold it out of sight. Clearing it before each submit lets the same refusal given again be brought into view again.
 */
export function RefusalAlert({ refusal }: Readonly<{ refusal: string | null }>) {
    const ref = useRevealWhenShown<HTMLDivElement>(refusal);
    if (!refusal) return null;
    return (
        <Alert ref={ref} variant="destructive">
            <AlertCircleIcon aria-hidden />
            <AlertDescription>{refusal}</AlertDescription>
        </Alert>
    );
}

/** Scrolls the element into view each time `shown` turns truthy. */
function useRevealWhenShown<T extends HTMLElement>(shown: unknown) {
    const ref = useRef<T>(null);
    useEffect(() => {
        if (shown) ref.current?.scrollIntoView?.({ block: 'nearest' });
    }, [shown]);
    return ref;
}
