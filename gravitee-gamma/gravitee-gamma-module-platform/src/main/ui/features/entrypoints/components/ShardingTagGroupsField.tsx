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
    Field,
    FieldLabel,
    useComboboxAnchor,
} from '@gravitee/graphene-core';
import { useMemo } from 'react';

import type { OrgGroup } from '../types/entrypoint';

export function ShardingTagGroupsField({
    groups,
    selectedGroupIds,
    onSelectedGroupIdsChange,
    isLoading = false,
    disabled = false,
}: Readonly<{
    groups: OrgGroup[];
    selectedGroupIds: string[];
    onSelectedGroupIdsChange: (groupIds: string[]) => void;
    isLoading?: boolean;
    disabled?: boolean;
}>) {
    const chipsAnchorRef = useComboboxAnchor();

    const sortedGroups = useMemo(() => [...groups].sort((a, b) => a.name.localeCompare(b.name)), [groups]);

    const selectedGroups = useMemo(
        () => sortedGroups.filter(group => selectedGroupIds.includes(group.id)),
        [selectedGroupIds, sortedGroups],
    );

    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel htmlFor="sharding-tag-groups">Restricted groups</FieldLabel>
            <Combobox
                multiple
                value={selectedGroupIds}
                onValueChange={value => onSelectedGroupIdsChange(Array.isArray(value) ? value : [value].filter(Boolean))}
                disabled={disabled || isLoading}
                autoComplete="list"
            >
                <ComboboxChips ref={chipsAnchorRef}>
                    {selectedGroups.map(group => (
                        <ComboboxChip key={group.id} removeAriaLabel={`Remove ${group.name}`}>
                            {group.name}
                        </ComboboxChip>
                    ))}
                    <ComboboxChipsInput
                        id="sharding-tag-groups"
                        placeholder={selectedGroupIds.length === 0 ? 'Select groups' : ''}
                        aria-label="Search restricted groups"
                        readOnly
                    />
                </ComboboxChips>
                <ComboboxContent anchor={chipsAnchorRef} align="start">
                    <ComboboxList>
                        {sortedGroups.length === 0 && !isLoading ? <ComboboxEmpty>No groups available</ComboboxEmpty> : null}
                        {sortedGroups.map(group => (
                            <ComboboxItem key={group.id} value={group.id}>
                                {group.name}
                            </ComboboxItem>
                        ))}
                    </ComboboxList>
                </ComboboxContent>
            </Combobox>
            <p className="text-xs text-muted-foreground">Optional. Restrict which groups may deploy APIs on this tag.</p>
        </Field>
    );
}
