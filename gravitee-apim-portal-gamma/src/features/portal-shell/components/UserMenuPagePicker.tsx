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
import { Input } from '@gravitee/graphene-core';
import { useEffect, useMemo, useRef, useState } from 'react';

import type { PortalNavigationPage } from '../../portals/types';
import { getNavTypeIcon } from '../utils/nav-type-icons';
import styles from './UserMenuPagePicker.module.scss';

interface UserMenuPagePickerProps {
    readonly pages: readonly PortalNavigationPage[];
    readonly onSelect: (page: PortalNavigationPage) => void;
    readonly onCancel: () => void;
}

export function UserMenuPagePicker({ pages, onSelect, onCancel }: UserMenuPagePickerProps) {
    const [query, setQuery] = useState('');
    const [activeIndex, setActiveIndex] = useState(0);
    const inputRef = useRef<HTMLInputElement>(null);

    const filteredPages = useMemo(() => {
        const normalizedQuery = query.trim().toLowerCase();
        if (!normalizedQuery) {
            return [...pages];
        }

        return pages.filter(page => page.title.toLowerCase().includes(normalizedQuery));
    }, [pages, query]);

    useEffect(() => {
        const frameId = requestAnimationFrame(() => {
            inputRef.current?.focus();
        });

        return () => cancelAnimationFrame(frameId);
    }, []);

    useEffect(() => {
        setActiveIndex(0);
    }, [query]);

    const selectPage = (page: PortalNavigationPage) => {
        onSelect(page);
    };

    return (
        <div className={styles.picker}>
            <Input
                ref={inputRef}
                className={styles.search}
                placeholder="Search for a page"
                value={query}
                aria-label="Search for a page"
                onChange={event => setQuery(event.target.value)}
                onKeyDown={event => {
                    if (event.key === 'Escape') {
                        event.preventDefault();
                        event.stopPropagation();
                        onCancel();
                        return;
                    }

                    if (event.key === 'ArrowDown') {
                        event.preventDefault();
                        setActiveIndex(index => Math.min(index + 1, Math.max(filteredPages.length - 1, 0)));
                        return;
                    }

                    if (event.key === 'ArrowUp') {
                        event.preventDefault();
                        setActiveIndex(index => Math.max(index - 1, 0));
                        return;
                    }

                    if (event.key === 'Enter' && filteredPages[activeIndex]) {
                        event.preventDefault();
                        selectPage(filteredPages[activeIndex]);
                    }
                }}
            />

            <div className={styles.list} role="listbox" aria-label="Portal pages">
                {filteredPages.length === 0 ? (
                    <p className={styles.emptyState}>No pages found</p>
                ) : (
                    filteredPages.map((page, index) => (
                        <button
                            key={page.id}
                            type="button"
                            role="option"
                            aria-selected={index === activeIndex}
                            className={`${styles.pageOption} ${index === activeIndex ? styles.pageOptionActive : ''}`}
                            onMouseEnter={() => setActiveIndex(index)}
                            onClick={() => selectPage(page)}
                        >
                            <span className={styles.pageIcon} aria-hidden="true">
                                {getNavTypeIcon('PAGE')}
                            </span>
                            <span className={styles.pageTitle}>{page.title}</span>
                        </button>
                    ))
                )}
            </div>
        </div>
    );
}
