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
import { Button, ToggleGroup, ToggleGroupItem } from '@gravitee/graphene-core';

import type { UsePortalThemeReturn } from '../hooks/usePortalTheme';
import { POC_ELEMENT_REGISTRY, resolvePartCssVariant } from '../registry/element-registry';
import { buildElementVarName } from '../registry/var-names';
import { downloadThemeCss } from '../utils/theme-export';
import { importThemeFromCss, readFileAsText } from '../utils/theme-import';
import { notify } from '../../../shared/notify/notify';
import { FoundationTokenEditor } from './FoundationTokenEditor';
import { CustomVariablesEditor } from './CustomVariablesEditor';
import { ColorInput } from './ColorInput';
import { SizeControl } from './SizeControl';
import styles from './ThemeSidebar.module.scss';

interface ThemeSidebarProps {
    readonly themeState: UsePortalThemeReturn;
    readonly portalName?: string;
    readonly onHighlightVariable?: (varName: string) => void;
    readonly className?: string;
}

function CollapsibleSection({ title, children, defaultOpen = false }: {
    readonly title: string;
    readonly children: React.ReactNode;
    readonly defaultOpen?: boolean;
}) {
    const [open, setOpen] = useState(defaultOpen);
    return (
        <section className={styles.section}>
            <button type="button" className={styles.sectionHeader} onClick={() => setOpen(!open)}>
                <span>{title}</span>
                <span className={styles.chevron}>{open ? '▾' : '▸'}</span>
            </button>
            {open && <div className={styles.sectionBody}>{children}</div>}
        </section>
    );
}

