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
import { Button, ToggleGroup, ToggleGroupItem } from '@gravitee/graphene-core';
import { GioDeveloperPortalIcon } from '@gravitee/graphene-core/icons';

import type { PortalLayout } from '../../portals/types';
import type { PageWidth } from '../constants/page-width';
import type { EditorMode } from '../stores/editor.store';
import type { UsePortalThemeReturn } from '../../theming/hooks/usePortalTheme';
import { ThemeVariablesManager } from '../../theming/components/ThemeVariablesManager';
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
    readonly themeState?: UsePortalThemeReturn;
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
    themeState,
}: EditorHeaderProps) {
    const isEditMode = mode === 'edit';
    const [themeDialogOpen, setThemeDialogOpen] = useState(false);

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

                            {themeState && (
                                <Button size="sm" variant="outline" onClick={() => setThemeDialogOpen(true)}>
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" className="mr-1.5">
                                        <circle cx="13.5" cy="6.5" r="2.5" />
                                        <circle cx="17.5" cy="10.5" r="2.5" />
                                        <circle cx="8.5" cy="7.5" r="2.5" />
                                        <circle cx="6.5" cy="12.5" r="2.5" />
                                        <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.555-2.503 5.555-5.554C21.965 6.012 17.461 2 12 2z" />
                                    </svg>
                                    Theme
                                </Button>
                            )}

                            <Button size="sm" onClick={onSave} disabled={isSaving}>
                                Save
                            </Button>
                        </div>
                    )}
                </div>
            </div>

            {themeState && (
                <ThemeVariablesManager
                    open={themeDialogOpen}
                    onOpenChange={setThemeDialogOpen}
                    themeState={themeState}
                    portalName={portalName}
                />
            )}
        </header>
    );
}
