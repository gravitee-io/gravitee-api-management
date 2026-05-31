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
    Combobox,
    ComboboxChip,
    ComboboxChips,
    ComboboxChipsInput,
    ComboboxContent,
    ComboboxEmpty,
    ComboboxItem,
    ComboboxList,
    Label,
    useComboboxAnchor,
} from '@gravitee/graphene-core';
import { useMemo } from 'react';

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
    const chipsAnchorRef = useComboboxAnchor();

    const sortedGroups = useMemo(() => [...groups].sort((a, b) => a.name.localeCompare(b.name)), [groups]);

    const selectedGroups = useMemo(
        () => sortedGroups.filter(group => selectedGroupIds.includes(group.id)),
        [selectedGroupIds, sortedGroups],
    );

    const placeholder = required ? 'Select groups (required)' : 'Select groups';

    return (
        <div className="space-y-2">
            <Label htmlFor="application-groups">
                Groups
                {required && <span className="text-destructive"> *</span>}
            </Label>

            <Combobox
                multiple
                value={selectedGroupIds}
                onValueChange={value => onSelectedGroupIdsChange(Array.isArray(value) ? value : [value].filter(Boolean))}
                disabled={isLoading}
                autoComplete="list"
            >
                <ComboboxChips ref={chipsAnchorRef}>
                    {selectedGroups.map(group => (
                        <ComboboxChip key={group.id} removeAriaLabel={`Remove ${group.name}`}>
                            {group.name}
                        </ComboboxChip>
                    ))}
                    <ComboboxChipsInput
                        id="application-groups"
                        placeholder={selectedGroupIds.length === 0 ? placeholder : ''}
                        aria-label="Search groups"
                    />
                </ComboboxChips>
                <ComboboxContent anchor={chipsAnchorRef} align="start">
                    <ComboboxList>
                        <ComboboxEmpty>No groups available</ComboboxEmpty>
                        {sortedGroups.map(group => (
                            <ComboboxItem key={group.id} value={group.id}>
                                {group.name}
                            </ComboboxItem>
                        ))}
                    </ComboboxList>
                </ComboboxContent>
            </Combobox>

            <p className="text-xs text-muted-foreground">Select user groups to add to the application</p>
        </div>
    );
}
