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
    Badge,
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Skeleton,
} from '@gravitee/graphene-core';
import { LockIcon, MoreVerticalIcon, PlusIcon, Trash2Icon, UsersIcon } from '@gravitee/graphene-core/icons';

import { SectionError } from '../../../shared/components/SectionError';
import type { RolesByScopeGroup } from '../hooks/useRoles';
import type { Role, RoleScope } from '../types/role';
import { canRoleBeDeleted } from '../utils/rolePermissions';
import { getRoleScopeIcon } from '../utils/roleScopeIcon';
import { roleSectionId } from '../utils/roleSectionId';

interface RoleRowActionsProps {
    role: Role;
    scope: RoleScope;
    canSeeMembers: boolean;
    canDeleteRole: boolean;
    onViewMembers: (scope: RoleScope, roleName: string) => void;
    onDeleteRole: (scope: RoleScope, role: Role) => void;
}

/** Card-list row actions: always a "..." dropdown with the actions this role allows, each paired with its icon. */
function RoleRowActions({ role, scope, canSeeMembers, canDeleteRole, onViewMembers, onDeleteRole }: Readonly<RoleRowActionsProps>) {
    if (!canSeeMembers && !canDeleteRole) {
        return null;
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label={`Actions for ${role.name}`}>
                    <MoreVerticalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                {canSeeMembers ? (
                    <DropdownMenuItem onSelect={() => onViewMembers(scope, role.name)}>
                        <UsersIcon className="size-4 shrink-0" aria-hidden />
                        <span className="whitespace-nowrap">View members</span>
                    </DropdownMenuItem>
                ) : null}
                {canSeeMembers && canDeleteRole ? <DropdownMenuSeparator /> : null}
                {canDeleteRole ? (
                    <DropdownMenuItem onSelect={() => onDeleteRole(scope, role)} className="text-destructive focus:text-destructive">
                        <Trash2Icon className="size-4 shrink-0" aria-hidden />
                        <span className="whitespace-nowrap">Delete role</span>
                    </DropdownMenuItem>
                ) : null}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

export function RolesByScopeSection({
    group,
    canCreate,
    canDelete,
    canManageMembers,
    hasCustomRolesLicense,
    onCreateRole,
    onSelectRole,
    onDeleteRole,
    onViewMembers,
    onShowLicenseDialog,
}: Readonly<{
    group: RolesByScopeGroup;
    canCreate: boolean;
    canDelete: boolean;
    canManageMembers: boolean;
    hasCustomRolesLicense: boolean;
    onCreateRole: (scope: RoleScope) => void;
    onSelectRole: (scope: RoleScope, roleName: string) => void;
    onDeleteRole: (scope: RoleScope, role: Role) => void;
    onViewMembers: (scope: RoleScope, roleName: string) => void;
    onShowLicenseDialog: () => void;
}>) {
    const ScopeIcon = getRoleScopeIcon(group.scope);

    function handleCreateClick() {
        if (hasCustomRolesLicense) {
            onCreateRole(group.scope);
        } else {
            onShowLicenseDialog();
        }
    }

    return (
        <Card id={roleSectionId(group.scope)} className="scroll-mt-20">
            <CardHeader className="space-y-0">
                <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-2">
                        {ScopeIcon ? <ScopeIcon className="size-4 shrink-0 text-primary" aria-hidden /> : null}
                        <CardTitle>{group.label}</CardTitle>
                    </div>
                    {canCreate ? (
                        <Button onClick={handleCreateClick}>
                            {hasCustomRolesLicense ? <PlusIcon className="size-4" aria-hidden /> : <LockIcon className="size-4" aria-hidden />}
                            Add a role
                        </Button>
                    ) : null}
                </div>
            </CardHeader>
            <CardContent>
                {group.isLoading ? (
                    <div className="space-y-2">
                        <Skeleton className="h-10 w-full rounded-md" />
                        <Skeleton className="h-10 w-full rounded-md" />
                    </div>
                ) : group.isError ? (
                    <SectionError message="Failed to load roles for this scope. Please refresh and try again." />
                ) : group.roles.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No role</p>
                ) : (
                    // A plain list, not DataTable, is a deliberate Classic-faithful choice (Classic's roles
                    // page is a mat-list, not a table) — it costs this page a search affordance DataTable
                    // would have given for free on scopes with many roles. Revisit if that becomes a problem.
                    <ul className="divide-y">
                        {group.roles.map(role => (
                            <li key={role.name} className="flex items-center justify-between gap-4 py-3">
                                {/* Name-as-link, not full-row-click: the row also carries row actions
                                    (delete/see members), so the clickable target for "open this role" stays
                                    scoped to its name instead of swallowing the whole row. */}
                                <div className="flex min-w-0 flex-1 items-start gap-3">
                                    <span className="min-w-0">
                                        <span className="flex min-w-0 items-center gap-2 font-medium">
                                            <Button
                                                type="button"
                                                variant="link"
                                                className="h-auto min-w-0 truncate p-0 text-left text-sm font-medium text-foreground hover:underline"
                                                onClick={() => onSelectRole(group.scope, role.name)}
                                            >
                                                {role.name}
                                            </Button>
                                            {role.system ? (
                                                <Badge variant="outline" className="shrink-0">
                                                    System
                                                </Badge>
                                            ) : null}
                                            {role.default ? (
                                                <Badge variant="highlight" className="shrink-0">
                                                    Default
                                                </Badge>
                                            ) : null}
                                        </span>
                                        {role.description ? (
                                            <span className="block truncate text-sm text-muted-foreground">{role.description}</span>
                                        ) : null}
                                    </span>
                                </div>
                                <div className="flex shrink-0 items-center">
                                    <RoleRowActions
                                        role={role}
                                        scope={group.scope}
                                        canSeeMembers={canManageMembers && group.scope === 'ORGANIZATION'}
                                        canDeleteRole={canDelete && canRoleBeDeleted(role)}
                                        onViewMembers={onViewMembers}
                                        onDeleteRole={onDeleteRole}
                                    />
                                </div>
                            </li>
                        ))}
                    </ul>
                )}
            </CardContent>
        </Card>
    );
}
