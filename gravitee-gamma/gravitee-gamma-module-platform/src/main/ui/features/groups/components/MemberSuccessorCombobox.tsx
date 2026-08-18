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

import { Combobox, ComboboxContent, ComboboxEmpty, ComboboxInput, ComboboxItem, ComboboxList, Label } from '@gravitee/graphene-core';

import type { GroupMember } from '../types/group';

export function MemberSuccessorCombobox({
    id,
    label = 'Search members',
    hint,
    candidates,
    value,
    onChange,
    disabled = false,
}: Readonly<{
    id: string;
    label?: string;
    hint?: string;
    candidates: GroupMember[];
    value: GroupMember | null;
    onChange: (member: GroupMember | null) => void;
    disabled?: boolean;
}>) {
    return (
        <div className="space-y-1.5">
            <Label htmlFor={id} className="text-sm text-muted-foreground">
                {label}
            </Label>
            <Combobox
                items={candidates}
                value={value}
                onValueChange={onChange}
                itemToStringLabel={(m: GroupMember) => m.displayName}
                disabled={disabled}
            >
                <ComboboxInput id={id} aria-label={label} placeholder="Search members…" showClear disabled={disabled} />
                <ComboboxContent>
                    <ComboboxEmpty>No members found</ComboboxEmpty>
                    <ComboboxList>
                        {(m: GroupMember) => (
                            <ComboboxItem key={m.id} value={m}>
                                {m.displayName}
                            </ComboboxItem>
                        )}
                    </ComboboxList>
                </ComboboxContent>
            </Combobox>
            {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
        </div>
    );
}
