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
import { Card, CardContent, CardHeader, CardTitle, Skeleton } from '@gravitee/graphene-core';
import { useMemo } from 'react';

import { UserRoleMultiSelect } from './UserRoleMultiSelect';
import type { OrganizationRole, OrganizationUser } from '../types/user';
import { getOrganizationRoles, resolveAssignedRoleIds } from '../utils/userDetailDisplay';
import { formatRoleDisplayName } from '../utils/userDisplay';

interface UserOrganizationRolesCardProps {
    readonly user: OrganizationUser;
    readonly roles: OrganizationRole[];
    readonly loading: boolean;
    readonly disabled: boolean;
    readonly saving: boolean;
    readonly onRolesChange: (roleIds: string[]) => void;
}

export function UserOrganizationRolesCard({ user, roles, loading, disabled, saving, onRolesChange }: UserOrganizationRolesCardProps) {
    const assignedRoleIds = useMemo(() => resolveAssignedRoleIds(getOrganizationRoles(user.roles), roles), [roles, user.roles]);

    const options = useMemo(
        () =>
            [...roles]
                .sort((a, b) => (a.name ?? a.id).localeCompare(b.name ?? b.id))
                .map(role => ({
                    value: role.id,
                    label: formatRoleDisplayName(role.name ?? role.id),
                })),
        [roles],
    );

    return (
        <Card>
            <CardHeader className="pb-3">
                <CardTitle className="text-base">Organization Roles</CardTitle>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <Skeleton className="h-10 w-full rounded-md" />
                ) : (
                    <UserRoleMultiSelect
                        options={options}
                        selectedValues={assignedRoleIds}
                        onSelectedValuesChange={onRolesChange}
                        placeholder="Select organization roles"
                        ariaLabel="Organization roles"
                        disabled={disabled || saving}
                    />
                )}
            </CardContent>
        </Card>
    );
}
