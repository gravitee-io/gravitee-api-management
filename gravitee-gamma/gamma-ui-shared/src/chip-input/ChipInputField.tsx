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
import { Badge, Button, cn, Popover, PopoverAnchor, PopoverContent } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import type { KeyboardEvent } from 'react';
import { useCallback, useEffect, useId, useMemo, useState } from 'react';

/** Matches Classic mat-autocomplete panel height; inline styles because shared lib classes are not in Graphene CSS. */
const SUGGESTIONS_LIST_MAX_HEIGHT = '14rem';

export interface ChipInputFieldProps {
    readonly id?: string;
    readonly inputAriaLabel?: string;
    readonly values: string[];
    readonly onChange: (next: string[]) => void;
    readonly placeholder: string;
    readonly disabled?: boolean;
    readonly addOnComma?: boolean;
    readonly suggestions?: readonly string[];
    readonly invalid?: boolean;
    readonly describedBy?: string;
    readonly required?: boolean;
    readonly addOnBlur?: boolean;
    readonly monospace?: boolean;
    readonly fieldClassName?: string;
    readonly inputClassName?: string;
}

export function ChipInputField({
    id,
    inputAriaLabel,
    values,
    onChange,
    placeholder,
    disabled = false,
    addOnComma = false,
    suggestions = [],
    invalid = false,
    describedBy,
    required = false,
    addOnBlur = true,
    monospace = false,
    fieldClassName,
    inputClassName,
}: ChipInputFieldProps) {
    const [draft, setDraft] = useState('');
    const [open, setOpen] = useState(false);
    const [activeIndex, setActiveIndex] = useState(-1);
    const reactId = useId();
    const fieldId = id ?? reactId;
    const listId = `${fieldId}-suggestions`;
    const optionId = (index: number) => `${fieldId}-option-${index}`;

    const add = (value: string, keepSuggestionsOpen = false) => {
        if (disabled) {
            return;
        }
        const trimmed = value.trim();
        if (!trimmed || values.includes(trimmed)) {
            return;
        }
        onChange([...values, trimmed]);
        setDraft('');
        setOpen(keepSuggestionsOpen);
        setActiveIndex(-1);
    };

    const removeAt = (index: number) => {
        if (disabled) {
            return;
        }
        onChange(values.filter((_, itemIndex) => itemIndex !== index));
    };

    const filteredSuggestions = useMemo(() => {
        if (suggestions.length === 0) {
            return [];
        }
        const query = draft.trim().toLowerCase();
        return suggestions.filter(suggestion => {
            if (values.includes(suggestion)) {
                return false;
            }
            return !query || suggestion.toLowerCase().includes(query);
        });
    }, [draft, suggestions, values]);

    const hasAutocomplete = suggestions.length > 0;
    const listOpen = !disabled && open && hasAutocomplete && filteredSuggestions.length > 0;
    const activeDescendant = listOpen && activeIndex >= 0 ? optionId(activeIndex) : undefined;

    const openSuggestions = useCallback(() => {
        if (!disabled && hasAutocomplete) {
            setOpen(true);
        }
    }, [disabled, hasAutocomplete]);

    useEffect(() => {
        if (!listOpen || activeIndex < 0) {
            return;
        }
        document.getElementById(optionId(activeIndex))?.scrollIntoView?.({ block: 'nearest' });
    }, [activeIndex, listOpen]);

    const handleInputKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (hasAutocomplete && (event.key === 'ArrowDown' || event.key === 'ArrowUp')) {
            event.preventDefault();
            if (filteredSuggestions.length === 0) {
                return;
            }
            setOpen(true);
            setActiveIndex(current => {
                if (event.key === 'ArrowDown') {
                    return (current + 1) % filteredSuggestions.length;
                }
                return current <= 0 ? filteredSuggestions.length - 1 : current - 1;
            });
            return;
        }
        if (event.key === 'Enter' && activeIndex >= 0 && filteredSuggestions[activeIndex]) {
            event.preventDefault();
            add(filteredSuggestions[activeIndex], true);
            return;
        }
        if (event.key === 'Enter' || (addOnComma && event.key === ',')) {
            event.preventDefault();
            add(draft);
        } else if (event.key === 'Backspace' && !draft && values.length > 0) {
            removeAt(values.length - 1);
        } else if (event.key === 'Escape') {
            setOpen(false);
            setActiveIndex(-1);
        }
    };

    const chipTextClass = monospace ? 'font-mono text-xs' : 'font-normal';
    const optionTextClass = monospace ? 'font-mono' : '';

    const inputElement = (
        <input
            id={id}
            className={cn('w-full min-w-[100px] bg-transparent text-sm outline-none', inputClassName)}
            placeholder={placeholder}
            value={draft}
            disabled={disabled}
            aria-label={inputAriaLabel}
            role={hasAutocomplete ? 'combobox' : undefined}
            aria-expanded={hasAutocomplete ? listOpen : undefined}
            aria-controls={listOpen ? listId : undefined}
            aria-autocomplete={hasAutocomplete ? 'list' : undefined}
            aria-activedescendant={activeDescendant}
            aria-invalid={invalid || undefined}
            aria-describedby={describedBy}
            aria-required={required || undefined}
            autoComplete="off"
            onChange={event => {
                setDraft(event.target.value);
                setOpen(true);
                setActiveIndex(-1);
            }}
            onFocus={openSuggestions}
            onClick={openSuggestions}
            onKeyDown={handleInputKeyDown}
            onBlur={() => {
                if (addOnBlur) {
                    add(draft);
                } else {
                    setDraft('');
                }
                setOpen(false);
                setActiveIndex(-1);
            }}
        />
    );

    const suggestionList = (
        <ul
            id={listId}
            role="listbox"
            className="m-0 list-none p-1"
            style={{
                maxHeight: SUGGESTIONS_LIST_MAX_HEIGHT,
                overflowY: 'auto',
                overscrollBehavior: 'contain',
            }}
            onMouseDown={event => event.preventDefault()}
        >
            {filteredSuggestions.map((suggestion, index) => (
                <li key={suggestion}>
                    <button
                        type="button"
                        id={optionId(index)}
                        role="option"
                        aria-selected={index === activeIndex}
                        className={cn(
                            'flex w-full rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent',
                            index === activeIndex && 'bg-accent',
                            optionTextClass,
                        )}
                        onClick={() => add(suggestion, true)}
                    >
                        {suggestion}
                    </button>
                </li>
            ))}
        </ul>
    );

    return (
        <div className={cn('flex flex-wrap gap-1.5 rounded-md border bg-muted/30 p-2 min-h-9', disabled && 'opacity-50', fieldClassName)}>
            {values.map((value, index) => (
                <Badge key={`${value}-${index}`} variant="secondary" className={cn('gap-0.5 pr-1', chipTextClass)}>
                    {value}
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        className="ml-0.5 shrink-0 hover:text-destructive"
                        onClick={() => removeAt(index)}
                        aria-label={`Remove ${value}`}
                        disabled={disabled}
                    >
                        <XIcon className="size-3" aria-hidden />
                    </Button>
                </Badge>
            ))}
            {hasAutocomplete ? (
                <Popover
                    open={listOpen}
                    onOpenChange={next => {
                        if (!next) {
                            setOpen(false);
                            setActiveIndex(-1);
                        }
                    }}
                >
                    <PopoverAnchor asChild>
                        <div className="min-w-[100px] flex-1 basis-full">{inputElement}</div>
                    </PopoverAnchor>
                    <PopoverContent
                        hideWhenDetached
                        align="start"
                        side="bottom"
                        sideOffset={4}
                        avoidCollisions={false}
                        className="gap-0 p-0"
                        style={{
                            width: 'var(--radix-popover-trigger-width)',
                            maxHeight: SUGGESTIONS_LIST_MAX_HEIGHT,
                            overflow: 'hidden',
                            padding: 0,
                        }}
                        onOpenAutoFocus={event => event.preventDefault()}
                        onCloseAutoFocus={event => event.preventDefault()}
                    >
                        {suggestionList}
                    </PopoverContent>
                </Popover>
            ) : (
                <div className="min-w-[100px] flex-1">{inputElement}</div>
            )}
        </div>
    );
}
