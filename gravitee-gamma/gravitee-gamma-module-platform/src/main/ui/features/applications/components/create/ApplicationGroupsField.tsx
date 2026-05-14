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
import { Badge, Button, Checkbox, cn, Label, Popover, PopoverContent, PopoverTrigger, ScrollArea } from '@gravitee/graphene-core';
import { ChevronsUpDownIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useLayoutEffect, useMemo, useRef, useState } from 'react';

import type { ApplicationGroup } from '../../types/applicationCreate';

interface ApplicationGroupsFieldProps {
    readonly groups: ApplicationGroup[];
    readonly selectedGroupIds: string[];
    readonly onSelectedGroupIdsChange: (groupIds: string[]) => void;
    readonly isLoading?: boolean;
    readonly required?: boolean;
}

export function ApplicationGroupsField({
    groups,
    selectedGroupIds,
    onSelectedGroupIdsChange,
    isLoading = false,
    required = false,
}: ApplicationGroupsFieldProps) {
    const triggerRef = useRef<HTMLButtonElement>(null);
    const [open, setOpen] = useState(false);
    const [popoverWidth, setPopoverWidth] = useState<number>();

    const sortedGroups = useMemo(() => [...groups].sort((a, b) => a.name.localeCompare(b.name)), [groups]);

    const selectedGroups = useMemo(
        () => sortedGroups.filter(group => selectedGroupIds.includes(group.id)),
        [selectedGroupIds, sortedGroups],
    );

    const syncPopoverWidth = () => {
        const width = triggerRef.current?.offsetWidth;
        if (width) {
            setPopoverWidth(width);
        }
    };

    const handleOpenChange = (nextOpen: boolean) => {
        if (nextOpen) {
            syncPopoverWidth();
        }
        setOpen(nextOpen);
    };

    useLayoutEffect(() => {
        if (!open) {
            return undefined;
        }

        syncPopoverWidth();

        const handleResize = () => syncPopoverWidth();
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, [open]);

    const toggleGroup = (groupId: string, checked: boolean) => {
        onSelectedGroupIdsChange(checked ? [...selectedGroupIds, groupId] : selectedGroupIds.filter(id => id !== groupId));
    };

    const removeGroup = (groupId: string) => {
        onSelectedGroupIdsChange(selectedGroupIds.filter(id => id !== groupId));
    };

    return (
        <div className="space-y-2">
            <Label htmlFor="application-groups">
                Groups
                {required && <span className="text-destructive"> *</span>}
            </Label>

            <div className="w-full">
                <Popover open={open} onOpenChange={handleOpenChange}>
                    <PopoverTrigger asChild>
                        <Button
                            ref={triggerRef}
                            id="application-groups"
                            type="button"
                            variant="outline"
                            role="combobox"
                            disabled={isLoading}
                            className={cn(
                                'h-auto min-h-9 w-full justify-between gap-2 px-3 py-2 font-normal',
                                selectedGroups.length === 0 && 'text-muted-foreground',
                            )}
                        >
                            <span className="flex min-w-0 flex-1 flex-wrap items-center gap-1 text-left">
                                {selectedGroups.length === 0 ? (
                                    <span>{required ? 'Select groups (required)' : 'Select groups'}</span>
                                ) : (
                                    selectedGroups.map(group => (
                                        <Badge
                                            key={group.id}
                                            variant="secondary"
                                            className="gap-0.5 pr-1 font-normal"
                                            onClick={event => event.stopPropagation()}
                                        >
                                            {group.name}
                                            <button
                                                type="button"
                                                className="rounded-sm p-0.5 hover:bg-muted"
                                                aria-label={`Remove ${group.name}`}
                                                onClick={event => {
                                                    event.stopPropagation();
                                                    removeGroup(group.id);
                                                }}
                                            >
                                                <XIcon className="size-3" aria-hidden />
                                            </button>
                                        </Badge>
                                    ))
                                )}
                            </span>
                            <ChevronsUpDownIcon className="size-4 shrink-0 opacity-50" aria-hidden />
                        </Button>
                    </PopoverTrigger>
                    <PopoverContent
                        align="start"
                        className="gap-0 p-0"
                        style={
                            popoverWidth !== undefined ? { width: popoverWidth, minWidth: popoverWidth, maxWidth: popoverWidth } : undefined
                        }
                    >
                        <ScrollArea className="max-h-60 w-full">
                            <div className="w-full p-1">
                                {sortedGroups.length === 0 ? (
                                    <p className="px-2 py-3 text-sm text-muted-foreground">No groups available</p>
                                ) : (
                                    sortedGroups.map(group => (
                                        <label
                                            key={group.id}
                                            className="flex w-full cursor-pointer items-center gap-2 rounded-sm px-2 py-2 text-sm hover:bg-accent"
                                        >
                                            <Checkbox
                                                checked={selectedGroupIds.includes(group.id)}
                                                onCheckedChange={checked => toggleGroup(group.id, checked === true)}
                                            />
                                            <span className="truncate">{group.name}</span>
                                        </label>
                                    ))
                                )}
                            </div>
                        </ScrollArea>
                    </PopoverContent>
                </Popover>
            </div>

            <p className="text-xs text-muted-foreground">Select user groups to add to the application</p>
        </div>
    );
}
