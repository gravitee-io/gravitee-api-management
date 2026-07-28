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
    Checkbox,
    cn,
    Popover,
    PopoverContent,
    PopoverTrigger,
    ScrollArea,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { ChevronDownIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { ROLE_LIST_TOOLTIP_CONTENT_CLASS, RoleListTooltipContent } from './RoleListTooltip';

export interface RoleMultiSelectOption {
    value: string;
    label: string;
}

interface UserRoleMultiSelectProps {
    readonly options: RoleMultiSelectOption[];
    readonly selectedValues: string[];
    readonly onSelectedValuesChange: (values: string[]) => void;
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
                onSelectedValuesChange(draftValues);
            }
        }
    }

    const triggerButton = (
        <Button
            type="button"
            variant="outline"
            aria-label={ariaLabel}
            disabled={disabled}
            className="h-10 w-full justify-between gap-2 border-0 px-0 font-normal shadow-none hover:bg-transparent"
        >
            <RoleDisplayText
                displayText={displayText}
                isPlaceholder={selectedLabels.length === 0}
                showTooltip={showTooltip}
                labels={selectedLabels}
            />
            <ChevronDownIcon className="size-4 shrink-0 opacity-50" aria-hidden />
        </Button>
    );

    return (
        <div className={cn('w-72 max-w-full rounded-md border px-3 py-2', className)}>
            <TooltipProvider delayDuration={300}>
                <Popover open={popoverOpen} onOpenChange={handlePopoverOpenChange}>
                    <PopoverTrigger asChild>{triggerButton}</PopoverTrigger>
                    <PopoverContent className="block w-72 p-0" align="start">
                        {options.length === 0 ? (
                            <p className="p-3 text-sm text-muted-foreground">{emptyMessage}</p>
                        ) : (
                            <ScrollArea className="max-h-48">
                                <div className="space-y-2 p-3">
                                    {options.map(option => (
                                        <label key={option.value} className="flex cursor-pointer items-center gap-2 text-sm">
                                            <Checkbox
                                                checked={draftValues.includes(option.value)}
                                                onCheckedChange={() => toggleRole(option.value)}
                                                disabled={disabled}
                                                aria-label={option.label}
                                            />
                                            <span className="truncate" title={option.label}>
                                                {option.label}
                                            </span>
                                        </label>
                                    ))}
                                </div>
                            </ScrollArea>
                        )}
                    </PopoverContent>
                </Popover>
            </TooltipProvider>
        </div>
    );
}
