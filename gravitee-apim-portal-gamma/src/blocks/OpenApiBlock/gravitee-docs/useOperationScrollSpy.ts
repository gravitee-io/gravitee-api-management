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
import { useCallback, useEffect, useRef, useState, type RefObject } from 'react';

import {
    findOperationSectionElement,
    getDocsScrollContainer,
    getDocsStickyHeaderOffset,
    scrollOperationSectionIntoView,
} from './gravitee-docs-utils';

const CLICK_SCROLL_LOCK_MS = 800;

interface UseOperationScrollSpyOptions {
    readonly operationIds: readonly string[];
    readonly defaultOperationId?: string;
    readonly docsRootRef: RefObject<HTMLElement | null>;
}

export function useOperationScrollSpy({
    operationIds,
    defaultOperationId,
    docsRootRef,
}: UseOperationScrollSpyOptions) {
    const [activeOperationId, setActiveOperationId] = useState(
        defaultOperationId ?? operationIds[0] ?? '',
    );
    const visibleSectionsRef = useRef<Map<string, IntersectionObserverEntry>>(new Map());
    const isClickScrollingRef = useRef(false);
    const clickScrollTimeoutRef = useRef<number | null>(null);

    useEffect(() => {
        const docsRoot = docsRootRef.current;
        if (!docsRoot || operationIds.length === 0) {
            return undefined;
        }

        const scrollContainer = getDocsScrollContainer(docsRoot);
        const headerOffset = getDocsStickyHeaderOffset(docsRoot);

        const pickTopmostVisible = () => {
            const sorted = [...visibleSectionsRef.current.entries()].sort(
                (left, right) => left[1].boundingClientRect.top - right[1].boundingClientRect.top,
            );
            const nextId = sorted[0]?.[0];
            if (nextId) {
                setActiveOperationId(nextId);
            }
        };

        const observer = new IntersectionObserver(
            entries => {
                for (const entry of entries) {
                    const operationId = entry.target.getAttribute('data-operation-id');
                    if (!operationId) {
                        continue;
                    }
                    if (entry.isIntersecting) {
                        visibleSectionsRef.current.set(operationId, entry);
                    } else {
                        visibleSectionsRef.current.delete(operationId);
                    }
                }

                if (isClickScrollingRef.current) {
                    return;
                }

                pickTopmostVisible();
            },
            {
                root: scrollContainer,
                rootMargin: `-${Math.max(headerOffset, 1)}px 0px -55% 0px`,
                threshold: [0, 0.1, 0.25, 0.5, 0.75, 1],
            },
        );

        for (const operationId of operationIds) {
            const element = findOperationSectionElement(docsRoot, operationId);
            if (element) {
                observer.observe(element);
            }
        }

        return () => {
            observer.disconnect();
            visibleSectionsRef.current.clear();
        };
    }, [docsRootRef, operationIds]);

    const scrollToOperation = useCallback(
        (operationId: string) => {
            const docsRoot = docsRootRef.current;
            const element = findOperationSectionElement(docsRoot, operationId);
            if (!element) {
                return;
            }

            if (clickScrollTimeoutRef.current !== null) {
                window.clearTimeout(clickScrollTimeoutRef.current);
            }

            isClickScrollingRef.current = true;
            setActiveOperationId(operationId);

            const scrollContainer = docsRoot ? getDocsScrollContainer(docsRoot) : null;
            requestAnimationFrame(() => {
                scrollOperationSectionIntoView(element, scrollContainer);
            });

            clickScrollTimeoutRef.current = window.setTimeout(() => {
                isClickScrollingRef.current = false;
                clickScrollTimeoutRef.current = null;
            }, CLICK_SCROLL_LOCK_MS);
        },
        [docsRootRef],
    );

    useEffect(
        () => () => {
            if (clickScrollTimeoutRef.current !== null) {
                window.clearTimeout(clickScrollTimeoutRef.current);
            }
        },
        [],
    );

    return { activeOperationId, scrollToOperation };
}
