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
import { cn, ToggleGroup, ToggleGroupItem } from '@gravitee/graphene-core';
import { useState } from 'react';
import { Group, Panel, Separator } from 'react-resizable-panels';

import type { PageWidth } from '../editor/constants/page-width';
import { HtmlContentView } from './HtmlContentView';
import { HtmlCssEditor } from './HtmlCssEditor';
import { HtmlPageWidthFrame } from './HtmlPageWidthFrame';
import { HtmlSourceEditor } from './HtmlSourceEditor';
import styles from './HtmlEditorShell.module.scss';

export type HtmlEditorLayout = 'tabs' | 'split';
export type HtmlEditorTab = 'preview' | 'html' | 'css';

interface HtmlEditorShellProps {
    readonly html: string;
    readonly css: string;
    readonly scopeId: string;
    readonly layout?: HtmlEditorLayout;
    readonly onLayoutChange?: (layout: HtmlEditorLayout) => void;
    readonly followLayoutWidth?: boolean;
    readonly onFollowLayoutWidthChange?: (value: boolean) => void;
    readonly pageWidth?: PageWidth;
    readonly onHtmlChange: (value: string) => void;
    readonly onCssChange: (value: string) => void;
    readonly isolateBlockNoteEvents?: boolean;
    readonly className?: string;
}

function WidthToggle({
    followLayoutWidth,
    onFollowLayoutWidthChange,
}: {
    readonly followLayoutWidth: boolean;
    readonly onFollowLayoutWidthChange: (value: boolean) => void;
}) {
    return (
        <ToggleGroup
            type="single"
            variant="outline"
            size="sm"
            spacing={0}
            value={followLayoutWidth ? 'layout' : 'full'}
            onValueChange={value => {
                if (value === 'full' || value === 'layout') {
                    onFollowLayoutWidthChange(value === 'layout');
                }
            }}
            aria-label="Page width"
        >
            <ToggleGroupItem value="full">Full width</ToggleGroupItem>
            <ToggleGroupItem value="layout">Layout width</ToggleGroupItem>
        </ToggleGroup>
    );
}

function LayoutToggle({
    layout,
    onLayoutChange,
}: {
    readonly layout: HtmlEditorLayout;
    readonly onLayoutChange: (layout: HtmlEditorLayout) => void;
}) {
    return (
        <ToggleGroup
            type="single"
            variant="outline"
            size="sm"
            spacing={0}
            value={layout}
            onValueChange={value => {
                if (value === 'split' || value === 'tabs') {
                    onLayoutChange(value);
                }
            }}
            aria-label="Editor layout"
        >
            <ToggleGroupItem value="split">Split</ToggleGroupItem>
            <ToggleGroupItem value="tabs">Tabs</ToggleGroupItem>
        </ToggleGroup>
    );
}

export function HtmlEditorShell({
    html,
    css,
    scopeId,
    layout = 'split',
    onLayoutChange,
    followLayoutWidth = false,
    onFollowLayoutWidthChange,
    pageWidth = 'narrow',
    onHtmlChange,
    onCssChange,
    isolateBlockNoteEvents = false,
    className,
}: HtmlEditorShellProps) {
    const [activeTab, setActiveTab] = useState<HtmlEditorTab>('preview');

    const widthToggle = onFollowLayoutWidthChange ? (
        <WidthToggle followLayoutWidth={followLayoutWidth} onFollowLayoutWidthChange={onFollowLayoutWidthChange} />
    ) : null;

    const layoutToggle = onLayoutChange ? (
        <LayoutToggle layout={layout} onLayoutChange={onLayoutChange} />
    ) : null;

    const headerActions =
        widthToggle || layoutToggle ? (
            <div className={styles.headerActions}>
                {widthToggle}
                {layoutToggle}
            </div>
        ) : null;

    const previewContent = (
        <HtmlContentView
            html={html}
            css={css}
            scopeId={scopeId}
            isolateBlockNoteEvents={isolateBlockNoteEvents}
            className={layout === 'split' ? styles.splitPreviewContent : undefined}
            styleTarget={onFollowLayoutWidthChange ? 'html-page' : 'html-block'}
        />
    );

    const framedPreview = onFollowLayoutWidthChange ? (
        <HtmlPageWidthFrame
            followLayoutWidth={followLayoutWidth}
            pageWidth={pageWidth}
            className={layout === 'split' ? styles.splitPreviewFrame : undefined}
        >
            {previewContent}
        </HtmlPageWidthFrame>
    ) : (
        previewContent
    );

    if (layout === 'split') {
        return (
            <Group orientation="horizontal" className={cn(styles.splitLayout, className)}>
                <Panel defaultSize={33} minSize={15} className={styles.splitPanel}>
                    <Group orientation="vertical" className={styles.splitInputPane}>
                        <Panel defaultSize={50} minSize={15} className={styles.splitPanel}>
                            <div className={styles.splitSection}>
                                <div className={styles.splitSectionLabel}>HTML</div>
                                <HtmlSourceEditor value={html} onChange={onHtmlChange} fill />
                            </div>
                        </Panel>
                        <Separator className={styles.resizeHandleRow} />
                        <Panel defaultSize={50} minSize={15} className={styles.splitPanel}>
                            <div className={styles.splitSection}>
                                <div className={styles.splitSectionLabel}>CSS</div>
                                <HtmlCssEditor value={css} onChange={onCssChange} fill />
                            </div>
                        </Panel>
                    </Group>
                </Panel>
                <Separator className={styles.resizeHandleCol} />
                <Panel defaultSize={67} minSize={25} className={styles.splitPanel}>
                    <div className={styles.splitPreviewPane}>
                        <div className={styles.splitPreviewHeader}>
                            <div className={styles.splitSectionLabel}>Preview</div>
                            {headerActions}
                        </div>
                        <div className={styles.splitPreviewBody}>{framedPreview}</div>
                    </div>
                </Panel>
            </Group>
        );
    }

    const tabs: { key: HtmlEditorTab; label: string }[] = [
        { key: 'preview', label: 'Preview' },
        { key: 'html', label: 'HTML' },
        { key: 'css', label: 'CSS' },
    ];

    return (
        <div className={cn(styles.tabLayout, className)}>
            <div className={styles.tabs}>
                <div className={styles.tabList}>
                    {tabs.map(tab => (
                        <button
                            key={tab.key}
                            className={`${styles.tab} ${activeTab === tab.key ? styles.activeTab : ''}`}
                            onClick={() => setActiveTab(tab.key)}
                            type="button"
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>
                {headerActions}
            </div>
            <div className={styles.tabBody}>
                {activeTab === 'html' && <HtmlSourceEditor value={html} onChange={onHtmlChange} />}
                {activeTab === 'css' && <HtmlCssEditor value={css} onChange={onCssChange} />}
                {activeTab === 'preview' && framedPreview}
            </div>
        </div>
    );
}
