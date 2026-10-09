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
import { Button, Checkbox, cn, Popover, PopoverContent, PopoverTrigger } from '@gravitee/graphene-core';
import { ChevronDownIcon } from '@gravitee/graphene-core/icons';

export interface MultiSelectFilterOption {
    value: string;
    label: string;
    /** When true, the option stays selected/unselected and cannot be toggled. */
    disabled?: boolean;
}

function formatSelection(options: MultiSelectFilterOption[], selectedValues: string[], placeholder: string): string {
    const optionValues = new Set(options.map(option => option.value));
    const labels = options.filter(option => selectedValues.includes(option.value)).map(option => option.label);
    const orphanValues = selectedValues.filter(value => !optionValues.has(value));
    const parts = [...labels, ...orphanValues];
    return parts.length === 0 ? placeholder : parts.join(', ');
}

/** Popover + checkbox list allowing multiple values to be selected for a single filter. */
export function MultiSelectFilter({
    id,
    placeholder,
    options,
    selectedValues,
    onSelectedValuesChange,
    emptyMessage,
    ariaLabel,
    className,
    disabled = false,
}: Readonly<{
    id?: string;
    placeholder: string;
    options: MultiSelectFilterOption[];
    selectedValues: string[];
    onSelectedValuesChange: (values: string[]) => void;
    emptyMessage?: string;
    ariaLabel: string;
    className?: string;
    disabled?: boolean;
}>) {
    const display = formatSelection(options, selectedValues, placeholder);

    const toggle = (value: string) => {
        const option = options.find(o => o.value === value);
        if (option?.disabled) return;
        onSelectedValuesChange(selectedValues.includes(value) ? selectedValues.filter(v => v !== value) : [...selectedValues, value]);
    };

    return (
        <Popover>
            <PopoverTrigger asChild>
                <Button
                    id={id}
                    type="button"
                    variant="outline"
                    aria-label={ariaLabel}
                    disabled={disabled}
                    className={cn('h-9 w-full justify-start gap-2 px-3 font-normal', className)}
                >
                    <span className={cn('min-w-0 flex-1 truncate text-left', selectedValues.length === 0 && 'text-muted-foreground')}>
                        {display}
                    </span>
                    <ChevronDownIcon className="size-4 shrink-0 opacity-50" aria-hidden />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-[260px] p-3" align="start">
                {options.length === 0 ? (
                    <p className="text-xs text-muted-foreground">{emptyMessage ?? 'No options'}</p>
                ) : (
                    <div className="max-h-48 min-h-0 space-y-2 overflow-y-auto overscroll-contain">
                        {options.map(option => (
                            <label
                                key={option.value}
                                className={cn(
                                    'flex items-center gap-2 text-sm',
                                    option.disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer',
                                )}
                            >
                                <Checkbox
                                    checked={selectedValues.includes(option.value)}
                                    onCheckedChange={() => toggle(option.value)}
                                    disabled={option.disabled}
                                />
                                <span className="truncate">{option.label}</span>
                            </label>
                        ))}
                    </div>
                )}
            </PopoverContent>
        </Popover>
    );
}
