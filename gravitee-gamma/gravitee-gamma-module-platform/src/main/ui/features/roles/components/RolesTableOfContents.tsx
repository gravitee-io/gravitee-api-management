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
import { cn } from '@gravitee/graphene-core';
import { useEffect, useState, type MouseEvent } from 'react';

import type { RoleScope } from '../types/role';
import { roleSectionId } from '../utils/roleSectionId';

export interface RolesTableOfContentsItem {
    readonly scope: RoleScope;
    readonly label: string;
}

// No shell header-height design token exists to reference yet; centralizing the pixel value here (rather
// than inline in the IntersectionObserver options below) documents what it stands for and gives future
// callers one place to update it if the shell header's height changes.
const SHELL_HEADER_HEIGHT_PX = 96;

/**
 * Sticky jump-nav for the scope cards on RolesPage — mirrors the Angular reference's
 * <gio-table-of-contents>: one link per scope, highlighting whichever card is currently in view.
 */
export function RolesTableOfContents({ items }: Readonly<{ items: readonly RolesTableOfContentsItem[] }>) {
    const [activeScope, setActiveScope] = useState<RoleScope | undefined>(items[0]?.scope);

    useEffect(() => {
        if (typeof IntersectionObserver === 'undefined') return;

        const sections = items
            .map(item => ({ scope: item.scope, element: document.getElementById(roleSectionId(item.scope)) }))
            .filter((entry): entry is { scope: RoleScope; element: HTMLElement } => entry.element !== null);
        if (sections.length === 0) return;

        const intersecting = new Set<RoleScope>();
        const observer = new IntersectionObserver(
            entries => {
                entries.forEach(entry => {
                    const scope = sections.find(section => section.element === entry.target)?.scope;
                    if (!scope) return;
                    if (entry.isIntersecting) {
                        intersecting.add(scope);
                    } else {
                        intersecting.delete(scope);
                    }
                });
                const topMost = sections.find(section => intersecting.has(section.scope));
                if (topMost) setActiveScope(topMost.scope);
            },
            { rootMargin: `-${SHELL_HEADER_HEIGHT_PX}px 0px -70% 0px`, threshold: 0 },
        );
        sections.forEach(section => observer.observe(section.element));
        return () => observer.disconnect();
    }, [items]);

    function handleClick(scope: RoleScope, event: MouseEvent<HTMLAnchorElement>) {
        event.preventDefault();
        setActiveScope(scope);
        document.getElementById(roleSectionId(scope))?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    if (items.length === 0) return null;

    return (
        <nav aria-label="Roles sections" className="sticky top-4 w-44 shrink-0 self-start">
            <ul className="space-y-0.5 border-l">
                {items.map(item => {
                    const isActive = activeScope === item.scope;
                    return (
                        <li key={item.scope}>
                            <a
                                href={`#${roleSectionId(item.scope)}`}
                                aria-current={isActive ? 'location' : undefined}
                                onClick={event => handleClick(item.scope, event)}
                                className={cn(
                                    '-ml-px block truncate border-l-2 px-3 py-1.5 text-sm transition-colors',
                                    isActive
                                        ? 'border-primary font-medium text-primary'
                                        : 'border-transparent text-muted-foreground hover:text-foreground',
                                )}
                            >
                                {item.label}
                            </a>
                        </li>
                    );
                })}
            </ul>
        </nav>
    );
}
