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
import type { PageWidth } from '../constants/page-width';
import { PAGE_WIDTH_VALUES } from '../constants/page-width';
import styles from './WidthSelector.module.scss';

interface WidthOption {
    readonly value: PageWidth;
    readonly label: string;
    readonly description: string;
}

const widthOptions: WidthOption[] = [
    {
        value: 'wide',
        label: 'Wide',
        description: `Maximum reading width (${PAGE_WIDTH_VALUES.wide}). Best for content-heavy pages.`,
    },
    {
        value: 'medium',
        label: 'Medium',
        description: `Balanced width (${PAGE_WIDTH_VALUES.medium}). Works well for most documentation.`,
    },
    {
        value: 'narrow',
        label: 'Narrow',
        description: `Focused width (${PAGE_WIDTH_VALUES.narrow}). Ideal for long-form reading.`,
    },
];

interface WidthSelectorProps {
    readonly value: PageWidth;
    readonly onChange: (value: PageWidth) => void;
}

function WidthSkeleton({ width }: { readonly width: PageWidth }) {
    return (
        <div className={styles.skeleton} aria-hidden="true">
            <div className={styles.skeletonPage}>
                <div className={styles.skeletonContent} data-width={width} />
            </div>
        </div>
    );
}

export function WidthSelector({ value, onChange }: WidthSelectorProps) {
    return (
        <div className={styles.grid} role="radiogroup" aria-label="Page width">
            {widthOptions.map(option => {
                const isSelected = option.value === value;

                return (
                    <button
                        key={option.value}
                        type="button"
                        className={`${styles.tile} ${isSelected ? styles.tileSelected : ''}`}
                        role="radio"
                        aria-label={option.label}
                        aria-checked={isSelected}
                        onClick={() => onChange(option.value)}
                    >
                        <WidthSkeleton width={option.value} />
                        <span className={styles.label}>{option.label}</span>
                        <span className={styles.description}>{option.description}</span>
                    </button>
                );
            })}
        </div>
    );
}