export function ThemeSidebar({ themeState, portalName, onHighlightVariable, className }: ThemeSidebarProps) {
    const {
        theme,
        getResolvedFoundation,
        updateFoundationToken,
        updateElementToken,
        addCustomVariable,
        updateCustomVariable,
        removeCustomVariable,
        replaceTheme,
        save,
        reset,
    } = themeState;

    const [editingMode, setEditingMode] = useState<'light' | 'dark'>('light');
    const [saving, setSaving] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const foundation = getResolvedFoundation(editingMode);

    const handleSave = useCallback(async () => {
        setSaving(true);
        try {
            await save();
            notify.success('Theme saved');
        } catch (error) {
            notify.error(error, 'Failed to save theme');
        } finally {
            setSaving(false);
        }
    }, [save]);

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

    const getElementValue = (elementId: string, variant: string | undefined, prop: string): string => {
        const entry = theme.elements[elementId];
        if (!entry) return '';
        if (variant) {
            const variantEntry = (entry as Record<string, { light: Record<string, string>; dark: Record<string, string> }>)[variant];
            return variantEntry?.[editingMode]?.[prop] ?? '';
        }
        const direct = entry as { light: Record<string, string>; dark: Record<string, string> };
        return direct[editingMode]?.[prop] ?? '';
    };

    return (
        <aside className={`${styles.sidebar} ${className ?? ''}`} aria-label="Theme editor">
            <div className={styles.header}>
                <h2 className={styles.title}>Theme</h2>
                <div className={styles.headerActions}>
                    <Button size="sm" variant="outline" onClick={reset}>Reset</Button>
                    <Button size="sm" variant="outline" onClick={() => fileInputRef.current?.click()}>Import</Button>
                    <Button size="sm" variant="outline" onClick={() => downloadThemeCss(theme, portalName)}>Export</Button>
                    <Button size="sm" onClick={() => void handleSave()} disabled={saving}>
                        {saving ? 'Saving…' : 'Save'}
                    </Button>
                </div>
            </div>

            <ToggleGroup
                type="single"
                variant="outline"
                size="sm"
                spacing={0}
                value={editingMode}
                onValueChange={v => { if (v) setEditingMode(v as 'light' | 'dark'); }}
                aria-label="Color mode"
                className={styles.modeToggle}
            >
                <ToggleGroupItem value="light">Light</ToggleGroupItem>
                <ToggleGroupItem value="dark">Dark</ToggleGroupItem>
            </ToggleGroup>

            <div className={styles.body}>
                <CollapsibleSection title="Foundation">
                    <FoundationTokenEditor
                        tokens={foundation}
                        editingMode={editingMode}
                        onUpdate={updateFoundationToken}
                    />
                </CollapsibleSection>

                <CollapsibleSection title="Layout components">
                    {POC_ELEMENT_REGISTRY.filter(e => e.category === 'layout').map(element => (
                        <div key={element.id} className={styles.elementGroup}>
                            <h4 className={styles.elementTitle}>{element.label}</h4>
                            {element.parts ? (
                                element.parts.map(part => (
                                    <div key={part.id} className={styles.variantGroup}>
                                        <h5 className={styles.variantTitle}>{part.label}</h5>
                                        {Object.entries(part.properties).map(([prop, def]) => (
                                            <div key={prop} className={styles.tokenRow}>
                                                <label className={styles.tokenLabel}>{def.label}</label>
                                                {def.type === 'color' ? (
                                                    <ColorInput
                                                        value={getElementValue(element.id, part.id, prop)}
                                                        onChange={v => updateElementToken(element.id, editingMode, prop, v, part.id)}
                                                        label={def.label}
                                                    />
                                                ) : (
                                                    <SizeControl
                                                        value={getElementValue(element.id, part.id, prop)}
                                                        property={prop}
                                                        presets={def.sizePresets}
                                                        onChange={v => updateElementToken(element.id, editingMode, prop, v, part.id)}
                                                    />
                                                )}
                                                <code className={styles.fallbackHint}>
                                                    {buildElementVarName(element.id, resolvePartCssVariant(part.id), prop)}
                                                </code>
                                            </div>
                                        ))}
                                    </div>
                                ))
                            ) : (
                                Object.entries(element.properties).map(([prop, def]) => (
                                    <div key={prop} className={styles.tokenRow}>
                                        <label className={styles.tokenLabel}>{def.label}</label>
                                        {def.type === 'color' ? (
                                            <ColorInput
                                                value={getElementValue(element.id, undefined, prop)}
                                                onChange={v => updateElementToken(element.id, editingMode, prop, v)}
                                                label={def.label}
                                            />
                                        ) : (
                                            <SizeControl
                                                value={getElementValue(element.id, undefined, prop)}
                                                property={prop}
                                                presets={def.sizePresets}
                                                onChange={v => updateElementToken(element.id, editingMode, prop, v)}
                                            />
                                        )}
                                        <code className={styles.fallbackHint}>
                                            {buildElementVarName(element.id, undefined, prop)}
                                        </code>
                                    </div>
                                ))
                            )}
                        </div>
                    ))}
                </CollapsibleSection>

                <CollapsibleSection title="Components">
                    {POC_ELEMENT_REGISTRY.filter(e => e.category === 'component').map(element => (
                        <div key={element.id} className={styles.elementGroup}>
                            <h4 className={styles.elementTitle}>{element.label}</h4>
                            {(element.variants ?? [undefined]).map(variant => (
                                <div key={variant ?? 'default'} className={styles.variantGroup}>
                                    {variant && <h5 className={styles.variantTitle}>{variant}</h5>}
                                    {Object.entries(element.properties).map(([prop, def]) => (
                                        <div key={prop} className={styles.tokenRow}>
                                            <label className={styles.tokenLabel}>{def.label}</label>
                                            {def.type === 'color' ? (
                                                <ColorInput
                                                    value={getElementValue(element.id, variant, prop)}
                                                    onChange={v => updateElementToken(element.id, editingMode, prop, v, variant)}
                                                    label={def.label}
                                                />
                                            ) : (
                                                <SizeControl
                                                    value={getElementValue(element.id, variant, prop)}
                                                    property={prop}
                                                    presets={def.sizePresets}
                                                    onChange={v => updateElementToken(element.id, editingMode, prop, v, variant)}
                                                />
                                            )}
                                        </div>
                                    ))}
                                </div>
                            ))}
                        </div>
                    ))}
                </CollapsibleSection>

                <CollapsibleSection title="Custom variables">
                    <CustomVariablesEditor
                        variables={theme.customVariables}
                        onAdd={addCustomVariable}
                        onUpdate={updateCustomVariable}
                        onRemove={removeCustomVariable}
                        onHighlight={onHighlightVariable}
                    />
                </CollapsibleSection>
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
        </aside>
    );
}
