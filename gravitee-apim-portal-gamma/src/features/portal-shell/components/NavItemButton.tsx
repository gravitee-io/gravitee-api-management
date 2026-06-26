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
import { Button } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import type { CSSProperties, ReactNode } from 'react';

import styles from './NavItemButton.module.scss';

interface NavItemButtonProps {
    readonly label: string;
    readonly selected: boolean;
    readonly showDelete: boolean;
    readonly onSelect: () => void;
    readonly onDelete: () => void;
    readonly variant?: 'header' | 'sidebar' | 'footer';
    readonly icon?: ReactNode;
    readonly style?: CSSProperties;
    readonly className?: string;
}

export function NavItemButton({
    label,
    selected,
    showDelete,
    onSelect,
    onDelete,
    variant = 'header',
    icon,
    style,
    className,
}: NavItemButtonProps) {
    const buttonClassName = [
        variant === 'header' ? styles.header : variant === 'sidebar' ? styles.sidebar : styles.footer,
        variant === 'sidebar' ? 'justify-start text-left' : undefined,
        className,
    ]
        .filter(Boolean)
        .join(' ');

    return (
        <div className={styles.navItemButton} style={style}>
            <Button
                variant={selected ? 'secondary' : 'ghost'}
                size="sm"
                className={buttonClassName}
                onClick={onSelect}
            >
                {icon ? <span className={styles.icon}>{icon}</span> : null}
                <span className={styles.label}>{label}</span>
            </Button>
            {showDelete ? (
                <button
                    type="button"
                    className={styles.deleteButton}
                    aria-label={`Delete ${label}`}
                    onClick={event => {
                        event.stopPropagation();
                        onDelete();
                    }}
                >
                    <XIcon className="size-3.5" aria-hidden="true" />
                </button>
            ) : null}
        </div>
    );
}
