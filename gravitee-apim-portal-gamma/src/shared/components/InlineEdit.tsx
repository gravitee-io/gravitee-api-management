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
import { useEffect, useRef, useState } from 'react';

import styles from './InlineEdit.module.scss';

interface InlineEditProps {
    readonly value: string;
    readonly editable: boolean;
    readonly onChange: (value: string) => void;
    readonly className?: string;
    readonly ariaLabel?: string;
    readonly placeholder?: string;
    readonly activateOn?: 'click' | 'doubleClick';
    readonly onEditingChange?: (isEditing: boolean) => void;
}

export function InlineEdit({
    value,
    editable,
    onChange,
    className,
    ariaLabel,
    placeholder,
    activateOn = 'click',
    onEditingChange,
}: InlineEditProps) {
    const [isEditing, setIsEditing] = useState(false);
    const [draft, setDraft] = useState(value);
    const inputRef = useRef<HTMLInputElement>(null);

    const setEditing = (editing: boolean) => {
        setIsEditing(editing);
        onEditingChange?.(editing);
    };

    useEffect(() => {
        if (!isEditing) {
            setDraft(value);
        }
    }, [isEditing, value]);

    useEffect(() => {
        if (!isEditing) {
            return;
        }

        inputRef.current?.focus();
        inputRef.current?.select();
    }, [isEditing]);

    const commit = () => {
        const nextValue = draft.trim();

        if (nextValue !== value) {
            onChange(nextValue);
        }

        setEditing(false);
    };

    const cancel = () => {
        setDraft(value);
        setEditing(false);
    };

    const showPlaceholder = editable && value.length === 0 && Boolean(placeholder);
    const displayContent = showPlaceholder ? (
        <span className={styles.placeholder}>{placeholder}</span>
    ) : (
        value
    );

    if (!editable) {
        return (
            <span className={`${styles.button} ${styles.readOnly} ${className ?? ''}`}>
                {value}
            </span>
        );
    }

    if (isEditing) {
        return (
            <input
                ref={inputRef}
                type="text"
                className={`${styles.input} ${className ?? ''}`}
                value={draft}
                aria-label={ariaLabel}
                placeholder={placeholder}
                onChange={event => setDraft(event.target.value)}
                onBlur={commit}
                onKeyDown={event => {
                    event.stopPropagation();

                    if (event.key === 'Enter') {
                        commit();
                    }

                    if (event.key === 'Escape') {
                        event.preventDefault();
                        cancel();
                    }
                }}
            />
        );
    }

    if (activateOn === 'doubleClick') {
        return (
            <span
                className={`${styles.button} ${styles.doubleClickLabel} ${className ?? ''}`}
                aria-label={ariaLabel}
                onDoubleClick={event => {
                    event.stopPropagation();
                    setEditing(true);
                }}
            >
                {displayContent}
            </span>
        );
    }

    return (
        <button
            type="button"
            className={`${styles.button} ${className ?? ''}`}
            aria-label={ariaLabel}
            onClick={() => setEditing(true)}
        >
            {displayContent}
        </button>
    );
}
