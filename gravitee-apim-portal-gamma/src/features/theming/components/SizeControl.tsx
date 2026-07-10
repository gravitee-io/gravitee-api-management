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
import { useEffect, useState } from 'react';

import type { SizePreset } from '../registry/size-presets';
import { isSizePreset } from '../registry/size-presets';
import styles from './SizeControl.module.scss';

interface SizeControlProps {
    readonly value: string;
    readonly property: string;
    readonly presets?: readonly SizePreset[];
    readonly onChange: (value: string) => void;
    readonly compact?: boolean;
}

export function SizeControl({ value, presets, onChange, compact = false }: SizeControlProps) {
    const [preferCustom, setPreferCustom] = useState(false);
    const isCustomValue = value !== '' && !isSizePreset(value);
    const showCustom = isCustomValue || preferCustom || !presets;

    useEffect(() => {
        if (value === '' || isSizePreset(value)) {
            setPreferCustom(false);
        }
    }, [value]);

    if (showCustom) {
        return (
            <div className={`${styles.custom} ${compact ? styles.compact : ''}`}>
                <input
                    type="text"
                    className={styles.input}
                    value={value}
                    placeholder="e.g. 16px, 1rem"
                    onChange={e => onChange(e.target.value)}
                />
                {presets && (
                    <button
                        type="button"
                        className={styles.switchBtn}
                        onClick={() => {
                            setPreferCustom(false);
                            onChange('');
                        }}
                    >
                        Use presets
                    </button>
                )}
                {!compact && (
                    <p className={styles.warning}>Custom values may not be responsive across devices.</p>
                )}
            </div>
        );
    }

    return (
        <div className={`${styles.presets} ${compact ? styles.compact : ''}`}>
            <select
                className={styles.select}
                value={isSizePreset(value) ? value : ''}
                onChange={e => {
                    if (e.target.value === '__custom__') {
                        setPreferCustom(true);
                    } else {
                        onChange(e.target.value);
                    }
                }}
            >
                <option value="">Default</option>
                {presets?.map(p => (
                    <option key={p} value={p}>{p.toUpperCase()}</option>
                ))}
                <option value="__custom__">Custom…</option>
            </select>
        </div>
    );
}
