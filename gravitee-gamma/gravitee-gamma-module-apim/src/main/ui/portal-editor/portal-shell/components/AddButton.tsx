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
import { PlusIcon } from '@gravitee/graphene-core/icons';
import type { ButtonHTMLAttributes } from 'react';

import styles from './AddButton.module.scss';

type AddButtonSize = 'sm' | 'md';

interface AddButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'type'> {
    readonly size?: AddButtonSize;
    readonly 'aria-label': string;
}

export function AddButton({ size = 'md', className, ...props }: AddButtonProps) {
    const sizeClass = size === 'sm' ? styles.sm : styles.md;
    const iconClass = size === 'sm' ? styles.iconSm : styles.iconMd;

    return (
        <button
            type="button"
            className={`${styles.button} ${sizeClass} ${className ?? ''}`}
            {...props}
        >
            <PlusIcon className={iconClass} aria-hidden="true" />
        </button>
    );
}
