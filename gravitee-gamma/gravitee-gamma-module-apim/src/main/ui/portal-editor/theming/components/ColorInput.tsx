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
import { useCallback, useRef, type CSSProperties } from 'react';

import styles from './ColorInput.module.scss';

interface ColorInputProps {
    readonly value: string;
    readonly onChange: (value: string) => void;
    readonly label?: string;
}

function isTransparent(value: string): boolean {
    return value.trim().toLowerCase() === 'transparent';
}

function toPickerHex(value: string): string {
    return /^#[0-9a-f]{6}$/i.test(value) ? value : '#000000';
}

export function ColorInput({ value, onChange, label }: ColorInputProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const transparent = isTransparent(value);
    const pickerHex = toPickerHex(value);

    const handleSwatchClick = useCallback(() => {
        inputRef.current?.click();
    }, []);

    const handleTransparentClick = useCallback(() => {
        onChange('transparent');
    }, [onChange]);

    return (
        <div className={styles.wrapper}>
            <button
                type="button"
                className={`${styles.swatch} ${transparent ? styles.swatchTransparent : ''}`}
                style={{ '--swatch-color': transparent ? 'transparent' : value } as CSSProperties}
                onClick={handleSwatchClick}
                aria-label={label ?? 'Choose color'}
                title={value}
            />
            <input
                ref={inputRef}
                type="color"
                className={styles.nativeInput}
                value={pickerHex}
                onChange={e => onChange(e.target.value)}
                aria-label={label}
                tabIndex={-1}
            />
            <button
                type="button"
                className={`${styles.transparentBtn} ${transparent ? styles.transparentBtnActive : ''}`}
                onClick={handleTransparentClick}
                aria-label={label ? `Set ${label} to transparent` : 'Set transparent'}
                aria-pressed={transparent}
                title="Transparent"
            >
                ∅
            </button>
            <input
                type="text"
                className={styles.textInput}
                value={value}
                onChange={e => onChange(e.target.value)}
                aria-label={label ? `${label} hex value` : 'Hex value'}
                spellCheck={false}
            />
        </div>
    );
}
