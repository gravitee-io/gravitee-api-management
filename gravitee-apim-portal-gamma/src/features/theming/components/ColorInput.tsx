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

export function ColorInput({ value, onChange, label }: ColorInputProps) {
    const inputRef = useRef<HTMLInputElement>(null);

    const handleSwatchClick = useCallback(() => {
        inputRef.current?.click();
    }, []);

    return (
        <div className={styles.wrapper}>
            <button
                type="button"
                className={styles.swatch}
                style={{ '--swatch-color': value } as CSSProperties}
                onClick={handleSwatchClick}
                aria-label={label ?? 'Choose color'}
                title={value}
            />
            <input
                ref={inputRef}
                type="color"
                className={styles.nativeInput}
                value={value}
                onChange={e => onChange(e.target.value)}
                aria-label={label}
                tabIndex={-1}
            />
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
