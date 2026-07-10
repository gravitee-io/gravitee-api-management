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

import type { SizePreset } from '../registry/size-presets';
import { isSizePreset } from '../registry/size-presets';
import styles from './SizeControl.module.scss';

interface SizeControlProps {
    readonly value: string;
    readonly property: string;
    readonly presets?: readonly SizePreset[];
    readonly onChange: (value: string) => void;
}

export function SizeControl({ value, property, presets, onChange }: SizeControlProps) {
    const [customMode, setCustomMode] = useState(!isSizePreset(value) && value !== '');

    if (customMode || !presets) {
        return (
            <div className={styles.custom}>
                <input
                    type="text"
                    className={styles.input}
                    value={value}
                    placeholder="e.g. 16px, 1rem"
                    onChange={e => onChange(e.target.value)}
                />
                {presets && (
                    <button type="button" className={styles.switchBtn} onClick={() => setCustomMode(false)}>
                        Use presets
                    </button>
                )}
                <p className={styles.warning}>Custom values may not be responsive across devices.</p>
            </div>
        );
    }

    return (
        <div className={styles.presets}>
            <select
                className={styles.select}
                value={isSizePreset(value) ? value : ''}
                onChange={e => {
                    if (e.target.value === '__custom__') {
                        setCustomMode(true);
                    } else {
                        onChange(e.target.value);
                    }
                }}
            >
                <option value="">Default</option>
                {presets.map(p => (
                    <option key={p} value={p}>{p.toUpperCase()}</option>
                ))}
                <option value="__custom__">Custom…</option>
            </select>
        </div>
    );
}
