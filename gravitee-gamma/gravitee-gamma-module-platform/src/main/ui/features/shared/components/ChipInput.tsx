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
import { Badge, Button } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

export interface ChipInputProps {
    readonly id?: string;
    readonly values: string[];
    readonly onChange: (next: string[]) => void;
    readonly placeholder: string;
    readonly disabled?: boolean;
    /** When true, pressing comma also commits the current draft value (off by default for URI-like values). */
    readonly addOnComma?: boolean;
    /** Optional autocomplete values, matching Classic `gio-form-tags-input` `[autocompleteOptions]`. */
    readonly suggestions?: readonly string[];
}

export function ChipInput({ id, values, onChange, placeholder, disabled = false, addOnComma = false, suggestions = [] }: ChipInputProps) {
    const [draft, setDraft] = useState('');
    const [open, setOpen] = useState(false);

    const add = (value: string) => {
        if (disabled) {
            return;
        }
        const trimmed = value.trim();
        if (!trimmed || values.includes(trimmed)) {
            return;
        }
        onChange([...values, trimmed]);
        setDraft('');
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
        return suggestions.filter(suggestion => !values.includes(suggestion) && (!query || suggestion.toLowerCase().includes(query)));
    }, [draft, suggestions, values]);

    const listId = id ? `${id}-suggestions` : undefined;
    const showSuggestions = !disabled && open && filteredSuggestions.length > 0;

    return (
        <div className="relative">
            <div className={`flex min-h-9 flex-wrap gap-1.5 rounded-md border bg-muted/30 p-2 ${disabled ? 'opacity-50' : ''}`}>
                {values.map((value, index) => (
                    <Badge key={`${value}-${index}`} variant="secondary" className="gap-0.5 pr-1 text-[11px] font-normal">
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
                <input
                    id={id}
                    className="min-w-[100px] flex-1 bg-transparent text-sm outline-none"
                    placeholder={placeholder}
                    value={draft}
                    disabled={disabled}
                    role={suggestions.length > 0 ? 'combobox' : undefined}
                    aria-expanded={suggestions.length > 0 ? showSuggestions : undefined}
                    aria-controls={showSuggestions ? listId : undefined}
                    aria-autocomplete={suggestions.length > 0 ? 'list' : undefined}
                    autoComplete="off"
                    onChange={event => {
                        setDraft(event.target.value);
                        setOpen(true);
                    }}
                    onFocus={() => setOpen(true)}
                    onKeyDown={event => {
                        if (event.key === 'Enter' || (addOnComma && event.key === ',')) {
                            event.preventDefault();
                            add(draft);
                        } else if (event.key === 'Backspace' && !draft && values.length > 0) {
                            removeAt(values.length - 1);
                        } else if (event.key === 'Escape') {
                            setOpen(false);
                        }
                    }}
                    onBlur={() => {
                        add(draft);
                        setOpen(false);
                    }}
                />
            </div>
            {showSuggestions ? (
                <ul
                    id={listId}
                    role="listbox"
                    className="absolute z-50 mt-1 max-h-56 w-full overflow-auto rounded-md border bg-popover p-1 shadow-md"
                    onMouseDown={event => event.preventDefault()}
                >
                    {filteredSuggestions.map(suggestion => (
                        <li key={suggestion}>
                            <button
                                type="button"
                                role="option"
                                aria-selected={false}
                                className="flex w-full rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent"
                                onClick={() => {
                                    add(suggestion);
                                    setOpen(false);
                                }}
                            >
                                {suggestion}
                            </button>
                        </li>
                    ))}
                </ul>
            ) : null}
        </div>
    );
}
