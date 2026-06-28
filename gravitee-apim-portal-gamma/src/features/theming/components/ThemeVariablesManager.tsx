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
import { useCallback, useRef, useState } from 'react';
import {
    Button,
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';

import type { UsePortalThemeReturn } from '../hooks/usePortalTheme';
import { downloadThemeCss } from '../utils/theme-export';
import { importThemeFromCss, readFileAsText } from '../utils/theme-import';
import { notify } from '../../../shared/notify/notify';
import { TokenEditor } from './TokenEditor';
import { CustomVariablesEditor } from './CustomVariablesEditor';
import styles from './ThemeVariablesManager.module.scss';

interface ThemeVariablesManagerProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly themeState: UsePortalThemeReturn;
    readonly portalName?: string;
}

type Tab = 'tokens' | 'variables';

export function ThemeVariablesManager({
    open,
    onOpenChange,
    themeState,
    portalName,
}: ThemeVariablesManagerProps) {
    const {
        theme,
        updateToken,
        addCustomVariable,
        updateCustomVariable,
        removeCustomVariable,
        replaceTheme,
        save,
        reset,
    } = themeState;

    const [activeTab, setActiveTab] = useState<Tab>('tokens');
    const [editingMode, setEditingMode] = useState<'light' | 'dark'>('light');
    const [saving, setSaving] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleSave = useCallback(async () => {
        setSaving(true);
        try {
            await save();
            notify.success('Theme saved');
            onOpenChange(false);
        } catch (error) {
            notify.error(error, 'Failed to save theme');
        } finally {
            setSaving(false);
        }
    }, [onOpenChange, save]);

    const handleExport = useCallback(() => {
        downloadThemeCss(theme, portalName);
    }, [theme, portalName]);

    const handleImport = useCallback(async (file: File) => {
        try {
            const css = await readFileAsText(file);
            const result = importThemeFromCss(css);
            if (!result.success || !result.theme) {
                notify.error(null, result.error ?? 'Import failed');
                return;
            }
            replaceTheme(result.theme);
            notify.success('Theme imported — review changes and save');
        } catch (error) {
            notify.error(error, 'Failed to import theme');
        }
    }, [replaceTheme]);

    const handleReset = useCallback(() => {
        reset();
        notify.success('Theme reset to defaults');
    }, [reset]);

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent
                className={styles.content}
                style={{ width: 'min(92vw, 72rem)', maxWidth: 'min(92vw, 72rem)', maxHeight: '85vh' }}
                showCloseButton={false}
            >
                <DialogHeader className={styles.header}>
                    <div className={styles.headerTop}>
                        <DialogTitle>Theme Editor</DialogTitle>
                        <div className={styles.headerActions}>
                            <Button size="sm" variant="outline" onClick={handleReset}>
                                Reset
                            </Button>
                            <Button size="sm" variant="outline" onClick={() => fileInputRef.current?.click()}>
                                Import
                            </Button>
                            <Button size="sm" variant="outline" onClick={handleExport}>
                                Export CSS
                            </Button>
                            <Button size="sm" onClick={() => void handleSave()} disabled={saving}>
                                {saving ? 'Saving…' : 'Save Theme'}
                            </Button>
                        </div>
                    </div>

                    <div className={styles.toolbar}>
                        <ToggleGroup
                            type="single"
                            variant="outline"
                            size="sm"
                            spacing={0}
                            value={activeTab}
                            onValueChange={v => { if (v) setActiveTab(v as Tab); }}
                            aria-label="Theme section"
                        >
                            <ToggleGroupItem value="tokens" aria-label="Design tokens">
                                Tokens
                            </ToggleGroupItem>
                            <ToggleGroupItem value="variables" aria-label="Custom variables">
                                Variables
                            </ToggleGroupItem>
                        </ToggleGroup>

                        {activeTab === 'tokens' && (
                            <ToggleGroup
                                type="single"
                                variant="outline"
                                size="sm"
                                spacing={0}
                                value={editingMode}
                                onValueChange={v => { if (v) setEditingMode(v as 'light' | 'dark'); }}
                                aria-label="Color mode"
                            >
                                <ToggleGroupItem value="light" aria-label="Light mode tokens">
                                    Light
                                </ToggleGroupItem>
                                <ToggleGroupItem value="dark" aria-label="Dark mode tokens">
                                    Dark
                                </ToggleGroupItem>
                            </ToggleGroup>
                        )}
                    </div>
                </DialogHeader>

                <div className={styles.body}>
                    {activeTab === 'tokens' && (
                        <TokenEditor
                            lightTokens={theme.tokens.light}
                            darkTokens={theme.tokens.dark}
                            editingMode={editingMode}
                            onUpdate={updateToken}
                        />
                    )}

                    {activeTab === 'variables' && (
                        <CustomVariablesEditor
                            variables={theme.customVariables}
                            onAdd={addCustomVariable}
                            onUpdate={updateCustomVariable}
                            onRemove={removeCustomVariable}
                        />
                    )}
                </div>

                <input
                    ref={fileInputRef}
                    type="file"
                    accept=".css"
                    className={styles.hidden}
                    onChange={e => {
                        const file = e.target.files?.[0];
                        if (file) void handleImport(file);
                        e.target.value = '';
                    }}
                />
            </DialogContent>
        </Dialog>
    );
}
