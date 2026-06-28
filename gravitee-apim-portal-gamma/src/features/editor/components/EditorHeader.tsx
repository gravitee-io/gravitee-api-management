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
import { GioDeveloperPortalIcon } from '@gravitee/graphene-core/icons';

import type { PortalLayout } from '../../portals/types';
import type { PageWidth } from '../constants/page-width';
import type { EditorMode } from '../stores/editor.store';
import { InlineEdit } from '../../../shared/components/InlineEdit';
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
    readonly onPortalNameChange: (name: string) => void;
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
    onPortalNameChange,
    onSave,
}: EditorHeaderProps) {
    const isEditMode = mode === 'edit';

    return (
        <header className={styles.header}>
            <div className={styles.inner}>
                <div className={styles.titleGroup}>
                    <div className={styles.productTitle}>
                        <GioDeveloperPortalIcon className={styles.productIcon} aria-hidden="true" />
                        <span className={styles.productLabel}>Portal Designer</span>
                    </div>
                    <div className={styles.titleSeparator} aria-hidden="true" />
                    <InlineEdit
                        value={portalName}
                        editable={isEditMode}
                        activateOn="doubleClick"
                        className={styles.portalName}
                        ariaLabel="Portal name"
                        onChange={onPortalNameChange}
                    />
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
                        className={styles.modeToggle}
                    >
                        <ToggleGroupItem value="edit" aria-label="Edit mode">
                            Edit
                        </ToggleGroupItem>
                        <ToggleGroupItem value="preview" aria-label="Preview mode">
                            Preview
                        </ToggleGroupItem>
                    </ToggleGroup>
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
                </div>
            </div>
        </header>
    );
}
