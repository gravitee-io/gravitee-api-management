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
import { useState } from 'react';

import {
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';

import type { PortalLayout } from '../../portals/types';
import type { PageWidth } from '../constants/page-width';
import { WidthSelector } from './WidthSelector';
import styles from './LayoutSelector.module.scss';

interface LayoutOption {
    readonly value: PortalLayout;
    readonly label: string;
    readonly description: string;
}

const layoutOptions: LayoutOption[] = [
    {
        value: 'header-content-footer',
        label: 'Header layout',
        description: 'Top navigation bar with a content area and footer. Best for traditional portal experiences.',
    },
    {
        value: 'sidebar-content',
        label: 'Sidebar layout',
        description: 'Persistent side navigation with a content area. Ideal for documentation-heavy portals.',
    },
];

interface LayoutSelectorProps {
    readonly value: PortalLayout;
    readonly onChange: (value: PortalLayout) => void;
    readonly pageWidth: PageWidth;
    readonly onPageWidthChange: (value: PageWidth) => void;
}

function HeaderLayoutSkeleton() {
    return (
        <div className={styles.skeleton} aria-hidden="true">
            <div className={styles.skeletonHeaderLayout}>
                <div className={styles.skeletonBar} />
                <div className={styles.skeletonContent} />
                <div className={`${styles.skeletonBar} ${styles.skeletonBarFooter}`} />
            </div>
        </div>
    );
}

function SidebarLayoutSkeleton() {
    return (
        <div className={styles.skeleton} aria-hidden="true">
            <div className={styles.skeletonSidebarLayout}>
                <div className={styles.skeletonSidebarNav} />
                <div className={styles.skeletonContentWide} />
            </div>
        </div>
    );
}

function LayoutSkeleton({ layout }: { readonly layout: PortalLayout }) {
    if (layout === 'header-content-footer') {
        return <HeaderLayoutSkeleton />;
    }

    return <SidebarLayoutSkeleton />;
}

export function LayoutSelector({ value, onChange, pageWidth, onPageWidthChange }: LayoutSelectorProps) {
    const [open, setOpen] = useState(false);

    const handleSelect = (layout: PortalLayout) => {
        onChange(layout);
    };

    return (
        <>
            <Button type="button" variant="outline" size="sm" aria-label="Portal layout" onClick={() => setOpen(true)}>
                Layout
            </Button>

            <Dialog open={open} onOpenChange={setOpen}>
                <DialogContent
                    className={styles.content}
                    style={{ width: 'min(92vw, 36rem)', maxWidth: 'min(92vw, 36rem)' }}
                >
                    <DialogHeader>
                        <DialogTitle>Layout settings</DialogTitle>
                        <DialogDescription>
                            Configure content width and how navigation is arranged across your portal.
                        </DialogDescription>
                    </DialogHeader>

                    <section className={styles.widthSection} aria-labelledby="layout-settings-width-label">
                        <div className={styles.widthSectionHeader}>
                            <h3 id="layout-settings-width-label" className={styles.widthSectionLabel}>
                                Content width
                            </h3>
                            <p className={styles.widthSectionDescription}>
                                Controls the maximum width of page content in the editor.
                            </p>
                        </div>
                        <WidthSelector value={pageWidth} onChange={onPageWidthChange} />
                    </section>

                    <div className={styles.layoutSection}>
                        <h3 className={styles.layoutSectionLabel}>Portal layout</h3>
                        <div className={styles.grid} role="listbox" aria-label="Portal layouts" aria-activedescendant={value}>
                        {layoutOptions.map(option => {
                            const isSelected = option.value === value;

                            return (
                                <button
                                    key={option.value}
                                    id={option.value}
                                    type="button"
                                    className={`${styles.tile} ${isSelected ? styles.tileSelected : ''}`}
                                    role="option"
                                    aria-selected={isSelected}
                                    onClick={() => handleSelect(option.value)}
                                >
                                    <LayoutSkeleton layout={option.value} />
                                    <span className={styles.label}>{option.label}</span>
                                    <span className={styles.description}>{option.description}</span>
                                </button>
                            );
                        })}
                        </div>
                    </div>
                </DialogContent>
            </Dialog>
        </>
    );
}
