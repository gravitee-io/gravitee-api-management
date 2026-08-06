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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import type { OrganizationRole } from '../types/user';

export const GROUP_MEMBERSHIP_NONE_ROLE_VALUE = '__none__';

interface GroupMembershipRoleSelectProps {
    readonly id: string;
    readonly ariaLabel: string;
    readonly value?: string;
    readonly roles: OrganizationRole[];
    readonly disabled?: boolean;
    readonly onChange: (value: string | undefined) => void;
}

export function GroupMembershipRoleSelect({ id, ariaLabel, value, roles, disabled = false, onChange }: GroupMembershipRoleSelectProps) {
    return (
        <Select
            value={value ?? GROUP_MEMBERSHIP_NONE_ROLE_VALUE}
            onValueChange={nextValue => onChange(nextValue === GROUP_MEMBERSHIP_NONE_ROLE_VALUE ? undefined : nextValue)}
            disabled={disabled}
        >
            <SelectTrigger id={id} aria-label={ariaLabel} className="w-full min-w-[7.5rem]">
                <SelectValue placeholder="None" />
            </SelectTrigger>
            <SelectContent>
                <SelectItem value={GROUP_MEMBERSHIP_NONE_ROLE_VALUE}>None</SelectItem>
                {roles.map(role => (
                    <SelectItem key={role.id} value={role.name ?? role.id} disabled={role.system}>
                        {role.name ?? role.id}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}
