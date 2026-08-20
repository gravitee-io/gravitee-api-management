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

import { Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import type { GroupRole } from '../types/group';
import { PRIMARY_OWNER_ROLE } from '../types/group';

const NO_ROLE_VALUE = '__none__';

function isRoleOptionDisabled(
    role: GroupRole,
    disabledOptionNames: Set<string> | undefined,
    disableSystemRoles: 'all' | 'except-primary-owner' | undefined,
): boolean {
    if (disabledOptionNames?.has(role.name)) {
        return true;
    }
    if (!role.system || !disableSystemRoles) {
        return false;
    }
    if (disableSystemRoles === 'except-primary-owner' && role.name === PRIMARY_OWNER_ROLE) {
        return false;
    }
    return true;
}

export function GroupRoleSelect({
    id,
    label,
    roles,
    value,
    onChange,
    disabled,
    disabledOptionNames,
    disableSystemRoles,
    hint,
}: Readonly<{
    id: string;
    label: string;
    roles: GroupRole[];
    value: string;
    onChange: (value: string) => void;
    disabled?: boolean;
    disabledOptionNames?: Set<string>;
    disableSystemRoles?: 'all' | 'except-primary-owner';
    hint?: string;
}>) {
    return (
        <div className="space-y-1.5">
            <Label htmlFor={id} className="text-sm text-muted-foreground">
                {label}
            </Label>
            <Select value={value || NO_ROLE_VALUE} onValueChange={v => onChange(v === NO_ROLE_VALUE ? '' : v)} disabled={disabled}>
                <SelectTrigger id={id} className="w-full">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    {roles.map(role => (
                        <SelectItem
                            key={role.name}
                            value={role.name}
                            disabled={isRoleOptionDisabled(role, disabledOptionNames, disableSystemRoles)}
                        >
                            {role.name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
            {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
        </div>
    );
}
