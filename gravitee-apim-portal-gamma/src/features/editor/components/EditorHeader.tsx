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
import { Button, ToggleGroup, ToggleGroupItem } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { Link } from 'react-router-dom';

import type { PortalLayout } from '../../portals/types';
import type { PageWidth } from '../constants/page-width';
import type { EditorMode } from '../stores/editor.store';
import { LayoutSelector } from './LayoutSelector';
import { WidthSelector } from './WidthSelector';
import styles from './EditorHeader.module.scss';

interface EditorHeaderProps {
    readonly portalName: string;
    readonly mode: EditorMode;
    readonly pageWidth: PageWidth;
    readonly layout: PortalLayout;
    readonly isSaving: boolean;
    readonly onModeChange: (mode: EditorMode) => void;
    readonly onPageWidthChange: (pageWidth: PageWidth) => void;
    readonly onLayoutChange: (layout: PortalLayout) => void;
    readonly onSave: () => void;
}

export function EditorHeader({
    portalName,
    mode,
    pageWidth,
    layout,
    isSaving,
    onModeChange,
    onPageWidthChange,
    onLayoutChange,
    onSave,
}: EditorHeaderProps) {
    const isEditMode = mode === 'edit';

    return (
        <header className={styles.header}>
            <div className={styles.inner}>
                <div className={styles.titleGroup}>
                    <Button variant="ghost" size="sm" className={styles.backLink} asChild>
                        <Link to="/">
                            <ArrowLeftIcon className="size-4" aria-hidden="true" />
                            Back to dashboards
                        </Link>
                    </Button>
                    <span className={styles.portalName}>{portalName}</span>
                    <span className={styles.modeBadge}>{isEditMode ? 'Editor' : 'Preview'}</span>
                </div>

                <div className={styles.controls}>
                    {isEditMode && (
                        <div className='flex items-center gap-6'>
                            <WidthSelector value={pageWidth} onChange={onPageWidthChange} />
                            <LayoutSelector value={layout} onChange={onLayoutChange} />

                            <Button size="sm" onClick={onSave} disabled={isSaving}>
                                Save
                            </Button>
                        </div>
                    )}

                    <ToggleGroup
                        type="single"
                        variant="outline"
                        size="sm"
                        spacing={0}
                        value={mode}
                        onValueChange={nextValue => {
                            if (nextValue === 'edit' || nextValue === 'preview') {
                                onModeChange(nextValue);
                            }
                        }}
                        aria-label="Editor mode"
                    >
                        <ToggleGroupItem value="edit" aria-label="Edit mode">
                            Edit
                        </ToggleGroupItem>
                        <ToggleGroupItem value="preview" aria-label="Preview mode">
                            Preview
                        </ToggleGroupItem>
                    </ToggleGroup>
                </div>
            </div>
        </header>
    );
}
