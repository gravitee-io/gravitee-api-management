/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Input } from '@gravitee/graphene-core';
import { useEffect, useId, useMemo, useRef, useState, type KeyboardEvent } from 'react';

export type FreeTextAutocompleteProps = {
    value: string;
    onChange: (value: string) => void;
    suggestions: readonly string[];
    disabled?: boolean;
    placeholder?: string;
    'aria-label'?: string;
    className?: string;
    emptyMessage?: string;
    isValid?: (value: string) => boolean;
    invalidMessage?: string;
    showInvalid?: boolean;
};

export function FreeTextAutocomplete({
    value,
    onChange,
    suggestions,
    disabled,
    placeholder,
    'aria-label': ariaLabel,
    className,
    emptyMessage = 'No matching suggestions — your custom value will be used.',
    isValid,
    invalidMessage,
    showInvalid = true,
}: Readonly<FreeTextAutocompleteProps>) {
    const listId = useId();
    const optionIdPrefix = useId();
    const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
    const [open, setOpen] = useState(false);
    const [activeIndex, setActiveIndex] = useState(-1);
    const invalid = useMemo(() => (isValid ? !isValid(value) : false), [isValid, value]);
    const showError = showInvalid && invalid && Boolean(invalidMessage);
    const activeOptionId = activeIndex >= 0 && activeIndex < suggestions.length ? `${optionIdPrefix}-${activeIndex}` : undefined;

    useEffect(() => {
        return () => {
            if (closeTimer.current) clearTimeout(closeTimer.current);
        };
    }, []);

    useEffect(() => {
        setActiveIndex(-1);
    }, [suggestions, value]);

    function cancelClose() {
        if (closeTimer.current) {
            clearTimeout(closeTimer.current);
            closeTimer.current = null;
        }
    }

    function select(name: string) {
        cancelClose();
        onChange(name);
        setOpen(false);
        setActiveIndex(-1);
    }

    function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
        if (disabled) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            cancelClose();
            setOpen(true);
            if (suggestions.length === 0) return;
            setActiveIndex(prev => (prev < suggestions.length - 1 ? prev + 1 : 0));
            return;
        }

        if (e.key === 'ArrowUp') {
            e.preventDefault();
            cancelClose();
            setOpen(true);
            if (suggestions.length === 0) return;
            setActiveIndex(prev => (prev <= 0 ? suggestions.length - 1 : prev - 1));
            return;
        }

        if (e.key === 'Enter' && open && activeIndex >= 0 && activeIndex < suggestions.length) {
            e.preventDefault();
            select(suggestions[activeIndex]);
            return;
        }

        if (e.key === 'Escape' && open) {
            e.preventDefault();
            setOpen(false);
            setActiveIndex(-1);
        }
    }

    return (
        <div className={className ?? 'space-y-1'}>
            <div className="relative">
                <Input
                    value={value}
                    placeholder={placeholder}
                    disabled={disabled}
                    className="w-full"
                    aria-label={ariaLabel}
                    role="combobox"
                    aria-autocomplete="list"
                    aria-expanded={open}
                    aria-controls={listId}
                    aria-activedescendant={activeOptionId}
                    aria-invalid={showError || undefined}
                    autoComplete="off"
                    onChange={e => {
                        onChange(e.target.value);
                        setOpen(true);
                        setActiveIndex(-1);
                    }}
                    onFocus={() => {
                        cancelClose();
                        setOpen(true);
                    }}
                    onBlur={() => {
                        closeTimer.current = setTimeout(() => {
                            setOpen(false);
                            setActiveIndex(-1);
                        }, 120);
                    }}
                    onKeyDown={handleKeyDown}
                />
                {open && !disabled ? (
                    <ul
                        id={listId}
                        role="listbox"
                        className="absolute z-50 mt-1 max-h-56 w-full overflow-auto rounded-md border bg-popover p-1 shadow-md"
                        onMouseDown={e => {
                            e.preventDefault();
                            cancelClose();
                        }}
                    >
                        {suggestions.length === 0 ? (
                            <li className="px-2 py-2 text-xs text-muted-foreground">{emptyMessage}</li>
                        ) : (
                            suggestions.map((name, index) => {
                                const optionId = `${optionIdPrefix}-${index}`;
                                const isActive = index === activeIndex;
                                return (
                                    <li key={name}>
                                        <button
                                            type="button"
                                            id={optionId}
                                            role="option"
                                            aria-selected={isActive}
                                            className={`flex w-full items-center rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent${
                                                isActive ? ' bg-accent' : ''
                                            }`}
                                            onClick={() => select(name)}
                                        >
                                            {name}
                                        </button>
                                    </li>
                                );
                            })
                        )}
                    </ul>
                ) : null}
            </div>
            {showError ? <p className="text-xs text-destructive">{invalidMessage}</p> : null}
        </div>
    );
}
