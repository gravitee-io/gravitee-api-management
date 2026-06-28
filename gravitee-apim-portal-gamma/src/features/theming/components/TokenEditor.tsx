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
import type { ThemeTokens, ThemeTokenCategory } from '../types';
import { ColorInput } from './ColorInput';
import styles from './TokenEditor.module.scss';

function toLabel(key: string): string {
    return key.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/^./, c => c.toUpperCase());
}

interface TokenEditorProps {
    readonly lightTokens: ThemeTokens;
    readonly darkTokens: ThemeTokens;
    readonly editingMode: 'light' | 'dark';
    readonly onUpdate: <C extends ThemeTokenCategory>(
        mode: 'light' | 'dark',
        category: C,
        key: keyof ThemeTokens[C],
        value: string | number,
    ) => void;
}

export function TokenEditor({ lightTokens, darkTokens, editingMode, onUpdate }: TokenEditorProps) {
    const tokens = editingMode === 'light' ? lightTokens : darkTokens;

    return (
        <div className={styles.container}>
            <section className={styles.section}>
                <h3 className={styles.sectionTitle}>Colors</h3>
                <div className={styles.grid}>
                    {Object.entries(tokens.colors).map(([key, value]) => (
                        <div key={key} className={styles.field}>
                            <label className={styles.label}>{toLabel(key)}</label>
                            <ColorInput
                                value={value}
                                onChange={v => onUpdate(editingMode, 'colors', key as keyof ThemeTokens['colors'], v)}
                                label={toLabel(key)}
                            />
                        </div>
                    ))}
                </div>
            </section>

            <section className={styles.section}>
                <h3 className={styles.sectionTitle}>Typography</h3>
                <div className={styles.grid}>
                    {Object.entries(tokens.typography).map(([key, value]) => (
                        <div key={key} className={styles.field}>
                            <label className={styles.label}>{toLabel(key)}</label>
                            <input
                                type={key === 'headingScale' ? 'number' : 'text'}
                                className={styles.input}
                                value={value}
                                step={key === 'headingScale' ? 0.05 : undefined}
                                onChange={e => {
                                    const v = key === 'headingScale' ? Number(e.target.value) : e.target.value;
                                    onUpdate(editingMode, 'typography', key as keyof ThemeTokens['typography'], v);
                                }}
                            />
                        </div>
                    ))}
                </div>
            </section>

            <section className={styles.section}>
                <h3 className={styles.sectionTitle}>Spacing</h3>
                <div className={styles.grid}>
                    {Object.entries(tokens.spacing).map(([key, value]) => (
                        <div key={key} className={styles.field}>
                            <label className={styles.label}>{toLabel(key)}</label>
                            <input
                                type="text"
                                className={styles.input}
                                value={value}
                                onChange={e => onUpdate(editingMode, 'spacing', key as keyof ThemeTokens['spacing'], e.target.value)}
                            />
                        </div>
                    ))}
                </div>
            </section>

            <section className={styles.section}>
                <h3 className={styles.sectionTitle}>Layout</h3>
                <div className={styles.grid}>
                    {Object.entries(tokens.layout).map(([key, value]) => (
                        <div key={key} className={styles.field}>
                            <label className={styles.label}>{toLabel(key)}</label>
                            <input
                                type="text"
                                className={styles.input}
                                value={value}
                                onChange={e => onUpdate(editingMode, 'layout', key as keyof ThemeTokens['layout'], e.target.value)}
                            />
                        </div>
                    ))}
                </div>
            </section>
        </div>
    );
}
