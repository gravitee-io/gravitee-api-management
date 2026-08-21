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
    Button,
    cn,
    Field,
    FieldError,
    FieldLabel,
    Popover,
    PopoverContent,
    PopoverTrigger,
    TooltipProvider,
} from '@gravitee/graphene-core';
import { CheckIcon, ChevronDownIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { RequiredMark } from './RequiredMark';
import { TruncatedDisplayText } from '../../../shared/components/TruncatedDisplayText';
import { formatTruncatedNameSummary } from '../../../shared/utils/truncatedList';

export interface IdentityProviderMappingOption {
    readonly id: string;
    readonly name: string;
    readonly description?: string;
}

export function IdentityProviderMappingMultiSelect({
    id,
    label,
    values,
    options,
    required = false,
    invalid = false,
    error,
    disabled = false,
    hideLabel = false,
    placeholder,
    emptyMessage,
    onChange,
}: Readonly<{
    id: string;
    label: string;
    values: string[];
    options: readonly IdentityProviderMappingOption[];
    required?: boolean;
    invalid?: boolean;
    error?: string;
    disabled?: boolean;
    hideLabel?: boolean;
    placeholder: string;
    emptyMessage: string;
    onChange: (values: string[]) => void;
}>) {
    const [open, setOpen] = useState(false);
    const errorId = `${id}-error`;
    const selected = useMemo(() => {
        const byId = new Map(options.map(option => [option.id, option]));
        return values.map(value => byId.get(value) ?? { id: value, name: value });
    }, [options, values]);
    const listOptions = useMemo(() => {
        const byId = new Map(options.map(option => [option.id, option]));
        const extras = values.filter(value => !byId.has(value)).map(value => ({ id: value, name: value }));
        return [...options, ...extras];
    }, [options, values]);
    const selectedLabels = selected.map(option => option.name);
    const summary = formatTruncatedNameSummary(selectedLabels);
    const displayText = selectedLabels.length === 0 ? placeholder : summary.display;
    const triggerName = selectedLabels.length === 0 ? label : `${label}: ${summary.full}`;

    function toggleValue(valueId: string) {
        onChange(values.includes(valueId) ? values.filter(value => value !== valueId) : [...values, valueId]);
    }

    return (
        <Field>
            <FieldLabel htmlFor={id} className={hideLabel ? 'sr-only' : undefined}>
                {label}
                {required ? (
                    <>
                        {' '}
                        <RequiredMark />
                    </>
                ) : null}
            </FieldLabel>
            <TooltipProvider delayDuration={300}>
                <Popover open={open} onOpenChange={setOpen}>
                    <PopoverTrigger asChild>
                        <Button
                            id={id}
                            type="button"
                            variant="outline"
                            aria-label={triggerName}
                            aria-required={required || undefined}
                            aria-invalid={invalid || undefined}
                            aria-describedby={invalid && error ? errorId : undefined}
                            disabled={disabled}
                            className="h-10 w-full max-w-full justify-between gap-2 px-3 font-normal"
                        >
                            <TruncatedDisplayText
                                displayText={displayText}
                                isPlaceholder={selectedLabels.length === 0}
                                showTooltip={summary.truncated && !open}
                                labels={selectedLabels}
                            />
                            <ChevronDownIcon className="size-4 shrink-0 opacity-50" aria-hidden />
                        </Button>
                    </PopoverTrigger>
                    <PopoverContent className="block w-72 p-0" align="start">
                        {listOptions.length === 0 ? (
                            <p className="p-3 text-sm text-muted-foreground">{emptyMessage}</p>
                        ) : (
                            <div className="max-h-48 space-y-1 overflow-y-auto p-2">
                                {listOptions.map(option => {
                                    const selectedOption = values.includes(option.id);
                                    return (
                                        <button
                                            key={option.id}
                                            type="button"
                                            aria-pressed={selectedOption}
                                            aria-label={option.name}
                                            disabled={disabled}
                                            className={cn(
                                                'flex w-full cursor-pointer items-center gap-2 rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent hover:text-accent-foreground',
                                                selectedOption && 'bg-accent/50',
                                            )}
                                            onClick={() => toggleValue(option.id)}
                                        >
                                            <CheckIcon className={cn('size-4 shrink-0', !selectedOption && 'invisible')} aria-hidden />
                                            <span className="truncate" title={option.name}>
                                                {option.name}
                                            </span>
                                        </button>
                                    );
                                })}
                            </div>
                        )}
                    </PopoverContent>
                </Popover>
            </TooltipProvider>
            {invalid && error ? <FieldError id={errorId}>{error}</FieldError> : null}
        </Field>
    );
}
