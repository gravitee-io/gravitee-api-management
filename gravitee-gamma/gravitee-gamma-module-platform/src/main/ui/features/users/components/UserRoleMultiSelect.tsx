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
    Popover,
    PopoverContent,
    PopoverTrigger,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { CheckIcon, ChevronDownIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { ROLE_LIST_TOOLTIP_CONTENT_CLASS, RoleListTooltipContent } from './RoleListTooltip';

export interface RoleMultiSelectOption {
    value: string;
    label: string;
}

interface UserRoleMultiSelectProps {
    readonly options: RoleMultiSelectOption[];
    readonly selectedValues: string[];
    readonly onSelectedValuesChange: (values: string[]) => void | Promise<void>;
    readonly placeholder?: string;
    readonly ariaLabel: string;
    readonly disabled?: boolean;
    readonly emptyMessage?: string;
    readonly className?: string;
}

function RoleDisplayText({
    displayText,
    isPlaceholder,
    showTooltip,
    labels,
}: Readonly<{
    displayText: string;
    isPlaceholder: boolean;
    showTooltip: boolean;
    labels: string[];
}>) {
    const textClassName = cn('min-w-0 flex-1 truncate text-left', isPlaceholder && 'text-muted-foreground');

    if (!showTooltip) {
        return <span className={textClassName}>{displayText}</span>;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className={textClassName}>{displayText}</span>
            </TooltipTrigger>
            <TooltipContent side="bottom" align="start" className={ROLE_LIST_TOOLTIP_CONTENT_CLASS}>
                <RoleListTooltipContent labels={labels} />
            </TooltipContent>
        </Tooltip>
    );
}

export function UserRoleMultiSelect({
    options,
    selectedValues,
    onSelectedValuesChange,
    placeholder = 'Select roles',
    ariaLabel,
    disabled = false,
    emptyMessage = 'No roles available',
    className,
}: UserRoleMultiSelectProps) {
    const [popoverOpen, setPopoverOpen] = useState(false);
    const [draftValues, setDraftValues] = useState(selectedValues);

    useEffect(() => {
        setDraftValues(selectedValues);
    }, [selectedValues]);

    const selectedLabels = useMemo(
        () => options.filter(option => draftValues.includes(option.value)).map(option => option.label),
        [draftValues, options],
    );
    const displayText = selectedLabels.length === 0 ? placeholder : selectedLabels.join(', ');
    const showTooltip = selectedLabels.length > 0 && !popoverOpen;

    function toggleRole(roleId: string) {
        setDraftValues(current => (current.includes(roleId) ? current.filter(value => value !== roleId) : [...current, roleId]));
    }

    function handlePopoverOpenChange(open: boolean) {
        setPopoverOpen(open);
        if (!open) {
            const unchanged = draftValues.length === selectedValues.length && draftValues.every(value => selectedValues.includes(value));
            if (!unchanged) {
                const committedValues = draftValues;
                const previousValues = selectedValues;
                void Promise.resolve(onSelectedValuesChange(committedValues)).catch(() => {
                    setDraftValues(previousValues);
                });
            }
        }
    }

    return (
        <TooltipProvider delayDuration={300}>
            <Popover open={popoverOpen} onOpenChange={handlePopoverOpenChange}>
                <PopoverTrigger asChild>
                    <Button
                        type="button"
                        variant="outline"
                        aria-label={ariaLabel}
                        disabled={disabled}
                        className={cn('h-10 w-72 max-w-full justify-between gap-2 px-3 font-normal', className)}
                    >
                        <RoleDisplayText
                            displayText={displayText}
                            isPlaceholder={selectedLabels.length === 0}
                            showTooltip={showTooltip}
                            labels={selectedLabels}
                        />
                        <ChevronDownIcon className="size-4 shrink-0 opacity-50" aria-hidden />
                    </Button>
                </PopoverTrigger>
                <PopoverContent className="block w-72 p-0" align="start">
                    {options.length === 0 ? (
                        <p className="p-3 text-sm text-muted-foreground">{emptyMessage}</p>
                    ) : (
                        <div className="max-h-48 space-y-1 overflow-y-auto p-2">
                            {options.map(option => {
                                const selected = draftValues.includes(option.value);
                                return (
                                    <button
                                        key={option.value}
                                        type="button"
                                        aria-pressed={selected}
                                        aria-label={option.label}
                                        disabled={disabled}
                                        className={cn(
                                            'flex w-full cursor-pointer items-center gap-2 rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent hover:text-accent-foreground',
                                            selected && 'bg-accent/50',
                                        )}
                                        onClick={() => toggleRole(option.value)}
                                    >
                                        <CheckIcon className={cn('size-4 shrink-0', !selected && 'invisible')} aria-hidden />
                                        <span className="truncate" title={option.label}>
                                            {option.label}
                                        </span>
                                    </button>
                                );
                            })}
                        </div>
                    )}
                </PopoverContent>
            </Popover>
        </TooltipProvider>
    );
}
