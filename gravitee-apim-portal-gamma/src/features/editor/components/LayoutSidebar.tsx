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
import { Label, Switch } from '@gravitee/graphene-core';

import type { PortalLayout } from '../../portals/types';
import type { PageWidth } from '../constants/page-width';
import { WidthSelector } from './WidthSelector';
import styles from './LayoutSidebar.module.scss';

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

interface LayoutSidebarProps {
    readonly value: PortalLayout;
    readonly onChange: (value: PortalLayout) => void;
    readonly pageWidth: PageWidth;
    readonly onPageWidthChange: (value: PageWidth) => void;
    readonly showFooter: boolean;
    readonly onShowFooterChange: (showFooter: boolean) => void;
    readonly className?: string;
}

function HeaderLayoutSkeleton({ showFooter }: { readonly showFooter: boolean }) {
    return (
        <div className={styles.skeleton} aria-hidden="true">
            <div className={styles.skeletonHeaderLayout}>
                <div className={styles.skeletonBar} />
                <div className={styles.skeletonContent} />
                {showFooter && <div className={`${styles.skeletonBar} ${styles.skeletonBarFooter}`} />}
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

function LayoutSkeleton({
    layout,
    showFooter,
}: {
    readonly layout: PortalLayout;
    readonly showFooter: boolean;
}) {
    if (layout === 'header-content-footer') {
        return <HeaderLayoutSkeleton showFooter={showFooter} />;
    }

    return <SidebarLayoutSkeleton />;
}

export function LayoutSidebar({
    value,
    onChange,
    pageWidth,
    onPageWidthChange,
    showFooter,
    onShowFooterChange,
    className,
}: LayoutSidebarProps) {
    const isHeaderLayout = value === 'header-content-footer';

    return (
        <aside className={`${styles.sidebar} ${className ?? ''}`} aria-label="Layout settings">
            <div className={styles.header}>
                <h2 className={styles.title}>Layout</h2>
                <p className={styles.description}>
                    Configure content width and how navigation is arranged across your portal.
                </p>
            </div>

            <div className={styles.body}>
                <section className={styles.widthSection} aria-labelledby="layout-settings-width-label">
                    <div className={styles.widthSectionHeader}>
                        <h3 id="layout-settings-width-label" className={styles.widthSectionLabel}>
                            Content width
                        </h3>
                        <p className={styles.widthSectionDescription}>
                            Controls the maximum width of page content across your portal.
                        </p>
                    </div>
                    <WidthSelector value={pageWidth} onChange={onPageWidthChange} />
                </section>

                <div className={styles.layoutSection}>
                    <h3 className={styles.layoutSectionLabel}>Navigation layout</h3>
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
                                    onClick={() => onChange(option.value)}
                                >
                                    <LayoutSkeleton
                                        layout={option.value}
                                        showFooter={option.value === 'header-content-footer' ? showFooter : false}
                                    />
                                    <span className={styles.label}>{option.label}</span>
                                    <span className={styles.description}>{option.description}</span>
                                </button>
                            );
                        })}
                    </div>
                </div>

                <section className={styles.footerSection} aria-labelledby="layout-settings-footer-label">
                    <div className={styles.footerSectionHeader}>
                        <Label htmlFor="layout-settings-show-footer" className={styles.footerSectionLabel}>
                            Show footer
                        </Label>
                        <p className={styles.footerSectionDescription}>
                            Display a footer with links at the bottom of the portal. Only available in header layout.
                        </p>
                    </div>
                    <Switch
                        id="layout-settings-show-footer"
                        checked={showFooter}
                        disabled={!isHeaderLayout}
                        onCheckedChange={onShowFooterChange}
                        aria-label="Show footer"
                    />
                </section>
            </div>
        </aside>
    );
}
