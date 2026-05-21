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

const FILTER_FIELD_WIDTH = 'w-[220px]';

function formatSelection(labels: string[], placeholder: string): string {
    if (labels.length === 0) return placeholder;
    return labels.join(', ');
}

export interface MultiSelectOption {
    value: string;
    label: string;
}

export function ApplicationSubscriptionMultiSelectFilter({
    placeholder,
    options,
    selectedValues,
    onSelectedValuesChange,
    emptyMessage,
    ariaLabel,
}: Readonly<{
    placeholder: string;
    options: MultiSelectOption[];
    selectedValues: string[];
    onSelectedValuesChange: (values: string[]) => void;
    emptyMessage?: string;
    ariaLabel: string;
}>) {
    const display = formatSelection(
        options.filter(o => selectedValues.includes(o.value)).map(o => o.label),
        placeholder,
    );

    const toggle = (value: string) => {
        onSelectedValuesChange(selectedValues.includes(value) ? selectedValues.filter(v => v !== value) : [...selectedValues, value]);
    };

    return (
        <Popover>
            <PopoverTrigger asChild>
                <Button
                    type="button"
                    variant="outline"
                    aria-label={ariaLabel}
                    className={cn(FILTER_FIELD_WIDTH, 'h-9 shrink-0 justify-start gap-2 px-3 font-normal')}
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
                    <div className="max-h-[200px] space-y-2 overflow-y-auto">
                        {options.map(option => (
                            <label key={option.value} className="flex cursor-pointer items-center gap-2 text-sm">
                                <Checkbox checked={selectedValues.includes(option.value)} onCheckedChange={() => toggle(option.value)} />
                                <span className="truncate">{option.label}</span>
                            </label>
                        ))}
                    </div>
                )}
            </PopoverContent>
        </Popover>
    );
}
