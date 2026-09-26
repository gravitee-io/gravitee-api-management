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
import { Badge, Label } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import type { KeyboardEvent, ReactNode } from 'react';
import { useCallback, useId, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

import { InfoTooltip } from './InfoTooltip';

const DROPDOWN_MAX_HEIGHT_PX = 224;
const DROPDOWN_OFFSET_PX = 4;

interface DropdownPosition {
    top: number;
    left: number;
    width: number;
    maxHeight: number;
}

export interface ChipsProps {
    label: string;
    hint: ReactNode;
    values: string[];
    placeholder: string;
    disabled?: boolean;
    suggestions?: readonly string[];
    /** When false, blur discards the draft (Classic CORS headers). Default true. */
    addOnBlur?: boolean;
    onChange: (next: string[]) => void;
}

export function Chips({ label, hint, values, placeholder, disabled, suggestions, addOnBlur = true, onChange }: ChipsProps) {
    const [draft, setDraft] = useState('');
    const [open, setOpen] = useState(false);
    const [activeIndex, setActiveIndex] = useState(-1);
    const [dropdownPosition, setDropdownPosition] = useState<DropdownPosition | null>(null);
    const rootRef = useRef<HTMLDivElement>(null);
    const reactId = useId();
    const listId = `${reactId}-suggestions`;
    const optionId = (index: number) => `${reactId}-option-${index}`;

    const add = (value: string, keepSuggestionsOpen = false) => {
        const trimmed = value.trim();
        if (!trimmed || values.includes(trimmed)) {
            return;
        }
        onChange([...values, trimmed]);
        setDraft('');
        setOpen(keepSuggestionsOpen);
        setActiveIndex(-1);
    };

    const remove = useCallback((v: string) => onChange(values.filter(x => x !== v)), [onChange, values]);

    const filteredSuggestions = useMemo(() => {
        if (!suggestions || suggestions.length === 0) {
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

    const hasAutocomplete = Boolean(suggestions && suggestions.length > 0);
    const showSuggestions = !disabled && open && hasAutocomplete && filteredSuggestions.length > 0;
    const activeDescendant = showSuggestions && activeIndex >= 0 ? optionId(activeIndex) : undefined;

    const updateDropdownPosition = useCallback(() => {
        const root = rootRef.current;
        if (!root) {
            setDropdownPosition(null);
            return;
        }
        const rect = root.getBoundingClientRect();
        const spaceBelow = window.innerHeight - rect.bottom - DROPDOWN_OFFSET_PX;
        const spaceAbove = rect.top - DROPDOWN_OFFSET_PX;
        const openAbove = spaceBelow < DROPDOWN_MAX_HEIGHT_PX && spaceAbove > spaceBelow;
        const maxHeight = Math.min(DROPDOWN_MAX_HEIGHT_PX, Math.max(0, openAbove ? spaceAbove : spaceBelow));

        if (maxHeight <= 0) {
            setDropdownPosition(null);
            return;
        }

        setDropdownPosition({
            top: openAbove ? rect.top - DROPDOWN_OFFSET_PX - maxHeight : rect.bottom + DROPDOWN_OFFSET_PX,
            left: rect.left,
            width: rect.width,
            maxHeight,
        });
    }, []);

    useLayoutEffect(() => {
        if (!showSuggestions) {
            setDropdownPosition(null);
            return;
        }
        updateDropdownPosition();
        window.addEventListener('resize', updateDropdownPosition);
        window.addEventListener('scroll', updateDropdownPosition, true);
        return () => {
            window.removeEventListener('resize', updateDropdownPosition);
            window.removeEventListener('scroll', updateDropdownPosition, true);
        };
    }, [showSuggestions, updateDropdownPosition, filteredSuggestions.length, draft, values.length]);

    const openSuggestions = () => {
        if (!disabled && hasAutocomplete) {
            setOpen(true);
        }
    };

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
        if (event.key === 'Enter' || event.key === ',') {
            event.preventDefault();
            add(draft);
        } else if (event.key === 'Backspace' && !draft && values.length) {
            remove(values[values.length - 1]);
        } else if (event.key === 'Escape') {
            setOpen(false);
            setActiveIndex(-1);
        }
    };

    return (
        <div className="space-y-2" ref={rootRef}>
            <div className="flex items-center gap-1.5">
                <Label className={disabled ? 'text-muted-foreground' : ''}>{label}</Label>
                <InfoTooltip content={hint} />
            </div>

            <div className="flex flex-wrap gap-2 rounded-md border bg-muted/30 p-2 min-h-11">
                {values.map(v => (
                    <Badge key={v} variant="secondary" className="gap-1 font-mono text-xs">
                        {v}
                        {!disabled && (
                            <button type="button" onClick={() => remove(v)} className="hover:text-destructive" aria-label={`Remove ${v}`}>
                                <XIcon className="size-3" />
                            </button>
                        )}
                    </Badge>
                ))}
                <input
                    className="flex-1 min-w-36 bg-transparent outline-none text-sm placeholder:text-muted-foreground disabled:cursor-not-allowed"
                    aria-label={label}
                    placeholder={values.length === 0 ? placeholder : ''}
                    value={draft}
                    disabled={disabled}
                    role={hasAutocomplete ? 'combobox' : undefined}
                    aria-expanded={hasAutocomplete ? showSuggestions : undefined}
                    aria-controls={showSuggestions ? listId : undefined}
                    aria-autocomplete={hasAutocomplete ? 'list' : undefined}
                    aria-activedescendant={activeDescendant}
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
                        if (addOnBlur && draft.trim()) {
                            add(draft);
                        } else if (!addOnBlur) {
                            setDraft('');
                        }
                        setOpen(false);
                        setActiveIndex(-1);
                    }}
                />
            </div>

            {showSuggestions && dropdownPosition
                ? createPortal(
                      <ul
                          id={listId}
                          role="listbox"
                          className="fixed z-[100] overflow-y-auto rounded-md border bg-popover p-1 shadow-md"
                          style={{
                              top: dropdownPosition.top,
                              left: dropdownPosition.left,
                              width: dropdownPosition.width,
                              maxHeight: dropdownPosition.maxHeight,
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
                                      className={`flex w-full rounded-sm px-2 py-1.5 text-left text-sm font-mono ${index === activeIndex ? 'bg-accent' : 'hover:bg-accent'}`}
                                      onClick={() => add(suggestion, true)}
                                  >
                                      {suggestion}
                                  </button>
                              </li>
                          ))}
                      </ul>,
                      document.body,
                  )
                : null}
        </div>
    );
}
