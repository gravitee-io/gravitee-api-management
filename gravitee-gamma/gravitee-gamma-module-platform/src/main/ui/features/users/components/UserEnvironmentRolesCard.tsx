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
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { useMemo } from 'react';

import { UserRoleMultiSelect } from './UserRoleMultiSelect';
import type { OrganizationEnvironment, OrganizationRole, OrganizationUser } from '../types/user';
import { resolveAssignedRoleIds } from '../utils/userDetailDisplay';
import { formatRoleDisplayName } from '../utils/userDisplay';

interface UserEnvironmentRolesCardProps {
    readonly user: OrganizationUser;
    readonly environments: OrganizationEnvironment[];
    readonly roles: OrganizationRole[];
    readonly loading: boolean;
    readonly disabled: boolean;
    readonly savingEnvironmentId?: string;
    readonly onEnvironmentRolesChange: (environmentId: string, roleIds: string[]) => void;
}

function EnvironmentRoleRow({
    environment,
    roleOptions,
    assignedRoleIds,
    disabled,
    saving,
    onRolesChange,
}: Readonly<{
    environment: OrganizationEnvironment;
    roleOptions: { value: string; label: string }[];
    assignedRoleIds: string[];
    disabled: boolean;
    saving: boolean;
    onRolesChange: (roleIds: string[]) => void;
}>) {
    return (
        <TableRow>
            <TableCell className="px-4 py-4 align-middle font-medium">{environment.name ?? environment.id}</TableCell>
            <TableCell className="px-4 py-4 align-middle text-muted-foreground">{environment.description ?? '—'}</TableCell>
            <TableCell className="px-4 py-4 align-middle">
                <UserRoleMultiSelect
                    options={roleOptions}
                    selectedValues={assignedRoleIds}
                    onSelectedValuesChange={onRolesChange}
                    placeholder="Select environment roles"
                    ariaLabel={`Environment roles for ${environment.name ?? environment.id}`}
                    disabled={disabled || saving}
                />
            </TableCell>
        </TableRow>
    );
}

export function UserEnvironmentRolesCard({
    user,
    environments,
    roles,
    loading,
    disabled,
    savingEnvironmentId,
    onEnvironmentRolesChange,
}: UserEnvironmentRolesCardProps) {
    const roleOptions = useMemo(
        () =>
            [...roles]
                .sort((a, b) => (a.name ?? a.id).localeCompare(b.name ?? b.id))
                .map(role => ({
                    value: role.id,
                    label: formatRoleDisplayName(role.name ?? role.id),
                })),
        [roles],
    );

    const assignedRoleIdsByEnvironment = useMemo(
        () =>
            Object.fromEntries(
                environments.map(environment => [environment.id, resolveAssignedRoleIds(user.envRoles?.[environment.id], roles)]),
            ),
        [environments, roles, user.envRoles],
    );

    return (
        <Card>
            <CardHeader className="pb-3">
                <CardTitle className="text-base">Environment Roles</CardTitle>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <div className="space-y-3">
                        {Array.from({ length: 3 }).map((_, index) => (
                            <Skeleton key={index} className="h-12 w-full rounded-lg" />
                        ))}
                    </div>
                ) : environments.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No environments available.</p>
                ) : (
                    <div className="rounded-lg border">
                        <Table aria-label="Environments table">
                            <TableHeader>
                                <TableRow className="bg-muted/30">
                                    <TableHead scope="col" className="w-1/3 px-4 text-muted-foreground">
                                        Name
                                    </TableHead>
                                    <TableHead scope="col" className="w-1/3 px-4 text-muted-foreground">
                                        Description
                                    </TableHead>
                                    <TableHead scope="col" className="w-1/3 px-4 text-muted-foreground">
                                        Roles
                                    </TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {environments.map(environment => (
                                    <EnvironmentRoleRow
                                        key={environment.id}
                                        environment={environment}
                                        roleOptions={roleOptions}
                                        assignedRoleIds={assignedRoleIdsByEnvironment[environment.id] ?? []}
                                        disabled={disabled}
                                        saving={savingEnvironmentId === environment.id}
                                        onRolesChange={roleIds => onEnvironmentRolesChange(environment.id, roleIds)}
                                    />
                                ))}
                            </TableBody>
                        </Table>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
