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
import type { FoundationTokens } from '../types';
import { ColorInput } from './ColorInput';
import styles from './TokenEditor.module.scss';

const COLOR_KEYS: (keyof FoundationTokens)[] = [
    'primary', 'primaryForeground', 'secondary', 'background', 'surface', 'text',
    'muted', 'mutedForeground', 'accent', 'border', 'ring', 'destructive', 'link',
];

const TYPOGRAPHY_KEYS: (keyof FoundationTokens)[] = [
    'fontFamily', 'headingFontFamily', 'fontSize', 'lineHeight',
];

const SPACING_KEYS: (keyof FoundationTokens)[] = ['borderRadius', 'borderWidth', 'padding'];

const LAYOUT_KEYS: (keyof FoundationTokens)[] = [
    'maxWidth', 'sidebarWidth', 'headerHeight', 'footerHeight',
];

function toLabel(key: string): string {
    return key.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/^./, c => c.toUpperCase());
}

interface FoundationTokenEditorProps {
    readonly tokens: FoundationTokens;
    readonly explicitTokens: Partial<FoundationTokens>;
    readonly editingMode: 'light' | 'dark';
    readonly onUpdate: (mode: 'light' | 'dark', key: keyof FoundationTokens, value: string) => void;
}

export function FoundationTokenEditor({ tokens, explicitTokens, editingMode, onUpdate }: FoundationTokenEditorProps) {
    return (
        <div className={styles.container}>
            <section className={styles.section}>
                <h3 className={styles.sectionTitle}>Colors</h3>
                <div className={styles.grid}>
                    {COLOR_KEYS.map(key => (
                        <div key={key} className={styles.field}>
                            <label className={styles.label}>{toLabel(key)}</label>
                            <ColorInput
                                value={tokens[key]}
                                onChange={v => onUpdate(editingMode, key, v)}
                                label={toLabel(key)}
                            />
                        </div>
                    ))}
                </div>
            </section>

            <section className={styles.section}>
                <h3 className={styles.sectionTitle}>Typography</h3>
                <div className={styles.grid}>
                    {TYPOGRAPHY_KEYS.map(key => (
                        <div key={key} className={styles.field}>
                            <label className={styles.label}>{toLabel(key)}</label>
                            <input
                                type="text"
                                className={styles.input}
                                value={tokens[key]}
                                onChange={e => onUpdate(editingMode, key, e.target.value)}
                            />
                        </div>
                    ))}
                </div>
            </section>

            <section className={styles.section}>
                <h3 className={styles.sectionTitle}>Spacing</h3>
                <div className={styles.grid}>
                    {SPACING_KEYS.map(key => (
                        <div key={key} className={styles.field}>
                            <label className={styles.label}>{toLabel(key)}</label>
                            <input
                                type="text"
                                className={styles.input}
                                value={explicitTokens[key] ?? ''}
                                placeholder={tokens[key]}
                                onChange={e => onUpdate(editingMode, key, e.target.value)}
                            />
                        </div>
                    ))}
                </div>
            </section>

            <section className={styles.section}>
                <h3 className={styles.sectionTitle}>Layout</h3>
                <div className={styles.grid}>
                    {LAYOUT_KEYS.map(key => (
                        <div key={key} className={styles.field}>
                            <label className={styles.label}>{toLabel(key)}</label>
                            <input
                                type="text"
                                className={styles.input}
                                value={explicitTokens[key] ?? ''}
                                placeholder={tokens[key]}
                                onChange={e => onUpdate(editingMode, key, e.target.value)}
                            />
                        </div>
                    ))}
                </div>
            </section>
        </div>
    );
}
