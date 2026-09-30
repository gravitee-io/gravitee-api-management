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
import {
    Badge,
    Button,
    cn,
    Combobox,
    ComboboxChip,
    ComboboxChips,
    ComboboxChipsInput,
    ComboboxContent,
    ComboboxItem,
    ComboboxList,
    useComboboxAnchor,
} from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import type { KeyboardEvent } from 'react';
import { useId, useMemo, useRef, useState } from 'react';

import { commitChipDraft, filterChipSuggestions } from './chipInputFieldUtils';

/** Matches Classic mat-autocomplete panel height; inline styles because shared lib classes are not in Graphene CSS. */
const SUGGESTIONS_LIST_MAX_HEIGHT = '14rem';

const chipsContainerClass = (disabled: boolean, invalid: boolean, fieldClassName?: string) =>
    cn(
        'flex flex-wrap gap-1.5 rounded-md border bg-muted/30 p-2 min-h-9',
        disabled && 'opacity-50',
        invalid && 'border-destructive',
        fieldClassName,
    );

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

function PlainChipInputField({
    id,
    inputAriaLabel,
    values,
    onChange,
    placeholder,
    disabled = false,
    addOnComma = false,
    invalid = false,
    describedBy,
    required = false,
    addOnBlur = true,
    monospace = false,
    fieldClassName,
    inputClassName,
}: ChipInputFieldProps) {
    const [draft, setDraft] = useState('');

    const removeAt = (index: number) => {
        if (disabled) {
            return;
        }
        onChange(values.filter((_, itemIndex) => itemIndex !== index));
    };

    const addDraft = () => {
        if (disabled) {
            return;
        }
        if (commitChipDraft(values, draft, onChange)) {
            setDraft('');
        }
    };

    const handleInputKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        const isEnterKey = event.key === 'Enter';
        const isCommaKey = addOnComma && event.key === ',';
        if (isEnterKey || isCommaKey) {
            event.preventDefault();
            addDraft();
        } else if (event.key === 'Backspace' && !draft && values.length > 0) {
            removeAt(values.length - 1);
        }
    };

    const chipTextClass = monospace ? 'font-mono text-xs' : 'font-normal';

    return (
        <div className={chipsContainerClass(disabled, invalid, fieldClassName)}>
            {values.map((value, index) => (
                <Badge key={`${value}-${index}`} variant="secondary" className={cn('gap-0.5 pr-1', chipTextClass)}>
                    {value}
                    {!disabled ? (
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon-xs"
                            className="ml-0.5 shrink-0 hover:text-destructive"
                            onClick={() => removeAt(index)}
                            aria-label={`Remove ${value}`}
                        >
                            <XIcon className="size-3" aria-hidden />
                        </Button>
                    ) : null}
                </Badge>
            ))}
            <input
                id={id}
                className={cn('min-w-[100px] flex-1 bg-transparent text-sm outline-none', inputClassName)}
                placeholder={placeholder}
                value={draft}
                disabled={disabled}
                aria-label={inputAriaLabel}
                aria-invalid={invalid || undefined}
                aria-describedby={describedBy}
                aria-required={required || undefined}
                autoComplete="off"
                onChange={event => setDraft(event.target.value)}
                onKeyDown={handleInputKeyDown}
                onBlur={() => {
                    if (addOnBlur) {
                        addDraft();
                    } else {
                        setDraft('');
                    }
                }}
            />
        </div>
    );
}

function AutocompleteChipInputField(props: ChipInputFieldProps) {
    const {
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
    } = props;

    const [draft, setDraft] = useState('');
    const [popupOpen, setPopupOpen] = useState(false);
    const suppressSuggestionOpenRef = useRef(false);
    const reactId = useId();
    const fieldId = id ?? reactId;
    const anchorRef = useComboboxAnchor();

    const filteredSuggestions = useMemo(
        () => filterChipSuggestions(suggestions, values, draft),
        [draft, suggestions, values],
    );

    const chipTextClass = monospace ? 'font-mono text-xs' : 'font-normal';
    const optionTextClass = monospace ? 'font-mono' : '';

    const commitDraftFromInput = (input: HTMLInputElement) => {
        if (commitChipDraft(values, input.value, onChange)) {
            setDraft('');
            input.value = '';
        }
    };

    const handleInputKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        const isEnterKey = event.key === 'Enter';
        const isCommaKey = addOnComma && event.key === ',';
        if (isEnterKey || isCommaKey) {
            event.preventDefault();
            commitDraftFromInput(event.currentTarget);
        } else if (event.key === 'Backspace' && !event.currentTarget.value && values.length > 0 && !disabled) {
            onChange(values.slice(0, -1));
        }
    };

    const openSuggestions = () => {
        if (disabled || suppressSuggestionOpenRef.current) {
            suppressSuggestionOpenRef.current = false;
            return;
        }
        setPopupOpen(true);
    };

    return (
        <Combobox
            multiple
            open={popupOpen}
            onOpenChange={open => {
                if (open && suppressSuggestionOpenRef.current) {
                    suppressSuggestionOpenRef.current = false;
                    setPopupOpen(false);
                    return;
                }
                setPopupOpen(open);
            }}
            value={values}
            onValueChange={next => {
                const nextValues = Array.isArray(next) ? next : [next].filter(Boolean);
                onChange(nextValues);
                if (nextValues.length > values.length) {
                    setPopupOpen(true);
                } else if (nextValues.length < values.length) {
                    suppressSuggestionOpenRef.current = true;
                    setPopupOpen(false);
                }
            }}
            disabled={disabled}
            autoComplete="list"
        >
            <ComboboxChips ref={anchorRef} className={chipsContainerClass(disabled, invalid, fieldClassName)}>
                {values.map(value => (
                    <ComboboxChip key={value} removeAriaLabel={`Remove ${value}`} className={chipTextClass}>
                        {value}
                    </ComboboxChip>
                ))}
                <ComboboxChipsInput
                    id={fieldId}
                    placeholder={placeholder}
                    aria-label={inputAriaLabel}
                    aria-invalid={invalid || undefined}
                    aria-describedby={describedBy}
                    aria-required={required || undefined}
                    className={cn('min-w-[100px] flex-1', inputClassName)}
                    value={draft}
                    onChange={event => {
                        setDraft(event.target.value);
                        openSuggestions();
                    }}
                    onFocus={openSuggestions}
                    onClick={openSuggestions}
                    onKeyDown={handleInputKeyDown}
                    onBlur={event => {
                        if (addOnBlur) {
                            commitDraftFromInput(event.currentTarget);
                        } else {
                            setDraft('');
                        }
                        setPopupOpen(false);
                    }}
                />
            </ComboboxChips>
            <ComboboxContent anchor={anchorRef} align="start">
                <ComboboxList
                    style={{
                        maxHeight: SUGGESTIONS_LIST_MAX_HEIGHT,
                        overflowY: 'auto',
                        overscrollBehavior: 'contain',
                    }}
                >
                    {filteredSuggestions.map(suggestion => (
                        <ComboboxItem key={suggestion} value={suggestion} className={optionTextClass}>
                            {suggestion}
                        </ComboboxItem>
                    ))}
                </ComboboxList>
            </ComboboxContent>
        </Combobox>
    );
}

export function ChipInputField(props: ChipInputFieldProps) {
    const hasAutocomplete = (props.suggestions?.length ?? 0) > 0;
    if (!hasAutocomplete) {
        return <PlainChipInputField {...props} />;
    }
    return <AutocompleteChipInputField {...props} />;
}
